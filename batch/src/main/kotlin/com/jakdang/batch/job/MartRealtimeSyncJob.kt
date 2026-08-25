package com.jakdang.batch.job

import com.jakdang.batch.db.MySqlClient
import com.jakdang.batch.db.PostgresClient
import org.slf4j.LoggerFactory

/**
 * 실시간 스냅샷 mart 동기화 (MySQL 원장 → Postgres mart).
 * DuckDB 폐기 전환 파일럿: snapshot, depletion_alert, congestion_alert.
 * Postgres 쓰기(syncMartSnapshot/syncMartAlert)와 mart 스키마는 기존 그대로 재사용.
 */
class MartRealtimeSyncJob(
    private val mysql: MySqlClient,
    private val postgres: PostgresClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartRealtimeSyncJob::class.java)

    fun execute() {
        log.info("실시간 mart 동기화 시작 (MySQL→Postgres) runId=$runId")

        postgres.syncMartSnapshot(mysql.readMartSnapshot(runId))
        postgres.syncMartAlert("mart_depletion_alert", mysql.readMartDepletionAlert(runId))
        postgres.syncMartAlert("mart_congestion_alert", mysql.readMartCongestionAlert(runId))
        postgres.syncWeatherDepletion(mysql.readWeatherDepletion(runId))
        postgres.syncDepletionWithWeather(mysql.readDepletionWithWeather(runId))

        log.info("실시간 mart 동기화 완료 runId=$runId")
    }
}
