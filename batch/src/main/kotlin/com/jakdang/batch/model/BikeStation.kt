package com.jakdang.batch.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class BikeStationResponse(
    val rentBikeStatus: RentBikeStatus
)

@Serializable
data class ApiResult(
    @SerialName("CODE") val code: String,
    @SerialName("MESSAGE") val message: String
)

@Serializable
data class RentBikeStatus(
    @SerialName("list_total_count") val totalCount: Int,
    @SerialName("RESULT") val result: ApiResult,
    val row: List<BikeStationRow>
)

@Serializable
data class BikeStationRow(
    val stationId: String,
    val stationName: String,
    val rackTotCnt: String,
    val parkingBikeTotCnt: String,
    val shared: String,
    val stationLatitude: String,
    val stationLongitude: String
)

