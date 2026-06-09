package com.jakdang.batch.job

import com.jakdang.batch.db.DuckDbClient
import org.slf4j.LoggerFactory

class MartHotSunnyStationJob(
    private val db: DuckDbClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartHotSunnyStationJob::class.java)

    fun execute() {
        log.info("mart hot_sunny_station_stats 적재 시작 runId=$runId")
        db.loadMartHotSunnyStationStats()
    }
}
