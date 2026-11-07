package com.jakdang.nlsql

import com.fasterxml.jackson.databind.JsonNode
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun allowedChat(chatId: String, configuredChatId: String): Boolean = chatId == configuredChatId

fun telegramBot(
    token: String,
    chatId: String,
    request: (String, Map<String, Any>, Long) -> JsonNode = { method, body, timeout ->
        postJson("https://api.telegram.org/bot$token/$method", body, timeout, requireHttpSuccess = false)
    },
    pause: (Long) -> Unit = { Thread.sleep(it) },
    ask: (String) -> String
) {
    fun send(answer: String) {
        val sent = request("sendMessage", mapOf("chat_id" to chatId, "text" to answer, "parse_mode" to "HTML"), 30)
        if (!sent.path("ok").asBoolean()) {
            val plain = answer.replace(Regex("<[^>]*>"), "")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
            check(request("sendMessage", mapOf("chat_id" to chatId, "text" to plain), 30).path("ok").asBoolean())
        }
    }
    var offset = 0L
    var backoff = 1000L
    println("텔레그램 polling 시작")
    while (!Thread.currentThread().isInterrupted) {
        val updates = try {
            request("getUpdates", mapOf("timeout" to 30, "offset" to offset,
                "allowed_updates" to listOf("message")), 40).also { check(it.path("ok").asBoolean()) }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            return
        } catch (_: Exception) {
            println("텔레그램 polling 실패: 대기 후 재시도")
            try { pause(backoff) } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }
            backoff = (backoff * 2).coerceAtMost(60000L)
            continue
        }
        backoff = 1000L
        for (update in updates.path("result")) {
            offset = maxOf(offset, update.path("update_id").asLong() + 1)
            try {
                val message = update.path("message")
                if (!message.isObject) continue
                if (!allowedChat(message.path("chat").path("id").asText(), chatId)) {
                    println("허용되지 않은 채팅: 메시지 무시")
                    continue
                }
                val text = message.path("text").asText()
                if (text.isBlank()) continue
                val answer = if (text.substringBefore(' ').substringBefore('@') == "/start") {
                    "따릉이 mart에 대해 질문하세요. 예: 지금 고갈된 대여소 몇 개야?\n조회만 가능하며 최대 10행과 실행 SQL, 데이터 기준 시각을 표시합니다."
                } else ask(text)
                send(answer)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            } catch (_: Exception) {
                println("텔레그램 update 처리 실패")
                try {
                    send("질문 처리 중 오류가 발생했습니다. 다시 질문해 주세요.")
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return
                } catch (_: Exception) {
                    println("텔레그램 오류 답장 실패: 다음 update 진행")
                }
            }
        }
    }
}

fun formatAnswer(generated: GeneratedSql, result: QueryResult, html: Boolean): String {
    fun escape(s: String) = if (html) s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") else s
    fun limited(s: String, budget: Int): String {
        if (escape(s).length <= budget) return escape(s)
        val out = StringBuilder()
        val points = s.codePoints().iterator()
        while (points.hasNext()) {
            val part = escape(String(Character.toChars(points.nextInt())))
            if (out.length + part.length > budget - 8) break
            out.append(part)
        }
        return out.append("… (잘림)").toString()
    }
    fun oneLine(s: String) = s.replace(Regex("[\\r\\n\\t]"), " ")
    fun shortText(text: String) = if (text.codePointCount(0, text.length) <= 60) text else
        text.substring(0, text.offsetByCodePoints(0, 60)) + "…"
    fun width(text: String) = text.codePoints().map {
        if (it in 0x1100..0x115f || it in 0x2e80..0xa4cf || it in 0xac00..0xd7a3 ||
            it in 0xf900..0xfaff || it in 0x1f300..0x1faff) 2 else 1
    }.sum()
    val number = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US)).apply { roundingMode = RoundingMode.HALF_UP }
    fun cell(column: Int, value: String?): String {
        if (value == null) return "NULL"
        val numeric = if (column in result.numericColumns) value.toBigDecimalOrNull() else null
        val text = oneLine(if (numeric == null) value else number.format(numeric))
        return shortText(text)
    }
    fun bold(s: String) = if (html) "<b>$s</b>" else s
    val title = limited(oneLine(generated.title.ifBlank { "조회 결과" }), 160)
    val unit = limited(oneLine(generated.unit), 40)
    val times = result.basis.values.mapNotNull { runCatching { LocalDateTime.parse(it.replace(' ', 'T')) }.getOrNull() }
    val basis = if (result.basis.isEmpty()) null else if (times.size != result.basis.size) "관측 시각 없음" else
        times.minOrNull()?.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))?.plus(" KST 기준")
    val body = when {
        result.rows.isEmpty() -> bold(title) + "\n조회 결과가 없습니다 (관측 없음 ≠ 0건)" + (basis?.let { "\n$it" } ?: "")
        result.rows.size == 1 && result.columns.size == 1 -> {
            val value = limited(cell(0, result.rows.single().single()), 180)
            bold("🚲 $title: $value$unit") + "\n" + limited(oneLine(generated.explanation), 300) + (basis?.let { " · $it" } ?: "")
        }
        else -> {
            val last = result.columns.lastIndex
            val textColumn = (0 until last).firstOrNull { it !in result.numericColumns }
            val rows = result.rows.take(10)
            val content = if (result.columns.size in 2..3 && last in result.numericColumns && textColumn != null) {
                limited(rows.mapIndexed { i, row -> "${i + 1}. ${cell(textColumn, row[textColumn])}  ${cell(last, row[last])}${oneLine(generated.unit)}" }.joinToString("\n"), 1700)
            } else {
                val headers = if (generated.labels.size == result.columns.size) generated.labels else result.columns
                val tableRows = listOf(headers.map { shortText(oneLine(it)) }) + rows.map { row -> row.mapIndexed { i, value -> cell(i, value) } }
                val widths = result.columns.indices.map { col -> tableRows.maxOf { width(it[col]) } }
                val table = tableRows.joinToString("\n") { row -> row.mapIndexed { i, value -> value + " ".repeat((widths[i] - width(value)).coerceAtLeast(0)) }.joinToString(" | ") }
                val text = limited(table, 1700)
                if (html) "<pre>$text</pre>" else text
            }
            bold("📊 $title") + (basis?.let { "\n$it" } ?: "") + "\n\n$content"
        }
    }
    val count = when {
        result.truncated -> "\n100행 초과로 잘림"
        result.rows.size > 10 -> "\n상위 10행만 표시 · 전체 ${result.rows.size}행"
        else -> ""
    }
    val sql = limited(generated.sql, 1400)
    val sqlBlock = if (html) "<blockquote expandable>SQL\n<code>$sql</code></blockquote>" else "SQL\n$sql"
    return "$body$count\n\n$sqlBlock"
}
