package com.jakdang.batch.job

import com.jakdang.batch.db.MySqlClient
import com.jakdang.batch.db.PostgresClient
import org.slf4j.LoggerFactory

class MartHolidayAnalysisSyncJob(
    private val mysql: MySqlClient,
    private val postgres: PostgresClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartHolidayAnalysisSyncJob::class.java)

    fun execute() {
        log.info("공휴일 분석 mart 동기화 시작 runId=$runId")

        postgres.syncHolidayBikeStats(mysql.readHolidayBikeStats())
        postgres.syncStationHolidayDepletion(mysql.readStationHolidayDepletion())

        log.info("공휴일 분석 mart 동기화 완료 runId=$runId")
    }
}
