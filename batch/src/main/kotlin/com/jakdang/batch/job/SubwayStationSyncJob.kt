package com.jakdang.batch.job

import com.jakdang.batch.client.SubwayClient
import com.jakdang.batch.db.MySqlClient
import org.slf4j.LoggerFactory
import kotlin.math.*

class SubwayStationSyncJob(
    private val client: SubwayClient,
    private val mysql: MySqlClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(SubwayStationSyncJob::class.java)

    suspend fun execute() {
        log.info("지하철역 동기화 시작 runId=$runId")

        val stations = client.fetchAll()
        log.info("지하철역 수집 완료 {}건 (1~9호선)", stations.size)
        mysql.upsertSubwayStations(stations)

        val bikeStations = mysql.readDistinctBikeStations()
        log.info("따릉이 대여소 {}개 로드", bikeStations.size)

        val mappings = mutableListOf<Triple<String, String, Double>>()
        for ((stationId, lat, lng) in bikeStations) {
            for (s in stations) {
                val dist = haversineM(lat, lng, s.convY.toDouble(), s.convX.toDouble())
                if (dist <= 500.0) mappings.add(Triple(stationId, s.outStnNum, dist))
            }
        }
        mysql.upsertBikeStationSubwayMap(mappings)
        log.info("지하철역 동기화 완료: 대여소-역 매핑 {}건", mappings.size)
    }

    private fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6_371_000.0
        val phi1 = Math.toRadians(lat1); val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lon2 - lon1)
        val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
