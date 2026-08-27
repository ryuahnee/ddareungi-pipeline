package com.jakdang.batch.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubwayStation(
    @SerialName("outStnNum") val outStnNum: String,
    @SerialName("stnKrNm")  val stnKrNm: String,
    @SerialName("lineNm")   val lineNm: String,
    @SerialName("convX")    val convX: String,  // 경도
    @SerialName("convY")    val convY: String   // 위도
)
