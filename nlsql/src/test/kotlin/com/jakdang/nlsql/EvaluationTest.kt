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
}
