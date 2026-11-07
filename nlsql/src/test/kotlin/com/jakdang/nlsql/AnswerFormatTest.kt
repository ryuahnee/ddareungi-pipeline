package com.jakdang.nlsql

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.*

class AnswerFormatTest {
    private val today = LocalDate.of(2026, 11, 8)
    private val query = GeneratedSql("SELECT 1;", title = "고갈 대여소", unit = "곳")

    @Test fun `개수와 전체 수는 대상 값과 소수 첫째 자리 비율을 표시한다`() {
        val result = QueryResult(listOf("target", "total"), listOf(listOf("218", "2735")), false, emptyMap(), setOf(0, 1))
        val count = query.copy(labels = listOf("대상 수", "전체 수"), condition = "거치율 10% 미만")
        val answer = formatAnswer(count, result, true, today)
        assertTrue(answer.startsWith("<b>🚲 고갈 대여소 : 218곳</b>\n비율 : 전체 2,735곳 중 8.0%\n조건 : 거치율 10% 미만"))
        assertFalse(answer.contains("<pre>"))
        val zero = formatAnswer(count, result.copy(rows = listOf(listOf("0", "0"))), true, today)
        assertTrue(zero.contains("전체 0곳 중 계산 불가"))
        assertFalse(zero.contains("0.0%"))
        val mean = formatAnswer(count.copy(labels = listOf("평균 수", "전체 수")), result, true, today)
        assertFalse(mean.contains("비율 :"))
    }

    @Test fun `단일 값과 빈 항목 생략 및 가장 오래된 기준시각을 표시한다`() {
        val result = QueryResult(listOf("count"), listOf(listOf("2530.00")), false,
            mapOf("mart_a" to "2026-11-08 01:10:00", "mart_b" to "2026-11-08 00:40:00"), setOf(0))
        val html = formatAnswer(query, result, true, today)
        assertTrue(html.startsWith("<b>🚲 고갈 대여소 : 2,530곳</b>\n기준 : 00:40"))
        assertEquals(1, Regex("기준 :").findAll(html).count())
        assertFalse(html.contains("01:10"))
        assertFalse(html.contains("KST"))
        for (field in listOf("비율 :", "조건 :", "기간 :", "참고 :")) assertFalse(html.contains(field))
        assertTrue(html.endsWith("<blockquote expandable>SQL 보기\n<code>SELECT 1;</code></blockquote>"))
        val plain = formatAnswer(query, result, false, today)
        assertTrue(plain.startsWith("🚲 고갈 대여소 : 2,530곳\n"))
        assertTrue(plain.endsWith("SQL 보기\nSELECT 1;"))
        assertFalse(plain.contains("<b>"))
    }

    @Test fun `오늘이 아닌 KST 관측에는 월일도 표시한다`() {
        val result = QueryResult(listOf("n"), listOf(listOf("1")), false, mapOf("mart_a" to "2026-11-07 23:50:00"), setOf(0))
        assertTrue(formatAnswer(query, result, true, today).contains("기준 : 11-07 23:50"))
    }

    @Test fun `ID 이름 숫자 순위는 숫자 바로 앞의 이름 컬럼을 사용한다`() {
        val result = QueryResult(listOf("id", "name", "count"),
            listOf(listOf("ST-1601", "강남<역>", "1234.5"), listOf("ST-2970", "잠실", "1000.00")),
            false, emptyMap(), setOf(2))
        val answer = formatAnswer(query.copy(title = "고갈 경보 TOP 5", unit = "회", condition = "거치율 10% 미만 기록 횟수 (10분 단위)",
            period = "전체 수집 기간", note = "실제 이용 횟수와 다릅니다"), result, true, today)
        assertTrue(answer.startsWith("<b>📊 고갈 경보 TOP 5</b>\n조건 : 거치율 10% 미만 기록 횟수 (10분 단위)\n기간 : 전체 수집 기간\n참고 : 💡 실제 이용 횟수와 다릅니다\n\n"))
        assertTrue(answer.contains("1. 강남&lt;역&gt; : 1,234.5회\n2. 잠실 : 1,000회"))
        assertFalse(answer.contains("ST-1601"))
        assertFalse(answer.contains("<pre>"))
    }

    @Test fun `나머지 결과는 한국어 labels 헤더의 고정폭 표로 표시한다`() {
        val result = QueryResult(listOf("id", "name", "line", "rate"),
            listOf(listOf("0012", "강남", "2호선", "42.567")), false, emptyMap(), setOf(3))
        val answer = formatAnswer(query.copy(labels = listOf("역 코드", "역 이름", "노선", "고갈율")), result, true, today)
        assertTrue(answer.contains("<pre>역 코드"))
        assertTrue(answer.contains("역 이름"))
        assertTrue(answer.contains("42.57"))
        assertTrue(answer.contains("0012"))
        val aligned = QueryResult(listOf("x", "y"), listOf(listOf("A", "B")), false, emptyMap())
        assertTrue(formatAnswer(query.copy(labels = listOf("이름", "값")), aligned, true, today).contains("<pre>이름 | 값\nA    | B </pre>"))
    }

