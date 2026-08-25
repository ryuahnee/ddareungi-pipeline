package com.jakdang.batch.client

import com.jakdang.batch.model.HolidayItem
import com.jakdang.batch.model.HolidayResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class HolidayClient(private val apiKey: String) {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val baseUrl = "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo"

    suspend fun fetch(year: String, month: String): List<HolidayItem> {
        val response: HolidayResponse = client.get(baseUrl) {
            parameter("serviceKey", apiKey)
            parameter("solYear", year)
            parameter("solMonth", month.padStart(2, '0'))
            parameter("numOfRows", 50)
            parameter("_type", "json")
        }.body()

        val header = response.response.header
        if (header.resultCode != "00") {
            error("공휴일 API 오류 [${header.resultCode}]: ${header.resultMsg}")
        }

        val body = response.response.body ?: return emptyList()
        if (body.totalCount == 0) return emptyList()

        // items가 빈 문자열이거나 null인 경우 처리
        val itemsElement = body.items ?: return emptyList()
        if (itemsElement is JsonPrimitive) return emptyList()

        val itemElement = (itemsElement as? JsonObject)?.get("item") ?: return emptyList()

        return when (itemElement) {
            is JsonArray  -> json.decodeFromJsonElement(itemElement)
            is JsonObject -> listOf(json.decodeFromJsonElement(itemElement))
            else          -> emptyList()
        }
    }

    fun close() = client.close()
}
