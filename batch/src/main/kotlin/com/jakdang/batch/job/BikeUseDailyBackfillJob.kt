package com.jakdang.batch.job

import com.jakdang.batch.client.BikeUseDailyClient
import com.jakdang.batch.db.MySqlClient
import org.slf4j.LoggerFactory

/**
 * 일별 이용정보 백필 (tbCycleRentUseDayInfo → MySQL bike_use_daily).
 * date: yyyyMMdd. 멱등: 해당 일자 DELETE 후 INSERT.
 */
class BikeUseDailyBackfillJob(
    private val client: BikeUseDailyClient,
    private val mysql: MySqlClient,
    private val runId: String,
    private val date: String,
    private val collectedAt: String
) {
    private val log = LoggerFactory.getLogger(BikeUseDailyBackfillJob::class.java)

    suspend fun execute() {
        log.info("일별 이용정보 백필 시작 date=$date runId=$runId")

        val rows = client.fetchAll(date)
        log.info("수집 완료 {}건", rows.size)

        val rentDt = "${date.substring(0, 4)}-${date.substring(4, 6)}-${date.substring(6, 8)}"
        mysql.insertBikeUseDaily(rentDt, rows, collectedAt, runId)
    }
}
