package com.jakdang.nlsql

import org.junit.jupiter.api.Test
import kotlin.test.*

class AnswerFormatTest {
    private val query = GeneratedSql("SELECT 1;", "관측된 스냅샷 집계", title = "고갈 대여소", unit = "곳")

    @Test fun `단일 값은 강조하고 가장 오래된 기준시각을 한 번만 표시한다`() {
        val result = QueryResult(listOf("count"), listOf(listOf("2530.00")), false,
            mapOf("mart_a" to "2026-11-08 01:10:00", "mart_b" to "2026-11-08 00:40:00"), setOf(0))
        val html = formatAnswer(query, result, true)
        assertTrue(html.startsWith("<b>🚲 고갈 대여소: 2,530곳</b>\n관측된 스냅샷 집계 · 11-08 00:40 KST 기준"))
        assertEquals(1, Regex("KST").findAll(html).count())
        assertFalse(html.contains("01:10"))
        assertFalse(html.contains("행 수"))
        assertTrue(html.endsWith("<blockquote expandable>SQL\n<code>SELECT 1;</code></blockquote>"))
        val plain = formatAnswer(query, result, false)
        assertTrue(plain.startsWith("🚲 고갈 대여소: 2,530곳\n"))
        assertTrue(plain.endsWith("SQL\nSELECT 1;"))
        assertFalse(plain.contains("<b>"))
    }

    @Test fun `2에서 3컬럼의 마지막 숫자는 첫 텍스트 컬럼으로 순위를 만든다`() {
        val result = QueryResult(listOf("id", "name", "count"),
            listOf(listOf("12", "강남<역>", "1234.5"), listOf("13", "잠실", "1000.00")),
            false, emptyMap(), setOf(0, 2))
        val answer = formatAnswer(query.copy(title = "고갈 경보 TOP 5", unit = "회"), result, true)
        assertTrue(answer.startsWith("<b>📊 고갈 경보 TOP 5</b>\n\n"))
        assertTrue(answer.contains("1. 강남&lt;역&gt;  1,234.5회\n2. 잠실  1,000회"))
        assertFalse(answer.contains("<pre>"))
        assertFalse(answer.contains("기준"))
    }

    @Test fun `나머지 결과는 한국어 labels 헤더의 고정폭 표로 표시한다`() {
        val result = QueryResult(listOf("id", "name", "line", "rate"),
            listOf(listOf("0012", "강남", "2호선", "42.567")), false, emptyMap(), setOf(3))
        val answer = formatAnswer(query.copy(labels = listOf("역 코드", "역 이름", "노선", "고갈율")), result, true)
        assertTrue(answer.contains("<pre>역 코드"))
        assertTrue(answer.contains("역 이름"))
        assertTrue(answer.contains("42.57"))
        assertTrue(answer.contains("0012"))
        assertFalse(answer.contains("1. "))
        val aligned = QueryResult(listOf("x", "y"), listOf(listOf("A", "B")), false, emptyMap())
        assertTrue(formatAnswer(query.copy(labels = listOf("이름", "값")), aligned, true).contains("<pre>이름 | 값\nA    | B </pre>"))
    }

    @Test fun `0행은 관측 없음이 0건과 다름을 알리고 기준시각을 표시한다`() {
        val result = QueryResult(listOf("count"), emptyList(), false,
            mapOf("mart_a" to "2026-11-08 00:40:00"))
        val answer = formatAnswer(query, result, true)
        assertTrue(answer.startsWith("<b>고갈 대여소</b>\n조회 결과가 없습니다 (관측 없음 ≠ 0건)\n11-08 00:40 KST 기준"))
        assertFalse(answer.contains("🚲"))
        assertFalse(answer.contains("전체 0행"))
        val unknown = formatAnswer(query, result.copy(basis = mapOf("mart_a" to "관측 없음")), true)
        assertTrue(unknown.contains("관측 시각 없음"))
    }

