package com.jakdang.batch.job

import com.jakdang.batch.client.DdareungiClient
import com.jakdang.batch.db.MySqlClient
import org.slf4j.LoggerFactory

class DdareungiRealtimeSyncJob(
    private val client: DdareungiClient,
    private val mysql: MySqlClient,
    private val runId: String,
    private val collectedAt: String
){
    private val log = LoggerFactory.getLogger(DdareungiRealtimeSyncJob::class.java)

suspend fun execute() {
    log.info("수집 시작 runId=$runId collectedAt=$collectedAt")

    val stations = client.fetchAll()

    log.info("수집 완료 총 {}건", stations.size)
    mysql.insertBikeStatus(stations, collectedAt, runId)
    }
}