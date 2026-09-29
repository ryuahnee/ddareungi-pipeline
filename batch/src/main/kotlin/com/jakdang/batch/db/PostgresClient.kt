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
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart_weather_depletion (
                    precip_type      INTEGER,
                    precip_label     VARCHAR,
                    temperature      DOUBLE PRECISION,
                    depletion_count  BIGINT,
                    run_id           VARCHAR,
                    collected_at     TIMESTAMP
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart_depletion_with_weather (
                    station_id            VARCHAR,
                    station_name          VARCHAR,
                    parking_bike_tot_cnt  INTEGER,
                    shared                INTEGER,
                    station_latitude      DOUBLE PRECISION,
                    station_longitude     DOUBLE PRECISION,
                    precip_type           INTEGER,
                    precip_label          VARCHAR,
                    temperature           DOUBLE PRECISION,
                    wind_speed            DOUBLE PRECISION,
                    humidity              INTEGER,
                    collected_at          TIMESTAMP,
                    run_id                VARCHAR
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart_subway_rush_depletion (
                    out_stn_num          VARCHAR,
                    stn_kr_nm            VARCHAR,
                    line_nm              VARCHAR,
                    subway_lat           DOUBLE PRECISION,
                    subway_lng           DOUBLE PRECISION,
                    hour_of_day          INTEGER,
                    depletion_rate       DOUBLE PRECISION,
                    nearby_station_count INTEGER,
                    sample_count         BIGINT
                )
            """.trimIndent())
        }
        log.info("PostgreSQL 초기화 완료")
    }

    /** SCD 버전별 평균 거치율 mart. 매일 전체 재계산(소량)이라 DELETE 후 INSERT */
    fun syncStationVersionUsage(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS mart_station_version_usage (
                rntls_id      VARCHAR,
                addr1         VARCHAR,
                valid_from    TIMESTAMP,
                valid_to      TIMESTAMP,
                is_current    INTEGER,
                avg_shared    DOUBLE PRECISION,
                snapshot_cnt  INTEGER,
                computed_at   TIMESTAMP DEFAULT NOW()
            )
        """.trimIndent())
        if (rows.isEmpty()) {
            log.warn("mart_station_version_usage: 입력 0건 → 기존 데이터 보존(DELETE 스킵)")
            return
        }
        conn.createStatement().execute("DELETE FROM mart_station_version_usage")
        val sql = "INSERT INTO mart_station_version_usage (rntls_id, addr1, valid_from, valid_to, is_current, avg_shared, snapshot_cnt) VALUES (?,?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setString(1, row["rntls_id"] as String?)
                pstmt.setString(2, row["addr1"] as String?)
                pstmt.setObject(3, row["valid_from"])
                pstmt.setObject(4, row["valid_to"])
                pstmt.setObject(5, row["is_current"])
                pstmt.setObject(6, row["avg_shared"])
                pstmt.setObject(7, row["snapshot_cnt"])
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_station_version_usage 동기화 완료 {}건", rows.size)
    }

    /**
     * 부족 전환 mart 동기화. 날짜별 DELETE 후 INSERT (멱등).
     * 지표 정의는 MySqlClient.readDepletionTransition 한 곳에 있고, 아래 테이블/컬럼 주석에 명시한다.
     */
    fun syncDepletionTransition(date: String, rows: List<Map<String, Any?>>) {
        conn.createStatement().use { stmt ->
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart_depletion_transition (
                    transition_date  DATE,
                    station_id       VARCHAR,
                    station_name     VARCHAR,
                    rate_0700        DOUBLE PRECISION,
                    rate_0900        DOUBLE PRECISION,
                    snapshots_0700   INTEGER,
                    snapshots_0900   INTEGER,
                    PRIMARY KEY (transition_date, station_id)
                )
            """.trimIndent())
            stmt.execute("COMMENT ON TABLE mart_depletion_transition IS " +
                "'부족 전환 대여소-일: 평일 07시대 평균 거치율>=10 & 09시대 평균 거치율<10 (거치율=shared, 두 시간대 모두 관측된 건만)'")
            stmt.execute("COMMENT ON COLUMN mart_depletion_transition.rate_0700 IS '07시대 스냅샷 평균 거치율(%), >=10'")
            stmt.execute("COMMENT ON COLUMN mart_depletion_transition.rate_0900 IS '09시대 스냅샷 평균 거치율(%), <10'")
        }
        conn.prepareStatement("DELETE FROM mart_depletion_transition WHERE transition_date = ?::date").use {
            it.setString(1, date); it.executeUpdate()
        }
        if (rows.isEmpty()) {
            log.info("mart_depletion_transition {} 부족 전환 0건", date)
            return
        }
        val sql = "INSERT INTO mart_depletion_transition VALUES (?::date,?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setString(1, date)
                pstmt.setString(2, row["station_id"] as String?)
                pstmt.setString(3, row["station_name"] as String?)
                pstmt.setObject(4, row["rate_0700"])
                pstmt.setObject(5, row["rate_0900"])
                pstmt.setObject(6, row["snapshots_0700"])
                pstmt.setObject(7, row["snapshots_0900"])
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_depletion_transition {} 동기화 완료 {}건", date, rows.size)
    }

    fun syncMartSnapshot(rows: List<Map<String, Any?>>) {
        if (rows.isEmpty()) {
            log.warn("mart_station_snapshot: 입력 0건 → 기존 데이터 보존(DELETE 스킵)")
            return
        }
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

    /** 특정 run_id의 경보 mart 적재 건수 (원천-mart 대조용) */
    fun countMartAlert(table: String, runId: String): Int {
        conn.prepareStatement("SELECT COUNT(*) FROM $table WHERE run_id = ?").use { pstmt ->
            pstmt.setString(1, runId)
            pstmt.executeQuery().use { rs -> rs.next(); return rs.getInt(1) }
        }
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

    fun syncWeatherDepletion(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM mart_weather_depletion")
        if (rows.isEmpty()) return
        val sql = "INSERT INTO mart_weather_depletion VALUES (?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setObject(1, row["precip_type"])
                pstmt.setString(2, row["precip_label"] as String?)
                pstmt.setObject(3, row["temperature"])
                pstmt.setObject(4, row["depletion_count"])
                pstmt.setString(5, row["run_id"] as String?)
                pstmt.setObject(6, row["collected_at"])
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_weather_depletion 동기화 완료 {}건", rows.size)
    }

    fun syncDepletionWithWeather(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM mart_depletion_with_weather")
        if (rows.isEmpty()) return
        val sql = "INSERT INTO mart_depletion_with_weather VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setString(1, row["station_id"] as String?)
                pstmt.setString(2, row["station_name"] as String?)
                pstmt.setObject(3, row["parking_bike_tot_cnt"])
                pstmt.setObject(4, row["shared"])
                pstmt.setObject(5, row["station_latitude"])
                pstmt.setObject(6, row["station_longitude"])
                pstmt.setObject(7, row["precip_type"])
                pstmt.setString(8, row["precip_label"] as String?)
                pstmt.setObject(9, row["temperature"])
                pstmt.setObject(10, row["wind_speed"])
                pstmt.setObject(11, row["humidity"])
                pstmt.setObject(12, row["collected_at"])
                pstmt.setString(13, row["run_id"] as String?)
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_depletion_with_weather 동기화 완료 {}건", rows.size)
    }

    fun syncSubwayRushDepletion(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM mart_subway_rush_depletion")
        if (rows.isEmpty()) { log.warn("mart_subway_rush_depletion: 0건"); return }
        val sql = "INSERT INTO mart_subway_rush_depletion VALUES (?,?,?,?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setString(1, row["out_stn_num"] as String?)
                pstmt.setString(2, row["stn_kr_nm"] as String?)
                pstmt.setString(3, row["line_nm"] as String?)
                pstmt.setObject(4, (row["subway_lat"] as Number?)?.toDouble())
                pstmt.setObject(5, (row["subway_lng"] as Number?)?.toDouble())
                pstmt.setObject(6, (row["hour_of_day"] as Number?)?.toInt())
                pstmt.setObject(7, (row["depletion_rate"] as Number?)?.toDouble())
                pstmt.setObject(8, (row["nearby_station_count"] as Number?)?.toInt())
                pstmt.setObject(9, (row["sample_count"] as Number?)?.toLong())
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_subway_rush_depletion 동기화 완료 {}건", rows.size)
    }

    fun close() = conn.close()
}
