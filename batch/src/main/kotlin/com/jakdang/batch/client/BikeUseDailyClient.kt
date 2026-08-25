package com.jakdang.batch.client


import com.jakdang.batch.model.BikeUseDayResponse
import com.jakdang.batch.model.BikeUseDayRow
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

// 일별 이용정보 api 호출 (URL: tbCycleRentUseDayInfo, 응답 키: cycleRentUseDayInfo — 이름 다름 주의)

class BikeUseDailyClient(private val apiKey: String) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private suspend fun fetchPage(date: String, start: Int, end: Int): BikeUseDayResponse {
        val url = "http://openapi.seoul.go.kr:8088/$apiKey/json/tbCycleRentUseDayInfo/$start/$end/$date/"
        val response: BikeUseDayResponse = client.get(url).body()

        val code = response.cycleRentUseDayInfo.result.code
        if (code != "INFO-000") {
            error("API 오류 [$code]: ${response.cycleRentUseDayInfo.result.message}")
        }
        return response
    }

    /** 특정 일자(yyyyMMdd) 전체 페이지 수집 */
    suspend fun fetchAll(date: String): List<BikeUseDayRow> {
        val pageSize = 1000
        val result = mutableListOf<BikeUseDayRow>()
        var start = 1

        while (true) {
            val end = start + pageSize - 1
            val page = fetchPage(date, start, end)
            val rows = page.cycleRentUseDayInfo.row
            result.addAll(rows)

            if (rows.size < pageSize) break  // 마지막 페이지
            start += pageSize
        }

        return result
    }

    fun close() = client.close()
}
