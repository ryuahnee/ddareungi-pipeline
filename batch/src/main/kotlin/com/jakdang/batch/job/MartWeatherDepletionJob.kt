package com.jakdang.batch.job

import com.jakdang.batch.db.DuckDbClient
import org.slf4j.LoggerFactory

class MartWeatherDepletionJob(
    private val db: DuckDbClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartWeatherDepletionJob::class.java)

    fun execute() {
        log.info("mart weather_depletion 적재 시작 runId=$runId")
        db.loadMartWeatherDepletion(runId)
    }
}
