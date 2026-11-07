package com.jakdang.nlsql

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Types

class Database(private val url: String, private val user: String, private val password: String) {
    private fun <T> read(block: (Connection) -> T): T {
        DriverManager.getConnection(url, user, password).use { conn ->
            conn.transactionIsolation = Connection.TRANSACTION_REPEATABLE_READ
            conn.isReadOnly = true
            conn.autoCommit = false
            try {
                conn.createStatement().use { stmt ->
                    stmt.queryTimeout = 5
                    stmt.execute("SET LOCAL statement_timeout='5s'")
                    stmt.execute("SET LOCAL search_path=pg_catalog, public")
                    stmt.executeQuery("SELECT current_user").use { rs ->
                        check(rs.next() && rs.getString(1) == "nl_reader") { "읽기 전용 계정이 필요합니다." }
                    }
                }
                return block(conn)
            } finally { conn.rollback() }
        }
    }

    fun schema(): Map<String, List<Pair<String, String>>> = read { conn ->
        conn.createStatement().use { stmt ->
            stmt.queryTimeout = 5
            stmt.executeQuery("""
                SELECT c.table_name, c.column_name, c.data_type
                FROM information_schema.columns c
                JOIN information_schema.tables t USING (table_catalog, table_schema, table_name)
                WHERE c.table_schema='public' AND c.table_name LIKE 'mart\_%' ESCAPE '\'
                  AND t.table_type='BASE TABLE'
                ORDER BY c.table_name, c.ordinal_position
            """.trimIndent()).use { rs ->
                val tables = linkedMapOf<String, MutableList<Pair<String, String>>>()
                while (rs.next()) tables.getOrPut(rs.getString(1)) { mutableListOf() }.add(rs.getString(2) to rs.getString(3))
                tables
            }
        }
    }

    fun execute(sql: String, tables: Set<String>, schema: Map<String, List<Pair<String, String>>>): QueryResult =
        read { conn -> execute(conn, sql, tables, schema) }

    fun evaluate(sql: String, expectedSql: String, tables: Set<String>, expectedTables: Set<String>,
                 schema: Map<String, List<Pair<String, String>>>): Pair<QueryResult, QueryResult> = read { conn ->
        val expected = execute(conn, expectedSql, expectedTables, schema)
        execute(conn, sql, tables, schema) to expected
    }

    private fun execute(conn: Connection, sql: String, tables: Set<String>,
                        schema: Map<String, List<Pair<String, String>>>): QueryResult {
        val started = System.nanoTime()
        conn.createStatement().use { stmt ->
            stmt.queryTimeout = 5
            stmt.maxRows = 101
            val result = stmt.executeQuery(sql).use { rs ->
                val meta = rs.metaData
                val columns = (1..meta.columnCount).map { meta.getColumnLabel(it) }
                val numericTypes = setOf(Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT,
                    Types.NUMERIC, Types.DECIMAL, Types.REAL, Types.FLOAT, Types.DOUBLE)
                val numericColumns = columns.indices.filter { meta.getColumnType(it + 1) in numericTypes }.toSet()
                val rows = buildList { while (rs.next()) add(columns.indices.map { rs.getString(it + 1) }) }
                QueryResult(columns, rows.take(100), rows.size > 100, emptyMap(), numericColumns)
            }
            val basis = tables.filter { table -> schema[table]?.any { it.first == "collected_at" } == true }
                .associateWith { table ->
                    val quoted = table.replace("\"", "\"\"")
                    stmt.executeQuery("SELECT max(collected_at) FROM public.\"$quoted\"").use { rs ->
                        rs.next()
                        rs.getString(1) ?: "관측 없음"
                    }
                }
            return result.copy(basis = basis, latencyMs = elapsedMs(started))
        }
    }
}

data class QueryResult(
    val columns: List<String>, val rows: List<List<String?>>, val truncated: Boolean,
    val basis: Map<String, String>, val numericColumns: Set<Int> = emptySet(), val latencyMs: Int = 0
)

fun elapsedMs(started: Long): Int = ((System.nanoTime() - started) / 1_000_000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
