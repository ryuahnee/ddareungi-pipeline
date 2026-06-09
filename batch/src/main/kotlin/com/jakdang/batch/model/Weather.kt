package com.jakdang.batch.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val response: WeatherBody
)

@Serializable
data class WeatherBody(
    val header: WeatherHeader,
    val body: WeatherData? = null
)

@Serializable
data class WeatherHeader(
    val resultCode: String,
    val resultMsg: String
)

@Serializable
data class WeatherData(
    val items: WeatherItems
)

@Serializable
data class WeatherItems(
    val item: List<WeatherItem>
)

@Serializable
data class WeatherItem(
    val baseDate: String,
    val baseTime: String,
    val category: String,
    val nx: Int,
    val ny: Int,
    val obsrValue: String
)

data class WeatherSnapshot(
    val baseDate: String,
    val baseTime: String,
    val nx: Int,
    val ny: Int,
    val temperature: Double,    // T1H 기온
    val precipitation: Double,  // RN1 강수량
    val precipType: Int,        // PTY 강수형태 (0:없음 1:비 2:비/눈 3:눈)
    val windSpeed: Double,      // WSD 풍속
    val humidity: Int           // REH 습도
)
