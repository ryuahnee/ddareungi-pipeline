package com.jakdang.batch.job

import com.jakdang.batch.client.StationMasterClient
import com.jakdang.batch.db.MySqlClient
import org.slf4j.LoggerFactory

class StationMasterSyncJob(
    private val client: StationMasterClient,
    private val mysql: MySqlClient,
    private val runId: String,
    private val now: String
) {
    private val log = LoggerFactory.getLogger(StationMasterSyncJob::class.java)

    suspend fun execute() {
        log.info("대여소 마스터 수집 시작 runId=$runId now=$now")

        val stations = client.fetchAll()
        log.info("수집 완료 총 {}건", stations.size)

        val (inserted, changed, retired) = mysql.upsertStationScd(stations, now, runId)
        log.info("SCD 적재 완료: 신규 {} 변경 {} 폐지 {}", inserted, changed, retired)
    }
}
