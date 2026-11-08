package com.jakdang.nlsql

import java.net.URI
import java.sql.DriverManager
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Properties

fun secretRedactor(environment: Map<String, String> = System.getenv()): (String) -> String {
    val secrets = environment.filterKeys { it.endsWith("PASSWORD") || it.endsWith("TOKEN") || it.endsWith("API_KEY") }.values +
        environment.filterKeys { it.endsWith("URL") }.values.flatMap { value ->
            listOfNotNull(value, runCatching { URI.create(value.removePrefix("jdbc:")).host }.getOrNull())
        }
    return { text -> secrets.filter { it.isNotEmpty() }.sortedByDescending { it.length }
        .fold(text) { out, secret -> out.replace(secret, "[비공개]") } }
}

data class QueryLog(
    val source: String, val question: String, val model: String,
    val askedAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    var generatedSql: String? = null, var guardPassed: Boolean? = null, var rejectReason: String? = null,
    var rowCount: Int? = null, var truncated: Boolean? = null,
    var llmLatencyMs: Int? = null, var dbLatencyMs: Int? = null,
    var promptTokens: Int? = null, var completionTokens: Int? = null,
    var dataAsOf: LocalDateTime? = null, var error: String? = null
)

class QueryLogger(private val url: String, private val user: String?, private val password: String?,
                  private val redact: (String) -> String) {
    fun write(log: QueryLog) {
        try {
            check(user == "grafana" && password != null)
            val props = Properties().apply {
                setProperty("user", user); setProperty("password", password)
                setProperty("connectTimeout", "5"); setProperty("socketTimeout", "5")
            }
            DriverManager.getConnection(url, props).use { conn ->
                conn.prepareStatement("""
                    INSERT INTO public.nl_query_log
                    (asked_at,source,question,model,generated_sql,guard_passed,reject_reason,row_count,truncated,
                     llm_latency_ms,db_latency_ms,prompt_tokens,completion_tokens,data_as_of,error)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """.trimIndent()).use { stmt ->
                    stmt.queryTimeout = 5
                    val values = listOf(log.askedAt, log.source, redact(log.question), redact(log.model),
                        log.generatedSql?.let(redact), log.guardPassed, log.rejectReason?.let(redact),
                        log.rowCount, log.truncated, log.llmLatencyMs, log.dbLatencyMs,
                        log.promptTokens, log.completionTokens, log.dataAsOf, log.error?.let(redact))
                    values.forEachIndexed { i, value -> stmt.setObject(i + 1, value) }
                    stmt.executeUpdate()
                }
            }
        } catch (_: Exception) {
            System.err.println("질문 로그 기록 실패: 답변은 계속합니다.")
        }
    }
}
