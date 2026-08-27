package com.jakdang.batch.job

import com.jakdang.batch.db.MySqlClient
import com.jakdang.batch.db.PostgresClient
import org.slf4j.LoggerFactory

class MartSubwayRushDepletionJob(
    private val mysql: MySqlClient,
    private val postgres: PostgresClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartSubwayRushDepletionJob::class.java)

    fun execute() {
        log.info("지하철역 러시아워 고갈율 mart 동기화 시작 runId=$runId")
        postgres.syncSubwayRushDepletion(mysql.readSubwayRushDepletion())
        log.info("mart_subway_rush_depletion 동기화 완료 runId=$runId")
    }
}
