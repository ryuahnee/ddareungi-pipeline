package com.jakdang.nlsql

import com.fasterxml.jackson.databind.JsonNode

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
                    "따릉이 mart에 대해 질문하세요. 예: 지금 고갈된 대여소 몇 개야?\n조회만 가능하며 최대 20행과 실행 SQL, 데이터 기준 시각을 표시합니다."
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
        for (c in s) {
            val part = escape(c.toString())
            if (out.length + part.length > budget - 12) break
            out.append(part)
        }
        return out.append("… (잘림)").toString()
    }
    fun code(s: String, budget: Int) = if (html) "<pre>${limited(s, budget)}</pre>" else "```\n${limited(s, budget)}\n```"
    val rows = (listOf(result.columns) + result.rows.take(20)).map { row ->
        row.map { it.replace(Regex("[\\r\\n\\t]"), " ").take(60) }
    }
    val widths = result.columns.indices.map { col -> rows.maxOf { it[col].length }.coerceAtMost(60) }
    val table = rows.joinToString("\n") { row -> row.mapIndexed { i, cell -> cell.padEnd(widths[i]) }.joinToString(" | ") }
    val basis = if (result.basis.isEmpty()) "참조 테이블에 collected_at 없음" else
        result.basis.entries.joinToString("\n") { "${it.key}: ${it.value} KST" }
    val count = if (result.truncated) "100행 초과 (100행에서 잘림)" else "${result.rows.size}행"
    return "${limited(generated.explanation.lineSequence().first(), 250)}\n" +
        "결과 (최대 20행):\n${code(table, 1200)}\n" +
        "실행 SQL:\n${code(generated.sql, 1500)}\n" +
        "데이터 기준 시각 (참조 테이블 전체 최댓값):\n${limited(basis, 650)}\n" +
        "행 수: $count${if (result.rows.size > 20) " / 화면 20행에서 잘림" else ""}"
}
