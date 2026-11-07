package com.jakdang.nlsql

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    fun env(key: String): String = System.getenv(key)?.takeIf { it.isNotBlank() } ?: error("필수 환경변수 누락")
    try {
        val url = env("GRAFANA_DB_URL")
        val key = env("OPENAI_API_KEY")
        val model = if (args.firstOrNull() == "--eval" && args.size == 3 && args[1] == "--model") args[2] else env("OPENAI_MODEL")
        val db = Database(url, env("NL_READER_USER"), env("NL_READER_PASSWORD"))
        val schema = db.schema()
        check(schema.isNotEmpty())
        val redact = secretRedactor()
        val logger = QueryLogger(url, System.getenv("GRAFANA_DB_USER"), System.getenv("GRAFANA_DB_PASSWORD"), redact)
        val runner = QueryRunner(db, schema, key, model, logger)
        fun ask(question: String, source: String, html: Boolean): String {
            val attempt = runner.run(question, source)
            return redact(attempt.reason ?: formatAnswer(checkNotNull(attempt.generated), checkNotNull(attempt.result), html))
        }
        when (args.firstOrNull()) {
            "--ask" -> {
                check(args.size > 1)
                println(ask(args.drop(1).joinToString(" "), "cli", false))
            }
            "--eval" -> {
                check(args.size == 1 || (args.size == 3 && args[1] == "--model"))
                evaluate(model, runner, db, schema, redact)
            }
            else -> {
                check(args.isEmpty())
                telegramBot(env("TELEGRAM_BOT_TOKEN"), env("TELEGRAM_CHAT_ID")) { ask(it, "telegram", true) }
            }
        }
    } catch (e: EvalPreparationFailure) {
        System.err.println("평가 준비 실패: ${e.message}")
        exitProcess(1)
    } catch (_: Exception) {
        System.err.println("nlsql 실행 실패: 환경변수·DB 연결·외부 API 상태를 확인해 주세요.")
        exitProcess(1)
    }
}
