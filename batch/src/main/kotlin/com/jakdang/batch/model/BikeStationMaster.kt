package com.jakdang.batch.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class BikeStationMasterResponse(
    val bikeStationMaster: StationMasterBody
)

@Serializable
data class StationMasterBody(
    @SerialName("list_total_count") val totalCount: Int,
    @SerialName("RESULT") val result: ApiResult,
    val row: List<StationMasterRow> = emptyList()
)

@Serializable
data class StationMasterRow(
    @SerialName("RNTLS_ID") val rntlsId: String,
    @SerialName("ADDR1") val addr1: String? = null,
    @SerialName("ADDR2") val addr2: String? = null,
    @SerialName("LAT") val lat: Double? = null,
    @SerialName("LOT") val lot: Double? = null
)
