package com.jakdang.batch.client

import com.jakdang.batch.model.SubwayStation
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class SubwayClient(private val apiKey: String) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val TARGET_LINES = setOf(
        "1호선", "2호선", "3호선", "4호선", "5호선",
        "6호선", "7호선", "8호선", "9호선", "9호선(연장)"
    )

    suspend fun fetchAll(): List<SubwayStation> {
        val all: List<SubwayStation> = client.get(
            "http://t-data.seoul.go.kr/apig/apiman-gateway/tapi/TaimsKsccDvSubwayStationGeom/1.0"
        ) {
            parameter("apikey", apiKey)
            parameter("rowCnt", 1000)
        }.body()
        return all.filter { it.lineNm in TARGET_LINES }
    }

    fun close() = client.close()
}