    @Test fun `10행 표시 제한과 100행 실행 잘림을 구분한다`() {
        val rows = (1..11).map { listOf("대여소$it", it.toString()) }
        val result = QueryResult(listOf("name", "count"), rows, false, emptyMap(), setOf(1))
        val answer = formatAnswer(query, result, true)
        assertTrue(answer.contains("10. 대여소10  10곳"))
        assertFalse(answer.contains("11. 대여소11"))
        assertTrue(answer.contains("상위 10행만 표시 · 전체 11행"))
        val truncated = formatAnswer(query, result.copy(truncated = true), true)
        assertTrue(truncated.contains("100행 초과로 잘림"))
        assertFalse(truncated.contains("전체 11행"))
    }

    @Test fun `정수 쉼표와 소수 반올림을 적용하고 문자열과 NULL은 유지한다`() {
        for ((value, expected) in listOf("1000" to "1,000", "1000.00" to "1,000", "1.235" to "1.24", "-1234.565" to "-1,234.57")) {
            val result = QueryResult(listOf("value"), listOf(listOf(value)), false, emptyMap(), setOf(0))
            assertTrue(formatAnswer(query, result, true).contains(": ${expected}곳</b>"))
        }
        val text = QueryResult(listOf("id"), listOf(listOf("0007")), false, emptyMap())
        assertTrue(formatAnswer(query.copy(unit = ""), text, true).contains(": 0007</b>"))
        assertTrue(formatAnswer(query, text.copy(rows = listOf(listOf(null))), true).contains(": NULL곳</b>"))
    }

    @Test fun `labels 길이가 다르면 실제 컬럼명을 사용한다`() {
        val result = QueryResult(listOf("code", "name"), listOf(listOf("001", "강남")), false, emptyMap())
        val answer = formatAnswer(query.copy(labels = listOf("잘못된 라벨")), result, true)
        assertTrue(answer.contains("<pre>code"))
        assertTrue(answer.contains("name"))
        assertFalse(answer.contains("잘못된 라벨"))
    }

    @Test fun `제목 설명 값 단위 SQL의 HTML을 모두 이스케이프한다`() {
        val result = QueryResult(listOf("value"), listOf(listOf("<값&>")), false, emptyMap())
        val answer = formatAnswer(query.copy(title = "<제목&>", explanation = "<설명&>", unit = "<&>", sql = "SELECT '<&>';"), result, true)
        assertTrue(answer.contains("&lt;제목&amp;&gt;"))
        assertTrue(answer.contains("&lt;설명&amp;&gt;"))
        assertTrue(answer.contains("&lt;값&amp;&gt;&lt;&amp;&gt;"))
        assertTrue(answer.contains("<code>SELECT '&lt;&amp;&gt;';</code>"))
        assertFalse(answer.contains("<제목"))
    }

    @Test fun `4096자를 지키고 태그와 이모지 쌍을 자르지 않는다`() {
        val long = "😀<&".repeat(3000)
        val result = QueryResult(List(4) { "col$it" }, List(11) { List(4) { long } }, true, emptyMap())
        val answer = formatAnswer(query.copy(title = long, unit = long, labels = List(4) { long }, sql = long), result, true)
        assertTrue(answer.length <= 4096)
        assertTrue(answer.contains("… (잘림)"))
        for (tag in listOf("b", "pre", "code")) {
            assertEquals(1, Regex("<$tag>").findAll(answer).count())
            assertEquals(1, Regex("</$tag>").findAll(answer).count())
        }
        assertEquals(1, Regex("<blockquote expandable>").findAll(answer).count())
        assertTrue(answer.endsWith("</blockquote>"))
        for (i in answer.indices) {
            if (answer[i].isHighSurrogate()) assertTrue(i + 1 < answer.length && answer[i + 1].isLowSurrogate())
            if (answer[i].isLowSurrogate()) assertTrue(i > 0 && answer[i - 1].isHighSurrogate())
        }
    }
}