    @Test fun `0행은 관측 없음 참고와 기준시각을 표시한다`() {
        val result = QueryResult(listOf("count"), emptyList(), false,
            mapOf("mart_a" to "2026-11-08 00:40:00"))
        val answer = formatAnswer(query, result, true, today)
        assertTrue(answer.startsWith("<b>📊 고갈 대여소 : 결과 없음</b>\n참고 : 💡 관측 없음 (0건과 다름)\n기준 : 00:40"))
        assertFalse(answer.contains("🚲"))
        assertFalse(answer.contains("비율 :"))
        assertTrue(formatAnswer(query, result.copy(basis = mapOf("mart_a" to "관측 없음")), true, today).contains("기준 : 관측 시각 없음"))
    }

    @Test fun `잘림은 모델 note와 함께 하나의 참고 줄에 표시한다`() {
        val rows = (1..11).map { listOf("대여소$it", it.toString()) }
        val result = QueryResult(listOf("name", "count"), rows, false, emptyMap(), setOf(1))
        val answer = formatAnswer(query.copy(note = "경보 기록 기준"), result, true, today)
        assertTrue(answer.contains("10. 대여소10 : 10곳"))
        assertFalse(answer.contains("11. 대여소11"))
        assertTrue(answer.contains("참고 : 💡 경보 기록 기준 · 상위 10행만 표시 · 전체 11행"))
        assertEquals(1, Regex("참고 :").findAll(answer).count())
        val truncated = formatAnswer(query.copy(note = "긴 참고".repeat(1000)), result.copy(truncated = true), true, today)
        assertTrue(truncated.contains("100행 초과로 잘림"))
        assertFalse(truncated.contains("전체 11행"))
    }

    @Test fun `정수 쉼표와 소수 반올림을 적용하고 문자열과 NULL은 유지한다`() {
        for ((value, expected) in listOf("1000" to "1,000", "1000.00" to "1,000", "1.235" to "1.24", "-1234.565" to "-1,234.57")) {
            val result = QueryResult(listOf("value"), listOf(listOf(value)), false, emptyMap(), setOf(0))
            assertTrue(formatAnswer(query, result, true, today).contains(" : ${expected}곳</b>"))
        }
        val text = QueryResult(listOf("id"), listOf(listOf("0007")), false, emptyMap())
        assertTrue(formatAnswer(query.copy(unit = ""), text, true, today).contains(" : 0007</b>"))
        assertTrue(formatAnswer(query, text.copy(rows = listOf(listOf(null))), true, today).contains(" : NULL곳</b>"))
    }

    @Test fun `labels 길이가 다르면 실제 컬럼명을 사용한다`() {
        val result = QueryResult(listOf("code", "name"), listOf(listOf("001", "강남")), false, emptyMap())
        val answer = formatAnswer(query.copy(labels = listOf("잘못된 라벨")), result, true, today)
        assertTrue(answer.contains("<pre>code"))
        assertTrue(answer.contains("name"))
        assertFalse(answer.contains("잘못된 라벨"))
    }

    @Test fun `제목 항목 값 단위 SQL의 HTML을 모두 이스케이프한다`() {
        val result = QueryResult(listOf("value"), listOf(listOf("<값&>")), false, emptyMap())
        val answer = formatAnswer(query.copy(title = "<제목&>", condition = "<조건&>", period = "<기간&>", note = "<참고&>", unit = "<&>", sql = "SELECT '<&>';"), result, true, today)
        for (field in listOf("제목", "조건", "기간", "참고")) assertTrue(answer.contains("&lt;$field&amp;&gt;"))
        assertTrue(answer.contains("&lt;값&amp;&gt;&lt;&amp;&gt;"))
        assertTrue(answer.contains("<code>SELECT '&lt;&amp;&gt;';</code>"))
        assertFalse(answer.contains("<제목"))
    }

    @Test fun `4096자를 지키고 태그와 이모지 쌍을 자르지 않는다`() {
        val long = "😀<&".repeat(3000)
        val result = QueryResult(List(4) { "col$it" }, List(11) { List(4) { long } }, true, emptyMap())
        val answer = formatAnswer(query.copy(title = long, unit = long, condition = long, period = long, note = long, labels = List(4) { long }, sql = long), result, true, today)
        assertTrue(answer.length <= 4096)
        assertTrue(answer.contains("… (잘림)"))
        assertTrue(answer.contains("100행 초과로 잘림"))
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
