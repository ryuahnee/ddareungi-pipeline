package com.jakdang.batch.client


import com.jakdang.batch.model.BikeStationResponse
import com.jakdang.batch.model.BikeStationRow
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

// api 호출

class DdareungiClient(private val apiKey: String) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private suspend fun fetchPage(start: Int, end: Int): BikeStationResponse {
        val url = "http://openapi.seoul.go.kr:8088/$apiKey/json/bikeList/$start/$end/"
        val response: BikeStationResponse = client.get(url).body()

        val code = response.rentBikeStatus.result.code
        if(code != "INFO-000"){
            error("API 오류 [$code]: ${response.rentBikeStatus.result.message}")
        }
        return response
    }

    suspend fun fetchAll(): List<BikeStationRow> {
        val pageSize = 1000
        val result = mutableListOf<BikeStationRow>()
        var start = 1

        while (true) {
            val end = start + pageSize - 1
            val page = fetchPage(start, end)
            val rows = page.rentBikeStatus.row
            result.addAll(rows)

            if (rows.size < pageSize) break  // 마지막 페이지
            start += pageSize
        }

        return result
    }

    fun close() = client.close()
}
