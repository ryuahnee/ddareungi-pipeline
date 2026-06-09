package com.jakdang.batch.job

import com.jakdang.batch.db.DuckDbClient
import com.jakdang.batch.db.PostgresClient
import org.slf4j.LoggerFactory

class MartSyncJob(
    private val duckDb: DuckDbClient,
    private val postgres: PostgresClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartSyncJob::class.java)

    fun execute() {
        log.info("mart PostgreSQL 동기화 시작 runId=$runId")

        postgres.syncMartSnapshot(duckDb.readMartSnapshot())
        postgres.syncMartAlert("mart_depletion_alert", duckDb.readMartDepletionAlert(runId))
        postgres.syncMartAlert("mart_congestion_alert", duckDb.readMartCongestionAlert(runId))
        postgres.syncWeatherBikeStats(duckDb.readMartWeatherBikeStats())
        postgres.syncWeatherDepletion(duckDb.readMartWeatherDepletion())
        postgres.syncBikeMovement(duckDb.readMartBikeMovement())

        log.info("mart PostgreSQL 동기화 완료 runId=$runId")
    }
}
