package com.jakdang.batch.job

import com.jakdang.batch.db.MySqlClient
import com.jakdang.batch.db.PostgresClient
import org.slf4j.LoggerFactory

/**
 * SCD 버전별 평균 거치율 mart (MySQL 원장 → Postgres).
 * 변경 이력 있는 대여소만 대상이라 소량. 매일 1회(station_master DAG 뒤) 재계산.
 */
class MartStationScdSyncJob(
    private val mysql: MySqlClient,
    private val postgres: PostgresClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartStationScdSyncJob::class.java)

    fun execute() {
        log.info("SCD 버전별 거치율 mart 동기화 시작 runId=$runId")
        postgres.syncStationVersionUsage(mysql.readMartStationVersionUsage())
        log.info("SCD 버전별 거치율 mart 동기화 완료 runId=$runId")
    }
}
