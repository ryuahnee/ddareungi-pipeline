package com.jakdang.batch.job

import com.jakdang.batch.db.DuckDbClient
import org.slf4j.LoggerFactory

class MartDepletionWithWeatherJob(
    private val db: DuckDbClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartDepletionWithWeatherJob::class.java)

    fun execute() {
        log.info("mart depletion_with_weather 적재 시작 runId=$runId")
        db.loadMartDepletionWithWeather(runId)
    }
}
