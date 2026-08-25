package com.jakdang.batch.model

/**
 * 기상청 ASOS 종관기상관측 시간자료 1행.
 * observedAt: 'yyyy-MM-dd HH:mm:ss' (TM 변환), 결측(-9/-9.0)은 null.
 */
data class AsosObservation(
    val observedAt: String,
    val stn: Int,
    val temperature: Double?,   // TA 기온(°C)
    val precipitation: Double?, // RN 강수량(mm)
    val windSpeed: Double?,     // WS 풍속(m/s)
    val humidity: Double?       // HM 습도(%)
)
