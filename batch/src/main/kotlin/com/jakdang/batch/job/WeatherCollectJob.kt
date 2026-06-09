package com.jakdang.batch.job

import com.jakdang.batch.client.WeatherClient
import com.jakdang.batch.db.DuckDbClient
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WeatherCollectJob(
    private val client: WeatherClient,
    private val db: DuckDbClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(WeatherCollectJob::class.java)

    suspend fun execute() {
        val now = LocalDateTime.now()
        val baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val baseTime = now.format(DateTimeFormatter.ofPattern("HH")) + "00"
        val collectedAt = now.toString()
        val observedAt = now.withMinute(0).withSecond(0).withNano(0).toString()

        log.info("날씨 수집 시작 runId=$runId baseDate=$baseDate baseTime=$baseTime")

        val snapshot = client.fetch(baseDate, baseTime)
        db.insertWeather(snapshot, observedAt, collectedAt, runId)

        log.info("날씨 수집 완료")
    }
}
