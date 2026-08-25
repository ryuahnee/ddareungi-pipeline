package com.jakdang.batch.client

import com.jakdang.batch.model.AsosObservation
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.slf4j.LoggerFactory

/**
 * 기상청 ASOS 종관기상관측 시간자료(기간조회, kma_sfctm3).
 * 응답은 공백 구분 고정폭 텍스트. 주석(#)은 제외, 결측(-9/-9.0)은 null.
 */
class AsosClient(private val apiKey: String) {
    private val log = LoggerFactory.getLogger(AsosClient::class.java)
    private val client = HttpClient(CIO)
    private val baseUrl = "https://apihub.kma.go.kr/api/typ01/url/kma_sfctm3.php"

    // 응답 컬럼 인덱스 (공백 split 기준)
    private companion object {
        const val IDX_TM = 0
        const val IDX_STN = 1
        const val IDX_WS = 3
        const val IDX_TA = 11
        const val IDX_HM = 13
        const val IDX_RN = 15
    }

    /** tm1~tm2 (yyyyMMddHHmm) 구간의 stn 지점 시간자료 조회 */
    suspend fun fetch(tm1: String, tm2: String, stn: Int): List<AsosObservation> {
        val text = client.get(baseUrl) {
            parameter("tm1", tm1)
            parameter("tm2", tm2)
            parameter("stn", stn)
            parameter("help", 0)
            parameter("authKey", apiKey)
        }.bodyAsText()

        val rows = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { parseLine(it) }
            .toList()

        log.info("ASOS 조회 완료 {}건 (tm1={}, tm2={}, stn={})", rows.size, tm1, tm2, stn)
        return rows
    }

    private fun parseLine(line: String): AsosObservation? {
        val c = line.split(Regex("\\s+"))
        if (c.size <= IDX_RN) return null
        val tm = c[IDX_TM]
        if (tm.length != 12) return null
        return AsosObservation(
            observedAt    = "${tm.substring(0,4)}-${tm.substring(4,6)}-${tm.substring(6,8)} " +
                            "${tm.substring(8,10)}:${tm.substring(10,12)}:00",
            stn           = c[IDX_STN].toInt(),
            temperature   = c[IDX_TA].toNullableDouble(),
            precipitation = c[IDX_RN].toNullableDouble(),
            windSpeed     = c[IDX_WS].toNullableDouble(),
            humidity      = c[IDX_HM].toNullableDouble()
        )
    }

    /** 결측값(-9, -9.0)은 null 처리 */
    private fun String.toNullableDouble(): Double? =
        toDoubleOrNull()?.takeIf { it > -9.0 }

    fun close() = client.close()
}
