package com.jakdang.batch.job

import com.jakdang.batch.db.DuckDbClient
import org.slf4j.LoggerFactory

class MartHourlyWeatherBikeJob(
    private val db: DuckDbClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartHourlyWeatherBikeJob::class.java)

    fun execute() {
        log.info("mart hourly_weather_bike 적재 시작 runId=$runId")
        db.loadMartHourlyWeatherBike()
    }
}
