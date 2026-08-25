package com.jakdang.batch.job

import com.jakdang.batch.db.MySqlClient
import com.jakdang.batch.db.PostgresClient
import org.slf4j.LoggerFactory

class MartWeatherAnalysisSyncJob(
    private val mysql: MySqlClient,
    private val postgres: PostgresClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartWeatherAnalysisSyncJob::class.java)

    fun execute() {
        log.info("날씨 분석 mart 동기화 시작 runId=$runId")

        postgres.syncHourlyWeatherBike(mysql.readHourlyWeatherBike())
        postgres.syncWeatherBikeStats(mysql.readWeatherBikeStats())

        log.info("날씨 분석 mart 동기화 완료 runId=$runId")
    }
}
