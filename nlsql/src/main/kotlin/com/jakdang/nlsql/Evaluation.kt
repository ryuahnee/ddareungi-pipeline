package com.jakdang.nlsql

import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale

fun resultsMatch(expected: QueryResult, actual: QueryResult, ordered: Boolean, relaxed: Boolean = false): Boolean {
    if (expected.truncated || actual.truncated || actual.columns.size < expected.columns.size) return false
    if (!relaxed && expected.columns.size != actual.columns.size) return false
    fun normalize(result: QueryResult): List<List<Any?>> = result.rows.map { row ->
        row.mapIndexed { i, value ->
            if (i in result.numericColumns && value != null)
                value.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP) ?: value
            else value
        }
    }
    val left = normalize(expected)
    val right = normalize(actual)
    fun sameRows(a: List<List<Any?>>, b: List<List<Any?>>) = if (ordered) a == b else a.toSet() == b.toSet()
    if (!relaxed) return sameRows(left, right)
    val candidates = expected.columns.indices.map { column ->
        actual.columns.indices.filter { candidate ->
            sameRows(left.map { listOf(it[column]) }, right.map { listOf(it[candidate]) })
        }
    }
    val mapping = mutableListOf<Int>()
    fun match(column: Int): Boolean {
        if (column == expected.columns.size) return sameRows(left, right.map { row -> mapping.map { row[it] } })
        for (candidate in candidates[column]) {
            if (candidate in mapping) continue
            mapping.add(candidate)
            // Preserve relationships between columns; independent column sets are insufficient.
            val projected = right.map { row -> mapping.map { row[it] } }
            if (sameRows(left.map { it.take(column + 1) }, projected) && match(column + 1)) return true
            mapping.removeAt(mapping.lastIndex)
        }
        return false
    }
    return match(0)
}

class EvalPreparationFailure(message: String) : RuntimeException(message)

data class GoldenCase(val id: String, val question: String, val expected_sql: String, val category: String)
private data class EvalItem(val case: GoldenCase, val attempt: QueryAttempt, val passed: Boolean, val relaxedPassed: Boolean, val reason: String,
                            val expectedHash: String?, val actualHash: String?)

fun sha256(text: String): String = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }

