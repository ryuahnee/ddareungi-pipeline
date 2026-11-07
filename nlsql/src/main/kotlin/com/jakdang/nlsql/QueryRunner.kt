package com.jakdang.nlsql

import java.sql.SQLException
import java.time.LocalDateTime

// Shared by telegram, cli and eval so every question gets exactly one logging attempt.
class QueryRunner(private val db: Database, private val schema: Map<String, List<Pair<String, String>>>,
                  private val key: String, private val model: String, private val logger: QueryLogger) {
    fun run(question: String, source: String, expectedSql: String? = null): QueryAttempt {
        val log = QueryLog(source, question, model)
        val generated: GeneratedSql
        var result: QueryResult? = null
        var expected: QueryResult? = null
        var reason: String? = null
        try {
            val llmStart = System.nanoTime()
            try {
                generated = generateSql(question, schema, key, model)
                log.generatedSql = generated.sql
                log.promptTokens = generated.promptTokens
                log.completionTokens = generated.completionTokens
            } catch (e: ApiFailure) {
                reason = "OpenAI 실패 (HTTP ${e.status})"
                log.error = reason
                return QueryAttempt(log, reason = reason)
            } catch (_: Exception) {
                reason = "OpenAI 실패: SQL을 생성하지 못했습니다."
                log.error = reason
                return QueryAttempt(log, reason = reason)
            } finally { log.llmLatencyMs = elapsedMs(llmStart) }
            val check = validateSql(generated.sql)
            log.rejectReason = check.reason ?: if (check.tables.any { it !in schema }) "조회 가능한 public mart 테이블만 허용합니다." else null
            log.guardPassed = log.rejectReason == null
            if (log.rejectReason != null) {
                reason = "검증 거부: ${log.rejectReason}"
                return QueryAttempt(log, generated, reason = reason)
            }
            val dbStart = System.nanoTime()
            try {
                if (expectedSql == null) result = db.execute(generated.sql, check.tables, schema)
                else {
                    val expectedCheck = validateSql(expectedSql)
                    check(expectedCheck.reason == null && expectedCheck.tables.all { it in schema })
                    val pair = db.evaluate(generated.sql, expectedSql, check.tables, expectedCheck.tables, schema)
                    result = pair.first
                    expected = pair.second
                }
                log.dbLatencyMs = result.latencyMs
                log.rowCount = result.rows.size
                log.truncated = result.truncated
                log.dataAsOf = result.basis.values.mapNotNull {
                    runCatching { LocalDateTime.parse(it.replace(' ', 'T')) }.getOrNull()
                }.maxOrNull()
            } catch (e: SQLException) {
                reason = if (e.sqlState == "57014") "SQL 오류: 실행 제한 시간(5초)을 초과했습니다." else "SQL 오류: 조회를 실행하지 못했습니다."
                log.dbLatencyMs = elapsedMs(dbStart)
                log.error = reason
            } catch (_: Exception) {
                reason = "SQL 오류: 조회를 실행하지 못했습니다."
                log.dbLatencyMs = elapsedMs(dbStart)
                log.error = reason
            }
            return QueryAttempt(log, generated, result, expected, reason)
        } finally { logger.write(log) }
    }
}

data class QueryAttempt(val log: QueryLog, val generated: GeneratedSql? = null,
                        val result: QueryResult? = null, val expected: QueryResult? = null, val reason: String? = null)
