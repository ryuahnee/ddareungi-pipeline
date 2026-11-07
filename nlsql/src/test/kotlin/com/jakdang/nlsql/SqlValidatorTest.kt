package com.jakdang.nlsql

import org.junit.jupiter.api.Test
import kotlin.test.*

class SqlValidatorTest {
    @Test fun `조회 외 명령과 다중 문장을 거부한다`() {
        listOf(
            "DELETE FROM mart_station_snapshot", "UPDATE mart_station_snapshot SET shared=0",
            "DROP TABLE mart_station_snapshot", "SELECT 1; DROP TABLE mart_station_snapshot",
            "SELECT (", "SET statement_timeout=0", "SELECT 1 /* */; DELETE FROM mart_station_snapshot",
            "COPY mart_station_snapshot TO STDOUT", "SHOW statement_timeout", "CALL foo()",
            "SELECT * INTO mart_copy FROM mart_station_snapshot",
            "SELECT * FROM mart_station_snapshot FOR UPDATE",
            "WITH gone AS (DELETE FROM mart_station_snapshot RETURNING *) SELECT * FROM gone"
        ).forEach { assertNotNull(validateSql(it).reason, it) }
    }

    @Test fun `허용하지 않은 테이블과 스키마를 거부한다`() {
        listOf(
            "SELECT * FROM dim_holiday", "SELECT * FROM pg_catalog.pg_roles",
            "SELECT * FROM information_schema.tables", "SELECT * FROM other.mart_station_snapshot",
            "SELECT * FROM mart_station_snapshot JOIN dim_holiday ON true",
            "SELECT * FROM mart_station_snapshot WHERE EXISTS (SELECT 1 FROM pg_catalog.pg_roles)",
            "SELECT * FROM mart_station_snapshot ORDER BY (SELECT count(*) FROM dim_holiday)",
            "SELECT 1 UNION SELECT 2 LIMIT (SELECT count(*) FROM dim_holiday)",
            "SELECT station_id FROM mart_station_snapshot GROUP BY GROUPING SETS ((station_id), ((SELECT count(*) FROM dim_holiday)))",
            "SELECT count(*) FILTER (WHERE EXISTS (SELECT 1 FROM dim_holiday)) FROM mart_station_snapshot",
            "WITH dim_holiday AS (SELECT * FROM dim_holiday) SELECT * FROM dim_holiday",
            "WITH x AS (SELECT * FROM mart_station_snapshot) SELECT * FROM public.x",
            "SELECT * FROM (WITH x AS (SELECT 1) SELECT * FROM x) sub JOIN x ON true"
        ).forEach { assertNotNull(validateSql(it).reason, it) }
    }

    @Test fun `위험 함수를 모든 절에서 거부한다`() {
        listOf("pg_sleep(10)", "pg_sleep_for('1 second')", "pg_read_file('/etc/passwd')",
            "pg_ls_dir('/')", "lo_export(1, '/tmp/file')", "dblink_connect('x')",
            "set_config('statement_timeout','0',false)", "current_setting('data_directory')",
            "query_to_xml('SELECT * FROM pg_catalog.pg_roles',true,false,'')",
            "inet_server_addr()", "public.custom_function()", "pg_terminate_backend(1)", "pg_cancel_backend(1)", "pg_catalog.\"pg_sleep\"(10)"
        ).forEach { fn ->
            assertNotNull(validateSql("SELECT $fn FROM mart_station_snapshot").reason, fn)
            assertNotNull(validateSql("SELECT station_id FROM mart_station_snapshot ORDER BY $fn").reason, fn)
        }
        assertNotNull(validateSql("SELECT pg_sleep /* comment */ (10)").reason)
    }

    @Test fun `mart 조회와 조인과 CTE를 허용하고 원본 테이블을 반환한다`() {
        val simple = validateSql("SELECT * FROM public.mart_station_snapshot WHERE shared < 10;")
        assertNull(simple.reason)
        assertEquals(setOf("mart_station_snapshot"), simple.tables)
        val join = validateSql("SELECT s.station_id FROM mart_station_snapshot s JOIN mart_depletion_alert a ON s.station_id=a.station_id")
        assertNull(join.reason)
        assertEquals(setOf("mart_station_snapshot", "mart_depletion_alert"), join.tables)
        val cte = validateSql("WITH x AS (SELECT * FROM mart_station_snapshot), y AS (SELECT * FROM x) SELECT count(*) FROM y")
        assertNull(cte.reason)
        assertEquals(setOf("mart_station_snapshot"), cte.tables)
        assertNull(validateSql("SELECT 'pg_sleep(10); DELETE' AS description FROM mart_station_snapshot").reason)
        assertNull(validateSql("SELECT count(*) FILTER (WHERE is_transition) FROM mart_depletion_transition").reason)
    }

    @Test fun `now와 to_timestamp를 허용한다`() {
        assertNull(validateSql("SELECT now(), to_timestamp(0) FROM public.mart_station_snapshot LIMIT 1").reason)
    }

    @Test fun `사전 예시 SQL을 허용한다`() {
        val examples = checkNotNull(javaClass.getResource("/glossary.md")).readText()
            .lineSequence().filter { it.startsWith("SELECT ") || it.startsWith("WITH ") }.toList()
        assertEquals(5, examples.size)
        examples.forEach { assertNull(validateSql(it).reason, it) }
    }
}
