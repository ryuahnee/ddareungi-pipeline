package com.jakdang.batch.job

import com.jakdang.batch.db.MySqlClient
import com.jakdang.batch.db.PostgresClient
import org.slf4j.LoggerFactory

/**
 * 부족 전환 mart 적재 (MySQL 원장 → Postgres mart).
 * 하루치를 계산해 날짜별로 멱등 동기화한다. 지표 정의는 MySqlClient.readDepletionTransition 한 곳.
 */
class MartDepletionTransitionJob(
    private val mysql: MySqlClient,
    private val postgres: PostgresClient,
    private val date: String
) {
    private val log = LoggerFactory.getLogger(MartDepletionTransitionJob::class.java)

    fun execute() {
        log.info("부족 전환 mart 동기화 시작 date=$date")
        postgres.syncDepletionTransition(date, mysql.readDepletionTransition(date))
        log.info("부족 전환 mart 동기화 완료 date=$date")
    }
}
