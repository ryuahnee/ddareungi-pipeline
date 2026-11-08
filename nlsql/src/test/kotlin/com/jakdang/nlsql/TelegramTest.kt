package com.jakdang.nlsql

import org.junit.jupiter.api.Test
import kotlin.test.*

class TelegramTest {
    @Test fun `설정된 chat id만 허용한다`() {
        assertTrue(allowedChat("-100123", "-100123"))
        assertFalse(allowedChat("456", "-100123"))
    }

    @Test fun `polling 예외와 API 실패는 최대 60초까지 대기하고 성공 후 초기화한다`() {
        var polls = 0
        val waits = mutableListOf<Long>()
        try {
            telegramBot("test-token", "123", request = { method, _, _ ->
                assertEquals("getUpdates", method)
                polls++
                when {
                    polls <= 8 -> {
                        if (polls % 2 == 1) error("외부 오류 원문")
                        json.readTree("""{"ok":false}""")
                    }
                    polls == 10 -> json.readTree("""{"ok":false}""")
                    else -> {
                        if (polls == 11) Thread.currentThread().interrupt()
                        json.readTree("""{"ok":true,"result":[]}""")
                    }
                }
            }, pause = { waits.add(it) }, ask = { error("메시지 없음") })
        } finally { Thread.interrupted() }
        assertEquals(listOf(1000L, 2000L, 4000L, 8000L, 16000L, 32000L, 60000L, 60000L, 1000L), waits)
        assertEquals(11, polls)
    }

    @Test fun `오류 답장 실패에도 offset이 진행되고 HTML 실패는 평문으로 한번 전송한다`() {
        val offsets = mutableListOf<Long>()
        val sends = mutableListOf<Map<String, Any>>()
        val questions = mutableListOf<String>()
        try {
            telegramBot("test-token", "123", request = { method, body, _ ->
                if (method == "getUpdates") {
                    offsets.add(body.getValue("offset") as Long)
                    if (offsets.size == 1) {
                        json.readTree("""{"ok":true,"result":[
                            {"update_id":10,"message":{"chat":{"id":123},"text":"bad"}},
                            {"update_id":11,"message":{"chat":{"id":123},"text":"good"}}
                        ]}""")
                    } else {
                        Thread.currentThread().interrupt()
                        json.readTree("""{"ok":true,"result":[]}""")
                    }
                } else {
                    assertEquals("sendMessage", method)
                    sends.add(body)
                    json.readTree(if (sends.size == 4) """{"ok":true}""" else """{"ok":false}""")
                }
            }, pause = { error("polling 실패 없음") }, ask = {
                questions.add(it)
                if (it == "bad") error("처리 오류 원문")
                "<pre>1 &lt; 2 &amp; &lt;b&gt;</pre>"
            })
        } finally { Thread.interrupted() }
        assertEquals(listOf(0L, 12L), offsets)
        assertEquals(listOf("bad", "good"), questions)
        assertEquals(4, sends.size)
        assertEquals("HTML", sends[0]["parse_mode"])
        assertFalse(sends[1].containsKey("parse_mode"))
        assertTrue((sends[0]["text"] as String).contains("오류가 발생했습니다"))
        assertEquals("HTML", sends[2]["parse_mode"])
        assertFalse(sends[3].containsKey("parse_mode"))
        assertEquals("1 < 2 & <b>", sends[3]["text"])
    }

}