fun evaluate(model: String, runner: QueryRunner, db: Database, schema: Map<String, List<Pair<String, String>>>,
             redact: (String) -> String) {
    // Model ids become file names; never interpret a supplied id as a path.
    require(Regex("[A-Za-z0-9][A-Za-z0-9._-]*").matches(model))
    val goldenText = checkNotNull(object {}.javaClass.getResource("/eval/golden.jsonl")).readText()
    val cases = goldenText.lineSequence().filter { it.isNotBlank() }.map { json.readValue(it, GoldenCase::class.java) }.toList()
    require(cases.map { it.id }.distinct().size == cases.size)
    val checks = cases.associate { case ->
        val check = validateSql(case.expected_sql)
        if (check.reason != null || check.tables.any { it !in schema }) throw EvalPreparationFailure("정답 SQL 검증 거부: ${case.id}")
        val reference = try { db.execute(case.expected_sql, check.tables, schema) } catch (_: Exception) {
            throw EvalPreparationFailure("정답 SQL 실행 실패: ${case.id}")
        }
        if (reference.rows.isEmpty() || reference.truncated) throw EvalPreparationFailure("정답 SQL 결과는 1~100행이어야 합니다: ${case.id}")
        case.id to check
    }
    println("정답 SQL ${cases.size}개: 검증기·nl_reader 실행·비어 있지 않은 결과 확인")
    val startedAt = Instant.now()
    val items = cases.map { case ->
        val attempt = runner.run(case.question, "eval", case.expected_sql)
        val passed = attempt.reason == null && attempt.expected != null && attempt.result != null &&
            resultsMatch(attempt.expected, attempt.result, checks.getValue(case.id).ordered)
        val relaxedPassed = attempt.reason == null && attempt.expected != null && attempt.result != null &&
            resultsMatch(attempt.expected, attempt.result, checks.getValue(case.id).ordered, relaxed = true)
        val reason = attempt.reason ?: if (passed) "일치" else if (attempt.result?.truncated == true) "결과 불일치: 100행 초과" else "결과 불일치"
        fun fingerprint(result: QueryResult?): String? = result?.let {
            sha256(json.writeValueAsString(it.rows))
        }
        println("${case.id}: strict=${if (passed) "통과" else reason}, relaxed=${if (relaxedPassed) "통과" else reason}")
        EvalItem(case, attempt, passed, relaxedPassed, reason, fingerprint(attempt.expected), fingerprint(attempt.result))
    }
    val correct = items.count { it.passed }
    val relaxedCorrect = items.count { it.relaxedPassed }
    val meanLatency = items.mapNotNull { it.attempt.log.llmLatencyMs }.average()
    val promptTokens = items.sumOf { it.attempt.log.promptTokens?.toLong() ?: 0L }
    val completionTokens = items.sumOf { it.attempt.log.completionTokens?.toLong() ?: 0L }
    val usageCount = items.count { it.attempt.log.promptTokens != null && it.attempt.log.completionTokens != null }
    fun percentage(n: Int, total: Int) = String.format(Locale.ROOT, "%.1f%%", 100.0 * n / total)
    val summary = "strict 정확도: $correct/${items.size} (${percentage(correct, items.size)})\n" +
        "relaxed 정확도: $relaxedCorrect/${items.size} (${percentage(relaxedCorrect, items.size)})\n" +
        "평균 LLM 지연(실패 시도 포함): ${String.format(Locale.ROOT, "%.1f", meanLatency)} ms\n" +
        "총 토큰: ${promptTokens + completionTokens} (prompt=$promptTokens, completion=$completionTokens, usage 수신=$usageCount/${items.size})"
    println(summary)
    val categories = items.groupBy { it.case.category }.map { (category, group) ->
        val count = group.count { it.passed }
        val relaxedCount = group.count { it.relaxedPassed }
        "| $category | $count/${group.size} (${percentage(count, group.size)}) | $relaxedCount/${group.size} (${percentage(relaxedCount, group.size)}) |"
    }
    categories.forEach(::println)
    val glossary = checkNotNull(object {}.javaClass.getResource("/glossary.md")).readText()
    val revision = runCatching {
        ProcessBuilder("git", "rev-parse", "HEAD").redirectError(ProcessBuilder.Redirect.DISCARD).start()
            .inputStream.bufferedReader().use { it.readText().trim() }
    }.getOrDefault("확인 불가")
    val report = buildString {
        appendLine("# $model 평가")
        appendLine()
        appendLine("- 실행 시각(UTC): $startedAt ~ ${Instant.now()}")
        appendLine("- 코드 기준 커밋: `$revision`")
        appendLine("- 평가셋 SHA-256: `${sha256(goldenText)}`")
        appendLine("- 용어 사전 SHA-256: `${sha256(glossary)}`")
        appendLine("- 스키마 SHA-256: `${sha256(json.writeValueAsString(schema))}`")
        appendLine()
        appendLine(summary)
        appendLine()
        appendLine("정답 SQL은 모두 검증기를 통과했고 nl_reader로 실행해 1~100행을 반환했습니다. 문항마다 REPEATABLE READ 트랜잭션 안에서 정답과 생성 SQL을 비교했습니다. 문항·모델 사이에는 실시간 mart 갱신이 있을 수 있습니다.")
        appendLine()
        appendLine("컬럼명은 무시하고 숫자 타입 값은 HALF_UP 소수 둘째 자리로 반올림합니다. 기본 비교는 행 집합이며, 정답 SQL 최상위에 ORDER BY와 LIMIT/FETCH가 함께 있으면 행 순서도 비교합니다. 100행 초과 결과는 통과시키지 않습니다.")
        appendLine()
        appendLine("strict는 컬럼 수와 위치도 일치해야 합니다. relaxed는 정답 각 컬럼을 생성 결과의 서로 다른 컬럼에 대응시킨 뒤 투영한 전체 행을 동일한 행 비교 규칙으로 비교합니다. 보조 컬럼과 컬럼 순서는 허용하지만, 값을 고르거나 행 사이의 컬럼 관계를 바꾸지는 않습니다.")
        appendLine()
        appendLine("API 실패도 두 정확도 분모에 포함합니다. usage 없는 호출의 토큰은 집계하지 않으며, 이런 모델의 품질·비용은 이 결과만으로 비교할 수 없습니다.")
        appendLine()
        appendLine("## 카테고리별 정확도\n\n| 카테고리 | strict | relaxed |\n|---|---:|---:|")
        categories.forEach { appendLine(it) }
        appendLine()
        appendLine("## 문항별 결과\n\n| id | 카테고리 | strict | relaxed | LLM ms | prompt | completion |\n|---|---|---|---|---:|---:|---:|")
        items.forEach { item ->
            val log = item.attempt.log
            appendLine("| ${item.case.id} | ${item.case.category} | ${item.reason} | ${if (item.relaxedPassed) "일치" else item.reason} | ${log.llmLatencyMs} | ${log.promptTokens ?: "미수신"} | ${log.completionTokens ?: "미수신"} |")
        }
        appendLine()
        appendLine("## 검증 Lineage\n\n결과 행은 보관하지 않고 각 비교 시점의 원시 결과 해시와 데이터 기준 시각만 남깁니다. 해시는 결과 순서·숫자 표기에도 영향을 받으며 정확도 판정에는 위 정규화 규칙을 사용합니다.")
        appendLine()
        appendLine("| id | 정답 결과 SHA-256 | 생성 결과 SHA-256 | 데이터 기준 시각(KST) |\n|---|---|---|---|")
        items.forEach { item ->
            appendLine("| ${item.case.id} | ${item.expectedHash ?: "없음"} | ${item.actualHash ?: "없음"} | ${item.attempt.log.dataAsOf ?: "collected_at 없음/실행 실패"} |")
        }
        appendLine()
        appendLine("## 실패 SQL\n")
        items.filter { !it.passed }.forEach { item ->
            appendLine("### ${item.case.id}: strict=${item.reason}, relaxed=${if (item.relaxedPassed) "일치" else item.reason}\n")
            appendLine("```sql\n${item.attempt.generated?.sql ?: "SQL 생성 실패"}\n```\n")
        }
    }
    // Gradle :run uses the module directory; direct Java launches may use the repository root.
    val module = if (Files.isDirectory(Path.of("nlsql/src"))) Path.of("nlsql") else Path.of(".")
    val output = module.resolve("eval-results")
    Files.createDirectories(output)
    Files.writeString(output.resolve("$model.md"), redact(report).trimEnd() + "\n")
    println("평가 결과 저장: eval-results/$model.md")
}
