package com.jakdang.nlsql

import org.junit.jupiter.api.Test
import kotlin.test.*

class TelegramTest {
    @Test fun `설정된 chat id만 허용한다`() {
        assertTrue(allowedChat("-100123", "-100123"))
        assertFalse(allowedChat("456", "-100123"))
    }

    @Test fun `답장은 HTML 이스케이프하고 길이와 화면 행수를 제한한다`() {
        val rows = (1..100).map { listOf("<&".repeat(100)) }
        val answer = formatAnswer(GeneratedSql("SELECT '<&' " + "x".repeat(5000), "설명"),
            QueryResult(listOf("열"), rows, true, mapOf("mart_station_snapshot" to "2026-11-08 00:10:45")), true)
        assertTrue(answer.length <= 4096)
        assertFalse(answer.contains("SELECT '<&'"))
        assertTrue(answer.contains("&lt;"))
        assertTrue(answer.contains("화면 20행에서 잘림"))
        assertTrue(answer.contains("100행 초과"))
        assertEquals(2, "<pre>".toRegex().findAll(answer).count())
        assertEquals(2, "</pre>".toRegex().findAll(answer).count())
        assertTrue(answer.contains("KST"))
    }
}
