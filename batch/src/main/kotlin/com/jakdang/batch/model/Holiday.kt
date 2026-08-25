package com.jakdang.batch.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class HolidayResponse(
    val response: HolidayBody
)

@Serializable
data class HolidayBody(
    val header: HolidayHeader,
    val body: HolidayData? = null
)

@Serializable
data class HolidayHeader(
    val resultCode: String,
    val resultMsg: String
)

@Serializable
data class HolidayData(
    val items: JsonElement? = null,
    val totalCount: Int = 0
)

@Serializable
data class HolidayItem(
    val dateKind: String,
    val dateName: String,
    val isHoliday: String,
    val locdate: Int,
    val seq: Int
)
