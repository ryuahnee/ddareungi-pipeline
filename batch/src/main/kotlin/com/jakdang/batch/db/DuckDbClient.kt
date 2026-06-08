package com.jakdang.batch.db

import com.jakdang.batch.model.BikeStationRow
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
