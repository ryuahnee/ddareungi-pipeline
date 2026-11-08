package com.jakdang.nlsql

import net.sf.jsqlparser.expression.AnalyticType
import net.sf.jsqlparser.expression.AnalyticExpression
import net.sf.jsqlparser.expression.Function
import net.sf.jsqlparser.expression.JsonAggregateFunction
import net.sf.jsqlparser.expression.JsonFunction
import net.sf.jsqlparser.parser.CCJSqlParserTokenManager
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.parser.SimpleCharStream
import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.select.*
import net.sf.jsqlparser.util.TablesNamesFinder
import net.sf.jsqlparser.parser.StringProvider
import java.util.concurrent.Executors

private val safeFunctions = setOf("count", "sum", "avg", "min", "max", "round", "coalesce", "nullif",
    "abs", "ceil", "ceiling", "floor", "date_trunc", "date_part", "to_char", "to_date", "now", "to_timestamp",
    "convert_to", "lower", "upper", "length", "substring", "concat", "greatest", "least")

// A non-null reason means the SQL must never reach JDBC.
data class SqlCheck(val tables: Set<String> = emptySet(), val reason: String? = null, val ordered: Boolean = false)

fun validateSql(sql: String): SqlCheck {
    if (sql.isBlank()) return SqlCheck(reason = "조회만 지원하며 변경·삭제 요청은 거부합니다.")
    return try {
        val parserExecutor = Executors.newSingleThreadExecutor()
        val statements = try {
            CCJSqlParserUtil.parseStatements(sql, parserExecutor, null)
        } finally { parserExecutor.shutdownNow() }
        require(statements.size == 1 && statements.single() is Select) { "SELECT 한 문장만 허용합니다." }
        // Tokenization also covers function calls in ORDER BY / FILTER / window expressions.
        // Comments and string literals cannot hide statement keywords or function identifiers.
        val lexer = CCJSqlParserTokenManager(SimpleCharStream(StringProvider(sql)))
        val forbidden = setOf("set", "show", "insert", "update", "delete", "drop", "create", "alter",
            "truncate", "copy", "call", "do", "execute", "grant", "revoke", "merge", "into", "lock")
        while (true) {
            val token = lexer.nextToken
            if (token.kind == 0) break
            val name = token.image.removeSurrounding("\"").lowercase()
            require(name !in forbidden) { "조회 외 명령은 허용하지 않습니다." }
            require(!listOf("pg_sleep", "pg_read", "pg_ls_", "lo_", "dblink", "pg_terminate", "pg_cancel", "pg_copy", "copy").any { name.startsWith(it) }
                && name !in setOf("set_config", "current_setting")) { "위험 함수는 허용하지 않습니다." }
        }
        val tables = linkedSetOf<String>()
        val finder = object : TablesNamesFinder() {
            private var ctes = mutableSetOf<String>()
            private fun scoped(block: () -> Unit) {
                val parent = ctes
                ctes = parent.toMutableSet()
                try { block() } finally { ctes = parent }
            }
            private fun tail(select: Select) {
                select.orderByElements?.forEach { it.expression.accept(this) }
                select.limit?.rowCount?.accept(this)
                select.limit?.offset?.accept(this)
                select.offset?.offset?.accept(this)
                select.fetch?.expression?.accept(this)
            }
            override fun visit(select: Select) { select.accept(this as SelectVisitor) }
            override fun visit(item: WithItem) {
                require(!item.isRecursive) { "재귀 CTE는 허용하지 않습니다." }
                // A nonrecursive CTE is visible only after its own body has been checked.
                item.select.accept(this as SelectVisitor)
                ctes.add(item.alias.name.removeSurrounding("\""))
            }
            override fun visit(select: ParenthesedSelect) = scoped { super.visit(select); tail(select) }
            override fun visit(select: SetOperationList) = scoped {
                super.visit(select)
                tail(select)
            }
            override fun visit(select: PlainSelect) = scoped {
                require(select.intoTables.isNullOrEmpty() && select.intoTempTable == null && select.forMode == null) {
                    "SELECT INTO 및 행 잠금은 허용하지 않습니다."
                }
                super.visit(select)
                tail(select)
                select.groupBy?.groupByExpressionList?.forEach { it.accept(this) }
                select.groupBy?.groupingSets?.forEach { it.accept(this) }
                select.distinct?.onSelectItems?.forEach { it.expression.accept(this) }
            }
            override fun visit(table: Table) {
                val name = table.name.removeSurrounding("\"")
                val schema = table.schemaName?.removeSurrounding("\"")
                require(table.nameParts.size <= 2 && (schema == null || schema == "public")) {
                    "public 스키마의 mart 테이블만 허용합니다."
                }
                if (schema == null && name in ctes) return
                require(name.startsWith("mart_")) { "mart 테이블만 조회할 수 있습니다." }
                tables.add(name)
            }
            override fun visit(function: Function) {
                // Unknown/user-defined functions may execute SQL internally; allow only these built-ins.
                require(function.name.lowercase() in safeFunctions) { "허용되지 않은 함수입니다." }
                super.visit(function)
                function.orderByElements?.forEach { it.expression.accept(this) }
            }
            override fun visit(values: Values) { error("SELECT 한 문장만 허용합니다.") }
            override fun visit(expression: JsonAggregateFunction) { error("JSON 집계는 MVP에서 지원하지 않습니다.") }
            override fun visit(expression: JsonFunction) { error("JSON 함수는 MVP에서 지원하지 않습니다.") }
            override fun visit(function: TableFunction) { error("테이블 함수는 허용하지 않습니다.") }
            override fun visit(expression: AnalyticExpression) {
                require(expression.type == AnalyticType.FILTER_ONLY && expression.name.lowercase() in safeFunctions) {
                    "허용되지 않은 함수 또는 윈도 표현식입니다."
                }
                expression.expression?.accept(this)
                expression.filterExpression?.accept(this)
                expression.funcOrderBy?.forEach { it.expression.accept(this) }
            }
        }
        finder.getTables(statements.single())
        val select = statements.single() as Select
        SqlCheck(tables, ordered = !select.orderByElements.isNullOrEmpty() && (select.limit != null || select.fetch != null))
    } catch (e: IllegalArgumentException) {
        SqlCheck(reason = e.message ?: "허용하지 않는 SQL입니다.")
    } catch (e: IllegalStateException) {
        SqlCheck(reason = e.message ?: "허용하지 않는 SQL입니다.")
    } catch (_: Exception) {
        SqlCheck(reason = "SQL을 안전하게 해석할 수 없습니다.")
    }
}
