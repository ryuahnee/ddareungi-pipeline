package com.jakdang.batch.job

import com.jakdang.batch.client.AsosClient
import com.jakdang.batch.db.MySqlClient
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * ASOS 시간자료 하루치(date 00:00~23:00)를 MySQL 원장에 backfill.
 * date: yyyyMMdd (Airflow logical_date 기반), stn: 지점번호(서울 108).
 */
class AsosBackfillJob(
    private val client: AsosClient,
    private val mysql: MySqlClient,
    private val runId: String,
    private val date: String,
    private val stn: Int
) {
    private val log = LoggerFactory.getLogger(AsosBackfillJob::class.java)

    suspend fun execute() {
        log.info("ASOS backfill 시작 date=$date stn=$stn runId=$runId")
        val tm1 = "${date}0000"
        val tm2 = "${date}2300"
        val rows = client.fetch(tm1, tm2, stn)
        val collectedAt = LocalDateTime.now().toString()
        mysql.insertWeatherHistory(rows, collectedAt, runId)
        log.info("ASOS backfill 완료 date=$date {}건", rows.size)
    }
}
