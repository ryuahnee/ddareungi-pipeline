package com.jakdang.batch.db

import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager

class PostgresClient(url: String, user: String, password: String) {
    private val log = LoggerFactory.getLogger(PostgresClient::class.java)
    private val conn: Connection = run {
        Class.forName("org.postgresql.Driver")
        DriverManager.getConnection(url, user, password)
    }

    init {
        conn.createStatement().use { stmt ->
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart_station_snapshot (
                    station_id            VARCHAR,
                    station_name          VARCHAR,
                    rack_tot_cnt          INTEGER,
                    parking_bike_tot_cnt  INTEGER,
                    shared                INTEGER,
                    station_latitude      DOUBLE PRECISION,
                    station_longitude     DOUBLE PRECISION,
                    collected_at          TIMESTAMP,
                    run_id                VARCHAR
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart_depletion_alert (
                    station_id            VARCHAR,
                    station_name          VARCHAR,
                    parking_bike_tot_cnt  INTEGER,
                    shared                INTEGER,
                    collected_at          TIMESTAMP,
                    run_id                VARCHAR
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart_congestion_alert (
                    station_id            VARCHAR,
                    station_name          VARCHAR,
                    parking_bike_tot_cnt  INTEGER,
                    shared                INTEGER,
                    collected_at          TIMESTAMP,
                    run_id                VARCHAR
                )
            """.trimIndent())
        }
        log.info("PostgreSQL 초기화 완료")
    }

    fun syncMartSnapshot(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM mart_station_snapshot")
        val sql = "INSERT INTO mart_station_snapshot VALUES (?,?,?,?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setString(1, row["station_id"] as String?)
                pstmt.setString(2, row["station_name"] as String?)
                pstmt.setObject(3, row["rack_tot_cnt"])
                pstmt.setObject(4, row["parking_bike_tot_cnt"])
                pstmt.setObject(5, row["shared"])
                pstmt.setObject(6, row["station_latitude"])
                pstmt.setObject(7, row["station_longitude"])
                pstmt.setObject(8, row["collected_at"])
                pstmt.setString(9, row["run_id"] as String?)
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_station_snapshot 동기화 완료 {}건", rows.size)
    }

    fun syncMartAlert(table: String, rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM $table WHERE run_id = '${rows.firstOrNull()?.get("run_id")}'")
        val sql = "INSERT INTO $table VALUES (?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setString(1, row["station_id"] as String?)
                pstmt.setString(2, row["station_name"] as String?)
                pstmt.setObject(3, row["parking_bike_tot_cnt"])
                pstmt.setObject(4, row["shared"])
                pstmt.setObject(5, row["collected_at"])
                pstmt.setString(6, row["run_id"] as String?)
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("{} 동기화 완료 {}건", table, rows.size)
    }

    fun close() = conn.close()
}
