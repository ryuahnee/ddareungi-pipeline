package com.jakdang.batch.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class BikeUseDayResponse(
    val cycleRentUseDayInfo: BikeUseDayBody
)

@Serializable
data class BikeUseDayBody(
    @SerialName("list_total_count") val totalCount: String,
    @SerialName("RESULT") val result: ApiResult,
    val row: List<BikeUseDayRow> = emptyList()
)

@Serializable
data class BikeUseDayRow(
    @SerialName("RENT_DT") val rentDt: String,
    @SerialName("RENT_ID") val rentId: String,
    @SerialName("RENT_NM") val rentNm: String? = null,
    @SerialName("RENT_TYPE") val rentType: String? = null,
    @SerialName("GENDER_CD") val genderCd: String? = null,
    @SerialName("AGE_TYPE") val ageType: String? = null,
    @SerialName("USE_CNT") val useCnt: String? = null,
    @SerialName("EXER_AMT") val exerAmt: String? = null,
    @SerialName("CARBON_AMT") val carbonAmt: String? = null,
    @SerialName("MOVE_METER") val moveMeter: String? = null,
    @SerialName("MOVE_TIME") val moveTime: String? = null
)
