package com.jakdang.batch.client

import com.jakdang.batch.model.WeatherResponse
import com.jakdang.batch.model.WeatherSnapshot
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class WeatherClient(private val apiKey: String) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val baseUrl = "https://apihub.kma.go.kr/api/typ02/openApi/VilageFcstInfoService_2.0/getUltraSrtNcst"
    private val nx = 60
    private val ny = 127

    suspend fun fetch(baseDate: String, baseTime: String): WeatherSnapshot {
        val response: WeatherResponse = client.get(baseUrl) {
            parameter("pageNo", 1)
            parameter("numOfRows", 100)
            parameter("dataType", "JSON")
            parameter("base_date", baseDate)
            parameter("base_time", baseTime)
            parameter("nx", nx)
            parameter("ny", ny)
            parameter("authKey", apiKey)
        }.body()

        val header = response.response.header
        if (header.resultCode != "00") {
            error("날씨 API 오류 [${header.resultCode}]: ${header.resultMsg}")
        }

        val items = response.response.body!!.items.item
        val map = items.associate { it.category to it.obsrValue }

        return WeatherSnapshot(
            baseDate     = baseDate,
            baseTime     = baseTime,
            nx           = nx,
            ny           = ny,
            temperature  = map["T1H"]?.toDoubleOrNull() ?: 0.0,
            precipitation = map["RN1"]?.toDoubleOrNull() ?: 0.0,
            precipType   = map["PTY"]?.toIntOrNull() ?: 0,
            windSpeed    = map["WSD"]?.toDoubleOrNull() ?: 0.0,
            humidity     = map["REH"]?.toIntOrNull() ?: 0
        )
    }

    fun close() = client.close()
}
