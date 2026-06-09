package com.jakdang.batch.job

import com.jakdang.batch.db.DuckDbClient
import org.slf4j.LoggerFactory

class MartWeatherBikeStatsJob(
    private val db: DuckDbClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartWeatherBikeStatsJob::class.java)

    fun execute() {
        log.info("mart weather_bike_stats 적재 시작 runId=$runId")
        db.loadMartWeatherBikeStats()
    }
}
