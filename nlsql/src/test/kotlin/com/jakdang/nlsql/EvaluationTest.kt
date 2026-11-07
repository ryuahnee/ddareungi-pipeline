package com.jakdang.nlsql

import org.junit.jupiter.api.Test
import kotlin.test.*

class EvaluationTest {
    private fun result(vararg values: String?) = QueryResult(listOf("value"), values.map { listOf(it) }, false, emptyMap())

    @Test fun `순서가 없으면 행 집합을 비교한다`() {
        assertTrue(resultsMatch(result("a", "b", "a"), result("b", "a"), false))
        assertFalse(resultsMatch(result("a", "b"), result("a", "c"), false))
    }

    @Test fun `순서 비교는 순서가 바뀌면 실패한다`() {
        assertFalse(resultsMatch(result("a", "b"), result("b", "a"), true))
        assertTrue(resultsMatch(result("a", "b"), result("a", "b"), true))
        assertTrue(validateSql("SELECT station_id FROM mart_station_snapshot ORDER BY station_id LIMIT 5").ordered)
        assertFalse(validateSql("SELECT station_id FROM mart_station_snapshot ORDER BY station_id").ordered)
    }

    @Test fun `숫자 타입만 소수 둘째 자리 HALF_UP으로 비교한다`() {
        val expected = result("1.234", "-2.345", "3").copy(numericColumns = setOf(0))
        val actual = result("1.23", "-2.35", "3.00").copy(numericColumns = setOf(0))
        assertTrue(resultsMatch(expected, actual, true))
        assertFalse(resultsMatch(result("001"), result("1"), false))
        assertFalse(resultsMatch(expected, result("1.24", "-2.35", "3").copy(numericColumns = setOf(0)), true))
    }

    @Test fun `컬럼명은 무시하고 컬럼 수와 NULL은 구별한다`() {
        assertTrue(resultsMatch(result("a"), result("a").copy(columns = listOf("renamed")), false))
        assertFalse(resultsMatch(result(null), result("NULL"), false))
        assertFalse(resultsMatch(result("a"), QueryResult(listOf("x", "y"), listOf(listOf("a", "b")), false, emptyMap()), false))
        assertFalse(resultsMatch(result("a"), result("a").copy(truncated = true), false))
    }

    @Test fun `비밀값과 연결 호스트를 저장 전에 가린다`() {
        val redact = secretRedactor(mapOf("OPENAI_API_KEY" to "test-secret", "GRAFANA_DB_URL" to "jdbc:postgresql://example.invalid/db"))
        assertFalse(redact("test-secret example.invalid").contains("test-secret"))
        assertFalse(redact("test-secret example.invalid").contains("example.invalid"))
    }
    @Test fun `25개 정답 SQL은 모두 검증기를 통과한다`() {
        val cases = checkNotNull(javaClass.getResource("/eval/golden.jsonl")).readText().lineSequence()
            .filter { it.isNotBlank() }.map { json.readValue(it, GoldenCase::class.java) }.toList()
        assertEquals(25, cases.size)
        cases.forEach { assertNull(validateSql(it.expected_sql).reason, it.id) }
    }

    @Test fun `보조 컬럼은 strict 실패 relaxed 통과하지만 다른 값은 모두 실패한다`() {
        val expected = result("1.234", "2.345").copy(numericColumns = setOf(0))
        val actual = QueryResult(listOf("extra", "renamed"), listOf(listOf("a", "2.35"), listOf("b", "1.23")),
            false, emptyMap(), numericColumns = setOf(1))
        assertFalse(resultsMatch(expected, actual, false))
        assertTrue(resultsMatch(expected, actual, false, relaxed = true))
        assertFalse(resultsMatch(expected, actual, true, relaxed = true))
        val wrong = actual.copy(rows = listOf(listOf("a", "2.36"), listOf("b", "1.23")))
        assertFalse(resultsMatch(expected, wrong, false))
        assertFalse(resultsMatch(expected, wrong, false, relaxed = true))
        assertFalse(resultsMatch(expected, actual.copy(truncated = true), false, relaxed = true))
    }

    @Test fun `relaxed는 서로 다른 컬럼을 대응시키고 행 관계를 보존한다`() {
        val expected = QueryResult(listOf("x", "y"), listOf(listOf("a", "1"), listOf("b", "2")), false, emptyMap())
        val reordered = expected.copy(columns = listOf("y", "extra", "x"),
            rows = listOf(listOf("1", "extra", "a"), listOf("2", "extra", "b")))
        assertTrue(resultsMatch(expected, reordered, true, relaxed = true))
        val scrambled = expected.copy(rows = listOf(listOf("a", "2"), listOf("b", "1")))
        assertFalse(resultsMatch(expected, scrambled, false, relaxed = true))
        val repeated = QueryResult(listOf("x", "y"), listOf(listOf("a", "a")), false, emptyMap())
        assertFalse(resultsMatch(repeated, QueryResult(listOf("one", "extra"), listOf(listOf("a", "b")), false, emptyMap()), false, relaxed = true))
    }

}
