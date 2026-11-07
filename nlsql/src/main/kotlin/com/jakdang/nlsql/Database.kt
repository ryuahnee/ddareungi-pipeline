package com.jakdang.nlsql

import java.sql.Connection
import java.sql.DriverManager

class Database(private val url: String, private val user: String, private val password: String) {
    private fun <T> read(block: (Connection) -> T): T {
        DriverManager.getConnection(url, user, password).use { conn ->
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

    fun execute(sql: String, tables: Set<String>, schema: Map<String, List<Pair<String, String>>>): QueryResult = read { conn ->
        conn.createStatement().use { stmt ->
            stmt.queryTimeout = 5
            stmt.maxRows = 101
            val (columns, allRows) = stmt.executeQuery(sql).use { rs ->
                val columns = (1..rs.metaData.columnCount).map { rs.metaData.getColumnLabel(it) }
                val rows = buildList { while (rs.next()) add(columns.indices.map { rs.getString(it + 1) ?: "NULL" }) }
                columns to rows
            }
            val basis = tables.filter { table -> schema[table]?.any { it.first == "collected_at" } == true }
                .associateWith { table ->
                    val quoted = table.replace("\"", "\"\"")
                    stmt.executeQuery("SELECT max(collected_at) FROM public.\"$quoted\"").use { rs ->
                        rs.next()
                        rs.getString(1) ?: "관측 없음"
                    }
                }
            QueryResult(columns, allRows.take(100), allRows.size > 100, basis)
        }
    }
}

data class QueryResult(
    val columns: List<String>, val rows: List<List<String>>, val truncated: Boolean,
    val basis: Map<String, String>
)
