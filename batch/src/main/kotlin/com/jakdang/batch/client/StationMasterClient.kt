package com.jakdang.batch.client


import com.jakdang.batch.model.BikeStationMasterResponse
import com.jakdang.batch.model.StationMasterRow
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

// 대여소 마스터 정보 api 호출 (bikeStationMaster)

class StationMasterClient(private val apiKey: String) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private suspend fun fetchPage(start: Int, end: Int): BikeStationMasterResponse {
        val url = "http://openapi.seoul.go.kr:8088/$apiKey/json/bikeStationMaster/$start/$end/"
        val response: BikeStationMasterResponse = client.get(url).body()

        val code = response.bikeStationMaster.result.code
        if (code != "INFO-000") {
            error("API 오류 [$code]: ${response.bikeStationMaster.result.message}")
        }
        return response
    }

    suspend fun fetchAll(): List<StationMasterRow> {
        val pageSize = 1000
        val result = mutableListOf<StationMasterRow>()
        var start = 1

        while (true) {
            val end = start + pageSize - 1
            val page = fetchPage(start, end)
            val rows = page.bikeStationMaster.row
            result.addAll(rows)

            if (rows.size < pageSize) break  // 마지막 페이지
            start += pageSize
        }

        return result
    }

    fun close() = client.close()
}
