package com.jakdang.api.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.DriverManager

class PostgresClient(
    private val url: String,
    private val user: String,
    private val password: String
) {
    init { Class.forName("org.postgresql.Driver") }

    private suspend fun query(sql: String): List<Map<String, Any?>> = withContext(Dispatchers.IO) {
        DriverManager.getConnection(url, user, password).use { conn ->
            conn.prepareStatement(sql).use { pstmt ->
                pstmt.executeQuery().use { rs ->
                    val meta = rs.metaData
                    buildList {
                        while (rs.next()) {
                            add((1..meta.columnCount).associate { meta.getColumnLabel(it) to rs.getObject(it) })
                        }
                    }
                }
            }
        }
    }

    // 대여소
    suspend fun stationSnapshot()      = query("SELECT * FROM mart_station_snapshot")
    suspend fun depletionAlert()       = query("SELECT * FROM mart_depletion_alert ORDER BY collected_at DESC LIMIT 5000")
    suspend fun congestionAlert()      = query("SELECT * FROM mart_congestion_alert ORDER BY collected_at DESC LIMIT 5000")
    suspend fun depletionWithWeather() = query("SELECT * FROM mart_depletion_with_weather ORDER BY collected_at DESC LIMIT 1000")
    suspend fun stationVersionUsage()  = query("SELECT * FROM mart_station_version_usage ORDER BY rntls_id, valid_from")

    // 날씨
    suspend fun weatherDepletion()     = query("SELECT * FROM mart_weather_depletion ORDER BY collected_at DESC LIMIT 1000")

    // 지하철
    suspend fun subwayRushDepletion(hour: Int?) = query(
        if (hour != null)
            "SELECT * FROM mart_subway_rush_depletion WHERE hour_of_day = $hour ORDER BY depletion_rate DESC"
        else
            "SELECT * FROM mart_subway_rush_depletion ORDER BY hour_of_day, depletion_rate DESC"
    )
}
