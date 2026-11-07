package com.jakdang.nlsql

import java.net.URI
import java.sql.SQLException
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    fun env(key: String): String = System.getenv(key)?.takeIf { it.isNotBlank() } ?: error("필수 환경변수 누락")
    try {
        val url = env("GRAFANA_DB_URL")
        val key = env("OPENAI_API_KEY")
        val model = env("OPENAI_MODEL")
        val password = env("NL_READER_PASSWORD")
        val db = Database(url, env("NL_READER_USER"), password)
        val schema = db.schema()
        check(schema.isNotEmpty())
        val secrets = listOfNotNull(key, password, System.getenv("TELEGRAM_BOT_TOKEN"),
            URI.create(url.removePrefix("jdbc:")).host) +
            System.getenv().filterKeys { it.endsWith("PASSWORD") || it.endsWith("TOKEN") || it.endsWith("API_KEY") }.values
        fun redact(text: String): String = secrets.filter { it.isNotEmpty() }.fold(text) { out, secret -> out.replace(secret, "[비공개]") }
        fun ask(question: String, html: Boolean): String {
            val generated = try { generateSql(question, schema, key, model) } catch (_: Exception) {
                return "OpenAI SQL 생성에 실패했습니다. 잠시 후 다시 질문해 주세요."
            }
            val check = validateSql(generated.sql)
            if (check.reason != null) return "SQL 검증 거부: ${check.reason}"
            if (check.tables.any { it !in schema }) return "SQL 검증 거부: 조회 가능한 public mart 테이블만 허용합니다."
            val result = try { db.execute(generated.sql, check.tables, schema) } catch (e: SQLException) {
                return if (e.sqlState == "57014") "SQL 실행 제한 시간(5초)을 초과했습니다. 조회 범위를 줄여 주세요."
                    else "SQL 실행에 실패했습니다. 조회 조건이나 데이터 상태를 확인해 주세요."
            } catch (_: Exception) {
                return "SQL 실행에 실패했습니다. 조회 조건이나 데이터 상태를 확인해 주세요."
            }
            return redact(formatAnswer(generated, result, html))
        }
        if (args.firstOrNull() == "--ask" && args.size > 1) {
            println(ask(args.drop(1).joinToString(" "), false))
        } else {
            check(args.isEmpty())
            telegramBot(env("TELEGRAM_BOT_TOKEN"), env("TELEGRAM_CHAT_ID")) { ask(it, true) }
        }
    } catch (_: Exception) {
        System.err.println("nlsql 실행 실패: 환경변수·DB 연결·외부 API 상태를 확인해 주세요.")
        exitProcess(1)
    }
}
