package com.jakdang.batch.db

import com.jakdang.batch.model.BikeStationRow
import com.jakdang.batch.model.WeatherSnapshot
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager

class DuckDbClient(dbPath: String) {
    private val log = LoggerFactory.getLogger(DuckDbClient::class.java)
    private val conn: Connection = DriverManager.getConnection("jdbc:duckdb:$dbPath")

    init {
        conn.createStatement().use { stmt ->
            stmt.execute("CREATE SCHEMA IF NOT EXISTS raw")
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS raw.bike_status (
                    station_id            VARCHAR,
                    station_name          VARCHAR,
                    rack_tot_cnt          VARCHAR,
                    parking_bike_tot_cnt  VARCHAR,
                    shared                VARCHAR,
                    station_latitude      VARCHAR,
                    station_longitude     VARCHAR,
                    collected_at          TIMESTAMP,
                    run_id                VARCHAR
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS raw.weather_snapshot (
                    base_date     VARCHAR,
                    base_time     VARCHAR,
                    nx            INTEGER,
                    ny            INTEGER,
                    temperature   DOUBLE,
                    precipitation DOUBLE,
                    precip_type   INTEGER,
                    wind_speed    DOUBLE,
                    humidity      INTEGER,
                    observed_at   TIMESTAMP,
                    collected_at  TIMESTAMP,
                    run_id        VARCHAR
                )
            """.trimIndent())
            stmt.execute("CREATE SCHEMA IF NOT EXISTS staging")
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS staging.bike_status (
                    station_id            VARCHAR,
                    station_name          VARCHAR,
                    rack_tot_cnt          INTEGER,
                    parking_bike_tot_cnt  INTEGER,
                    shared                INTEGER,
                    station_latitude      DOUBLE,
                    station_longitude     DOUBLE,
                    collected_at          TIMESTAMP,
                    run_id                VARCHAR
                )
            """.trimIndent())
            stmt.execute("CREATE SCHEMA IF NOT EXISTS mart")
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart.station_snapshot (
                    station_id            VARCHAR,
                    station_name          VARCHAR,
                    rack_tot_cnt          INTEGER,
                    parking_bike_tot_cnt  INTEGER,
                    shared                INTEGER,
                    station_latitude      DOUBLE,
                    station_longitude     DOUBLE,
                    collected_at          TIMESTAMP,
                    run_id                VARCHAR
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart.depletion_alert (
                    station_id            VARCHAR,
                    station_name          VARCHAR,
                    parking_bike_tot_cnt  INTEGER,
                    shared                INTEGER,
                    collected_at          TIMESTAMP,
                    run_id                VARCHAR
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart.congestion_alert (
                    station_id            VARCHAR,
                    station_name          VARCHAR,
                    parking_bike_tot_cnt  INTEGER,
                    shared                INTEGER,
                    collected_at          TIMESTAMP,
                    run_id                VARCHAR
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart.weather_bike_stats (
                    precip_type   INTEGER,
                    precip_label  VARCHAR,
                    temp_group    DOUBLE,
                    avg_shared    DOUBLE,
                    sample_count  BIGINT,
                    last_updated  TIMESTAMP
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart.weather_depletion (
                    precip_type      INTEGER,
                    precip_label     VARCHAR,
                    temperature      DOUBLE,
                    depletion_count  BIGINT,
                    run_id           VARCHAR,
                    collected_at     TIMESTAMP
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart.bike_movement (
                    station_id    VARCHAR,
                    station_name  VARCHAR,
                    collected_at  TIMESTAMP,
                    movement      BIGINT,
                    precip_type   INTEGER,
                    temperature   DOUBLE,
                    run_id        VARCHAR
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart.depletion_with_weather (
                    station_id            VARCHAR,
                    station_name          VARCHAR,
                    parking_bike_tot_cnt  INTEGER,
                    shared                INTEGER,
                    station_latitude      DOUBLE,
                    station_longitude     DOUBLE,
                    precip_type           INTEGER,
                    precip_label          VARCHAR,
                    temperature           DOUBLE,
                    wind_speed            DOUBLE,
                    humidity              INTEGER,
                    collected_at          TIMESTAMP,
                    run_id                VARCHAR
                )
            """.trimIndent())
        }
        log.info("DuckDB 초기화 완료: $dbPath")
    }

    fun insertBikeStatus(rows: List<BikeStationRow>, collectedAt: String, runId: String) {
        val sql = "INSERT INTO raw.bike_status VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setString(1, row.stationId)
                pstmt.setString(2, row.stationName)
                pstmt.setString(3, row.rackTotCnt)
                pstmt.setString(4, row.parkingBikeTotCnt)
                pstmt.setString(5, row.shared)
                pstmt.setString(6, row.stationLatitude)
                pstmt.setString(7, row.stationLongitude)
                pstmt.setString(8, collectedAt)
                pstmt.setString(9, runId)
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("raw.bike_status 저장 완료 {}건", rows.size)
    }

    fun insertWeather(snapshot: WeatherSnapshot, observedAt: String, collectedAt: String, runId: String) {
        val sql = "INSERT INTO raw.weather_snapshot VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, snapshot.baseDate)
            pstmt.setString(2, snapshot.baseTime)
            pstmt.setInt(3, snapshot.nx)
            pstmt.setInt(4, snapshot.ny)
            pstmt.setDouble(5, snapshot.temperature)
            pstmt.setDouble(6, snapshot.precipitation)
            pstmt.setInt(7, snapshot.precipType)
            pstmt.setDouble(8, snapshot.windSpeed)
            pstmt.setInt(9, snapshot.humidity)
            pstmt.setString(10, observedAt)
            pstmt.setString(11, collectedAt)
            pstmt.setString(12, runId)
            pstmt.executeUpdate()
        }
        log.info("raw.weather_snapshot 저장 완료 (기온={}°C, 강수형태={}, 습도={}%)",
            snapshot.temperature, snapshot.precipType, snapshot.humidity)
    }

    fun loadStaging(runId: String): Int {
        val sql = """
            INSERT INTO staging.bike_status
            SELECT station_id, station_name,
                   TRY_CAST(rack_tot_cnt AS INTEGER),
                   TRY_CAST(parking_bike_tot_cnt AS INTEGER),
                   TRY_CAST(shared AS INTEGER),
                   TRY_CAST(station_latitude AS DOUBLE),
                   TRY_CAST(station_longitude AS DOUBLE),
                   collected_at,
                   run_id
            FROM (
                SELECT *, ROW_NUMBER() OVER (PARTITION BY run_id, station_id ORDER BY collected_at DESC) AS rn
                FROM raw.bike_status
                WHERE run_id = ?
            ) t
            WHERE rn = 1
        """.trimIndent()

        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, runId)
            pstmt.executeUpdate()
        }

        var count = 0
        conn.prepareStatement("SELECT COUNT(*) FROM staging.bike_status WHERE run_id = ?").use { pstmt ->
            pstmt.setString(1, runId)
            val rs = pstmt.executeQuery()
            if (rs.next()) count = rs.getInt(1)
        }
        log.info("staging.bike_status 적재 완료 {}건 (run_id={})", count, runId)
        return count
    }

    fun loadMartSnapshot(runId: String): Int {
        conn.createStatement().use { stmt ->
            stmt.execute("DELETE FROM mart.station_snapshot")
            stmt.execute("""
                INSERT INTO mart.station_snapshot
                SELECT station_id, station_name, rack_tot_cnt, parking_bike_tot_cnt,
                       shared, station_latitude, station_longitude, collected_at, run_id
                FROM staging.bike_status
                WHERE run_id = '$runId'
            """.trimIndent())
        }
        val count = conn.createStatement().executeQuery("SELECT COUNT(*) FROM mart.station_snapshot")
            .also { it.next() }.getInt(1)
        log.info("mart.station_snapshot 적재 완료 {}건 (run_id={})", count, runId)
        return count
    }

    fun loadMartDepletionAlert(runId: String): Int {
        conn.createStatement().use { stmt ->
            stmt.execute("DELETE FROM mart.depletion_alert WHERE run_id = '$runId'")
            stmt.execute("""
                INSERT INTO mart.depletion_alert
                SELECT station_id, station_name, parking_bike_tot_cnt, shared, collected_at, run_id
                FROM staging.bike_status
                WHERE run_id = '$runId' AND shared < 10
            """.trimIndent())
        }
        val count = conn.createStatement()
            .executeQuery("SELECT COUNT(*) FROM mart.depletion_alert WHERE run_id = '$runId'")
            .also { it.next() }.getInt(1)
        log.info("mart.depletion_alert 적재 완료 {}건 (run_id={})", count, runId)
        return count
    }

    fun loadMartCongestionAlert(runId: String): Int {
        conn.createStatement().use { stmt ->
            stmt.execute("DELETE FROM mart.congestion_alert WHERE run_id = '$runId'")
            stmt.execute("""
                INSERT INTO mart.congestion_alert
                SELECT station_id, station_name, parking_bike_tot_cnt, shared, collected_at, run_id
                FROM staging.bike_status
                WHERE run_id = '$runId' AND shared > 90
            """.trimIndent())
        }
        val count = conn.createStatement()
            .executeQuery("SELECT COUNT(*) FROM mart.congestion_alert WHERE run_id = '$runId'")
            .also { it.next() }.getInt(1)
        log.info("mart.congestion_alert 적재 완료 {}건 (run_id={})", count, runId)
        return count
    }

    fun loadMartWeatherBikeStats(): Int {
        conn.createStatement().use { stmt ->
            stmt.execute("DELETE FROM mart.weather_bike_stats")
            stmt.execute("""
                INSERT INTO mart.weather_bike_stats
                SELECT
                    w.precip_type,
                    CASE w.precip_type
                        WHEN 0 THEN '맑음' WHEN 1 THEN '비'
                        WHEN 2 THEN '비/눈' WHEN 3 THEN '눈'
                        ELSE '기타'
                    END AS precip_label,
                    ROUND(w.temperature / 5.0) * 5 AS temp_group,
                    ROUND(AVG(b.shared), 1) AS avg_shared,
                    COUNT(*) AS sample_count,
                    MAX(b.collected_at) AS last_updated
                FROM staging.bike_status b
                JOIN raw.weather_snapshot w
                    ON DATE_TRUNC('hour', b.collected_at) = DATE_TRUNC('hour', w.observed_at)
                GROUP BY w.precip_type, precip_label, temp_group
            """.trimIndent())
        }
        val count = conn.createStatement().executeQuery("SELECT COUNT(*) FROM mart.weather_bike_stats")
            .also { it.next() }.getInt(1)
        log.info("mart.weather_bike_stats 적재 완료 {}건", count)
        return count
    }

    fun loadMartWeatherDepletion(runId: String): Int {
        conn.createStatement().use { stmt ->
            stmt.execute("DELETE FROM mart.weather_depletion WHERE run_id = '$runId'")
            stmt.execute("""
                INSERT INTO mart.weather_depletion
                SELECT
                    w.precip_type,
                    CASE w.precip_type
                        WHEN 0 THEN '맑음' WHEN 1 THEN '비'
                        WHEN 2 THEN '비/눈' WHEN 3 THEN '눈'
                        ELSE '기타'
                    END AS precip_label,
                    w.temperature,
                    COUNT(DISTINCT d.station_id) AS depletion_count,
                    d.run_id,
                    MAX(d.collected_at) AS collected_at
                FROM mart.depletion_alert d
                JOIN raw.weather_snapshot w
                    ON DATE_TRUNC('hour', d.collected_at) = DATE_TRUNC('hour', w.observed_at)
                WHERE d.run_id = '$runId'
                GROUP BY w.precip_type, precip_label, w.temperature, d.run_id
            """.trimIndent())
        }
        val count = conn.createStatement()
            .executeQuery("SELECT COUNT(*) FROM mart.weather_depletion WHERE run_id = '$runId'")
            .also { it.next() }.getInt(1)
        log.info("mart.weather_depletion 적재 완료 {}건 (run_id={})", count, runId)
        return count
    }

    fun loadMartBikeMovement(runId: String): Int {
        conn.createStatement().use { stmt ->
            stmt.execute("DELETE FROM mart.bike_movement WHERE run_id = '$runId'")
            stmt.execute("""
                INSERT INTO mart.bike_movement
                WITH ranked AS (
                    SELECT
                        station_id, station_name, parking_bike_tot_cnt, collected_at, run_id,
                        LAG(parking_bike_tot_cnt) OVER (PARTITION BY station_id ORDER BY collected_at) AS prev_cnt
                    FROM staging.bike_status
                )
                SELECT
                    r.station_id, r.station_name, r.collected_at,
                    ABS(r.parking_bike_tot_cnt - r.prev_cnt) AS movement,
                    w.precip_type, w.temperature, r.run_id
                FROM ranked r
                JOIN raw.weather_snapshot w
                    ON DATE_TRUNC('hour', r.collected_at) = DATE_TRUNC('hour', w.observed_at)
                WHERE r.prev_cnt IS NOT NULL AND r.run_id = '$runId'
            """.trimIndent())
        }
        val count = conn.createStatement()
            .executeQuery("SELECT COUNT(*) FROM mart.bike_movement WHERE run_id = '$runId'")
            .also { it.next() }.getInt(1)
        log.info("mart.bike_movement 적재 완료 {}건 (run_id={})", count, runId)
        return count
    }

    fun loadMartDepletionWithWeather(runId: String): Int {
        conn.createStatement().use { stmt ->
            stmt.execute("DELETE FROM mart.depletion_with_weather WHERE run_id = '$runId'")
            stmt.execute("""
                INSERT INTO mart.depletion_with_weather
                SELECT
                    d.station_id, d.station_name,
                    d.parking_bike_tot_cnt, d.shared,
                    s.station_latitude, s.station_longitude,
                    w.precip_type,
                    CASE w.precip_type
                        WHEN 0 THEN '맑음' WHEN 1 THEN '비'
                        WHEN 2 THEN '비/눈' WHEN 3 THEN '눈'
                        ELSE '기타'
                    END AS precip_label,
                    w.temperature, w.wind_speed, w.humidity,
                    d.collected_at, d.run_id
                FROM mart.depletion_alert d
                JOIN mart.station_snapshot s ON d.station_id = s.station_id
                JOIN raw.weather_snapshot w
                    ON DATE_TRUNC('hour', d.collected_at) = DATE_TRUNC('hour', w.observed_at)
                WHERE d.run_id = '$runId'
            """.trimIndent())
        }
        val count = conn.createStatement()
            .executeQuery("SELECT COUNT(*) FROM mart.depletion_with_weather WHERE run_id = '$runId'")
            .also { it.next() }.getInt(1)
        log.info("mart.depletion_with_weather 적재 완료 {}건 (run_id={})", count, runId)
        return count
    }

    fun readMartDepletionWithWeather(): List<Map<String, Any?>> = readTable("SELECT * FROM mart.depletion_with_weather")

    fun readMartWeatherBikeStats(): List<Map<String, Any?>> = readTable("SELECT * FROM mart.weather_bike_stats")
    fun readMartWeatherDepletion(): List<Map<String, Any?>> = readTable("SELECT * FROM mart.weather_depletion")
    fun readMartBikeMovement(): List<Map<String, Any?>> = readTable("SELECT * FROM mart.bike_movement")

    fun readMartSnapshot(): List<Map<String, Any?>> = readTable("SELECT * FROM mart.station_snapshot")

    fun readMartDepletionAlert(runId: String): List<Map<String, Any?>> =
        readTable("SELECT * FROM mart.depletion_alert WHERE run_id = '$runId'")

    fun readMartCongestionAlert(runId: String): List<Map<String, Any?>> =
        readTable("SELECT * FROM mart.congestion_alert WHERE run_id = '$runId'")

    private fun readTable(sql: String): List<Map<String, Any?>> {
        val rows = mutableListOf<Map<String, Any?>>()
        conn.createStatement().executeQuery(sql).use { rs ->
            val meta = rs.metaData
            while (rs.next()) {
                rows.add((1..meta.columnCount).associate { meta.getColumnName(it) to rs.getObject(it) })
            }
        }
        return rows
    }

    fun check() {
        conn.createStatement().use { stmt ->
            val rs = stmt.executeQuery("SELECT COUNT(*), MIN(collected_at), MAX(collected_at) FROM raw.bike_status")
            if (rs.next()) {
                log.info("총 건수: {}건 | 최초 수집: {} | 최근 수집: {}", rs.getInt(1), rs.getString(2), rs.getString(3))
            }
            val rs2 = stmt.executeQuery("""
                SELECT station_name, parking_bike_tot_cnt, shared, collected_at
                FROM raw.bike_status
                ORDER BY collected_at DESC
                LIMIT 3
            """.trimIndent())
            log.info("--- 최근 샘플 3건 ---")
            while (rs2.next()) {
                log.info("대여소: {} | 자전거: {}대 | 거치율: {}% | 수집시각: {}",
                    rs2.getString(1), rs2.getString(2), rs2.getString(3), rs2.getString(4))
            }
        }
    }

    fun close() = conn.close()
}
