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

    suspend fun stationSnapshot()      = query("SELECT * FROM mart_station_snapshot")
    suspend fun depletionAlert()       = query("SELECT * FROM mart_depletion_alert")
    suspend fun congestionAlert()      = query("SELECT * FROM mart_congestion_alert")
    suspend fun depletionWithWeather() = query("SELECT * FROM mart_depletion_with_weather")
    suspend fun holidayDepletion()     = query("SELECT * FROM mart_station_holiday_depletion")
    suspend fun morningRush()          = query("SELECT * FROM mart_morning_rush")
    suspend fun eveningRush()          = query("SELECT * FROM mart_evening_rush")
    suspend fun stationCluster()       = query("SELECT * FROM mart_station_cluster")
    suspend fun weatherBikeStats()     = query("SELECT * FROM mart_weather_bike_stats")
    suspend fun weatherDepletion()     = query("SELECT * FROM mart_weather_depletion")
    suspend fun hourlyWeatherBike()    = query("SELECT * FROM mart_hourly_weather_bike")
    suspend fun holidayBikeStats()     = query("SELECT * FROM mart_holiday_bike_stats")
    suspend fun bikeMovement()         = query("SELECT * FROM mart_bike_movement")
    suspend fun weatherThreshold()     = query("SELECT * FROM mart_weather_threshold")
    suspend fun clusterProfile()       = query("SELECT * FROM mart_cluster_profile")
    suspend fun useDailyAgg()          = query("SELECT * FROM mart_use_daily_agg")
    suspend fun useMonthlyAgg()        = query("SELECT * FROM mart_use_monthly_agg")
}
