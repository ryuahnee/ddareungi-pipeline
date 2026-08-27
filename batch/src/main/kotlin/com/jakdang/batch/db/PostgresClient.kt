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
                CREATE TABLE IF NOT EXISTS mart_weather_bike_stats (
                    precip_type   INTEGER,
                    precip_label  VARCHAR,
                    temp_group    DOUBLE PRECISION,
                    avg_shared    DOUBLE PRECISION,
                    sample_count  BIGINT,
                    last_updated  TIMESTAMP
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
                CREATE TABLE IF NOT EXISTS mart_bike_movement (
                    station_id    VARCHAR,
                    station_name  VARCHAR,
                    collected_at  TIMESTAMP,
                    movement      BIGINT,
                    precip_type   INTEGER,
                    temperature   DOUBLE PRECISION,
                    run_id        VARCHAR
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart_hot_sunny_station_stats (
                    station_id        VARCHAR,
                    station_name      VARCHAR,
                    avg_shared        DOUBLE PRECISION,
                    sample_count      BIGINT,
                    station_latitude  DOUBLE PRECISION,
                    station_longitude DOUBLE PRECISION
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart_hourly_weather_bike (
                    collected_at    TIMESTAMP,
                    precip_type     INTEGER,
                    precip_label    VARCHAR,
                    temperature     DOUBLE PRECISION,
                    avg_shared      DOUBLE PRECISION,
                    total_stations  INTEGER,
                    sample_count    BIGINT
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart_station_holiday_depletion (
                    station_id        VARCHAR,
                    station_name      VARCHAR,
                    station_latitude  DOUBLE PRECISION,
                    station_longitude DOUBLE PRECISION,
                    day_type          VARCHAR,
                    depletion_rate    DOUBLE PRECISION,
                    sample_hours      BIGINT
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart_holiday_bike_stats (
                    date            DATE,
                    hour_of_day     INTEGER,
                    day_type        VARCHAR,
                    holiday_name    VARCHAR,
                    depletion_rate  DOUBLE PRECISION,
                    sample_count    BIGINT
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
                CREATE TABLE IF NOT EXISTS mart_morning_rush (
                    station_id        VARCHAR,
                    station_name      VARCHAR,
                    station_latitude  DOUBLE PRECISION,
                    station_longitude DOUBLE PRECISION,
                    shared_07         DOUBLE PRECISION,
                    shared_09         DOUBLE PRECISION,
                    drop_amount       DOUBLE PRECISION,
                    run_id            VARCHAR,
                    computed_at       TIMESTAMP
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart_evening_rush (
                    station_id        VARCHAR,
                    station_name      VARCHAR,
                    station_latitude  DOUBLE PRECISION,
                    station_longitude DOUBLE PRECISION,
                    shared_18         DOUBLE PRECISION,
                    shared_20         DOUBLE PRECISION,
                    rise_amount       DOUBLE PRECISION,
                    run_id            VARCHAR,
                    computed_at       TIMESTAMP
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart_weather_threshold (
                    factor        VARCHAR,
                    bucket_label  VARCHAR,
                    bucket_order  INTEGER,
                    avg_metric    DOUBLE PRECISION,
                    sample_count  BIGINT,
                    run_id        VARCHAR,
                    computed_at   TIMESTAMP
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart_station_cluster (
                    station_id        VARCHAR,
                    station_name      VARCHAR,
                    station_latitude  DOUBLE PRECISION,
                    station_longitude DOUBLE PRECISION,
                    day_type          VARCHAR,
                    cluster_id        INTEGER,
                    cluster_label     VARCHAR,
                    avg_shared        DOUBLE PRECISION,
                    run_id            VARCHAR,
                    computed_at       TIMESTAMP,
                    input_from        TIMESTAMP,
                    input_to          TIMESTAMP
                )
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mart_cluster_profile (
                    cluster_id    INTEGER,
                    day_type      VARCHAR,
                    hour_of_day   INTEGER,
                    avg_shared    DOUBLE PRECISION,
                    station_count INTEGER,
                    run_id        VARCHAR
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
        try {
            conn.createStatement().use {
                it.execute("ALTER TABLE mart_holiday_bike_stats RENAME COLUMN avg_shared TO depletion_rate")
            }
        } catch (_: Exception) {}
        try {
            conn.createStatement().use {
                it.execute("ALTER TABLE mart_station_cluster ADD COLUMN IF NOT EXISTS avg_shared DOUBLE PRECISION")
            }
        } catch (_: Exception) {}
        try {
            conn.createStatement().use {
                it.execute("ALTER TABLE mart_station_cluster ADD COLUMN IF NOT EXISTS day_type VARCHAR")
            }
        } catch (_: Exception) {}
        log.info("PostgreSQL 초기화 완료")
    }

    /** 일×권종×성별×연령 이용 집계. 멱등: 해당 월 DELETE 후 INSERT */
    fun syncUseDailyAgg(ym: String, rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS mart_use_daily_agg (
                rent_dt     DATE,
                rent_type   VARCHAR,
                gender_cd   VARCHAR,
                age_type    VARCHAR,
                use_cnt     BIGINT,
                move_meter  DOUBLE PRECISION,
                move_time   BIGINT,
                exer_amt    DOUBLE PRECISION,
                carbon_amt  DOUBLE PRECISION
            )
        """.trimIndent())
        val start = "${ym.substring(0, 4)}-${ym.substring(4, 6)}-01"
        conn.prepareStatement(
            "DELETE FROM mart_use_daily_agg WHERE rent_dt >= ?::date AND rent_dt < ?::date + INTERVAL '1 month'"
        ).use { it.setString(1, start); it.setString(2, start); it.executeUpdate() }
        if (rows.isEmpty()) {
            log.info("mart_use_daily_agg {}: 데이터 없음", ym)
            return
        }
        val sql = "INSERT INTO mart_use_daily_agg VALUES (?,?,?,?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setObject(1, row["rent_dt"])
                pstmt.setString(2, row["rent_type"] as String?)
                pstmt.setString(3, row["gender_cd"] as String?)
                pstmt.setString(4, row["age_type"] as String?)
                pstmt.setObject(5, (row["use_cnt"] as Number?)?.toLong())
                pstmt.setObject(6, (row["move_meter"] as Number?)?.toDouble())
                pstmt.setObject(7, (row["move_time"] as Number?)?.toLong())
                pstmt.setObject(8, (row["exer_amt"] as Number?)?.toDouble())
                pstmt.setObject(9, (row["carbon_amt"] as Number?)?.toDouble())
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_use_daily_agg {} 동기화 완료 {}건", ym, rows.size)
    }

    /** 월별 이용 집계. 전체 교체(소량). rent_ym(CHAR6) → stat_month DATE 변환해 시계열 지원 */
    fun syncUseMonthlyAgg(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS mart_use_monthly_agg (
                stat_month  DATE,
                rent_type   VARCHAR,
                gender_cd   VARCHAR,
                age_type    VARCHAR,
                use_cnt     BIGINT,
                move_meter  DOUBLE PRECISION,
                move_time   BIGINT,
                exer_amt    DOUBLE PRECISION,
                carbon_amt  DOUBLE PRECISION
            )
        """.trimIndent())
        if (rows.isEmpty()) {
            log.warn("mart_use_monthly_agg: 입력 0건 → 기존 데이터 보존")
            return
        }
        conn.createStatement().execute("DELETE FROM mart_use_monthly_agg")
        val sql = "INSERT INTO mart_use_monthly_agg VALUES (to_date(?, 'YYYYMM'),?,?,?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setString(1, row["rent_ym"] as String?)
                pstmt.setString(2, row["rent_type"] as String?)
                pstmt.setString(3, row["gender_cd"] as String?)
                pstmt.setString(4, row["age_type"] as String?)
                pstmt.setObject(5, (row["use_cnt"] as Number?)?.toLong())
                pstmt.setObject(6, (row["move_meter"] as Number?)?.toDouble())
                pstmt.setObject(7, (row["move_time"] as Number?)?.toLong())
                pstmt.setObject(8, (row["exer_amt"] as Number?)?.toDouble())
                pstmt.setObject(9, (row["carbon_amt"] as Number?)?.toDouble())
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_use_monthly_agg 동기화 완료 {}건", rows.size)
    }

    /** 일별 날씨 요약. 전체 교체(소량) */
    fun syncWeatherDaily(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS mart_weather_daily (
                obs_date     DATE PRIMARY KEY,
                avg_temp     DOUBLE PRECISION,
                min_temp     DOUBLE PRECISION,
                max_temp     DOUBLE PRECISION,
                total_precip DOUBLE PRECISION,
                avg_wind     DOUBLE PRECISION,
                avg_humidity DOUBLE PRECISION
            )
        """.trimIndent())
        if (rows.isEmpty()) {
            log.warn("mart_weather_daily: 입력 0건 → 기존 데이터 보존")
            return
        }
        conn.createStatement().execute("DELETE FROM mart_weather_daily")
        val sql = "INSERT INTO mart_weather_daily VALUES (?,?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setObject(1, row["obs_date"])
                pstmt.setObject(2, (row["avg_temp"] as Number?)?.toDouble())
                pstmt.setObject(3, (row["min_temp"] as Number?)?.toDouble())
                pstmt.setObject(4, (row["max_temp"] as Number?)?.toDouble())
                pstmt.setObject(5, (row["total_precip"] as Number?)?.toDouble())
                pstmt.setObject(6, (row["avg_wind"] as Number?)?.toDouble())
                pstmt.setObject(7, (row["avg_humidity"] as Number?)?.toDouble())
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_weather_daily 동기화 완료 {}건", rows.size)
    }

    /** 공휴일 dim. 전체 교체(61건) */
    fun syncDimHoliday(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS dim_holiday (
                locdate    DATE,
                date_name  VARCHAR,
                is_holiday VARCHAR
            )
        """.trimIndent())
        if (rows.isEmpty()) {
            log.warn("dim_holiday: 입력 0건 → 기존 데이터 보존")
            return
        }
        conn.createStatement().execute("DELETE FROM dim_holiday")
        val sql = "INSERT INTO dim_holiday VALUES (?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setObject(1, row["locdate"])
                pstmt.setString(2, row["date_name"] as String?)
                pstmt.setString(3, row["is_holiday"] as String?)
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("dim_holiday 동기화 완료 {}건", rows.size)
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

    fun syncWeatherBikeStats(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM mart_weather_bike_stats")
        if (rows.isEmpty()) return
        val sql = "INSERT INTO mart_weather_bike_stats VALUES (?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setObject(1, row["precip_type"])
                pstmt.setString(2, row["precip_label"] as String?)
                pstmt.setObject(3, row["temp_group"])
                pstmt.setObject(4, row["avg_shared"])
                pstmt.setObject(5, row["sample_count"])
                pstmt.setObject(6, row["last_updated"])
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_weather_bike_stats 동기화 완료 {}건", rows.size)
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

    fun syncBikeMovement(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM mart_bike_movement")
        if (rows.isEmpty()) return
        val sql = "INSERT INTO mart_bike_movement VALUES (?,?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setString(1, row["station_id"] as String?)
                pstmt.setString(2, row["station_name"] as String?)
                pstmt.setObject(3, row["collected_at"])
                pstmt.setObject(4, row["movement"])
                pstmt.setObject(5, row["precip_type"])
                pstmt.setObject(6, row["temperature"])
                pstmt.setString(7, row["run_id"] as String?)
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_bike_movement 동기화 완료 {}건", rows.size)
    }

    fun syncHotSunnyStationStats(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM mart_hot_sunny_station_stats")
        if (rows.isEmpty()) return
        val sql = "INSERT INTO mart_hot_sunny_station_stats VALUES (?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setString(1, row["station_id"] as String?)
                pstmt.setString(2, row["station_name"] as String?)
                pstmt.setObject(3, row["avg_shared"])
                pstmt.setObject(4, row["sample_count"])
                pstmt.setObject(5, row["station_latitude"])
                pstmt.setObject(6, row["station_longitude"])
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_hot_sunny_station_stats 동기화 완료 {}건", rows.size)
    }

    fun syncHourlyWeatherBike(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM mart_hourly_weather_bike")
        if (rows.isEmpty()) return
        val sql = "INSERT INTO mart_hourly_weather_bike VALUES (?,?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setObject(1, row["collected_at"])
                pstmt.setObject(2, row["precip_type"])
                pstmt.setString(3, row["precip_label"] as String?)
                pstmt.setObject(4, row["temperature"])
                pstmt.setObject(5, row["avg_shared"])
                pstmt.setObject(6, row["total_stations"])
                pstmt.setObject(7, row["sample_count"])
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_hourly_weather_bike 동기화 완료 {}건", rows.size)
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

    fun syncStationHolidayDepletion(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM mart_station_holiday_depletion")
        if (rows.isEmpty()) return
        val sql = "INSERT INTO mart_station_holiday_depletion VALUES (?,?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setString(1, row["station_id"] as String?)
                pstmt.setString(2, row["station_name"] as String?)
                pstmt.setObject(3, row["station_latitude"])
                pstmt.setObject(4, row["station_longitude"])
                pstmt.setString(5, row["day_type"] as String?)
                pstmt.setObject(6, row["depletion_rate"])
                pstmt.setObject(7, row["sample_hours"])
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_station_holiday_depletion 동기화 완료 {}건", rows.size)
    }

    fun syncHolidayBikeStats(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM mart_holiday_bike_stats")
        if (rows.isEmpty()) return
        val sql = "INSERT INTO mart_holiday_bike_stats VALUES (?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setObject(1, row["date"])
                pstmt.setObject(2, row["hour_of_day"])
                pstmt.setString(3, row["day_type"] as String?)
                pstmt.setString(4, row["holiday_name"] as String?)
                pstmt.setObject(5, row["depletion_rate"])
                pstmt.setObject(6, row["sample_count"])
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_holiday_bike_stats 동기화 완료 {}건", rows.size)
    }

    fun syncStationCluster(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM mart_station_cluster")
        if (rows.isEmpty()) return
        val sql = """
            INSERT INTO mart_station_cluster
                (station_id, station_name, station_latitude, station_longitude,
                 day_type, cluster_id, cluster_label, avg_shared, run_id, computed_at,
                 input_from, input_to)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
        """.trimIndent()
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setString(1, row["station_id"] as String?)
                pstmt.setString(2, row["station_name"] as String?)
                pstmt.setObject(3, row["station_latitude"])
                pstmt.setObject(4, row["station_longitude"])
                pstmt.setString(5, row["day_type"] as String?)
                pstmt.setObject(6, row["cluster_id"])
                pstmt.setString(7, row["cluster_label"] as String?)
                pstmt.setObject(8, row["avg_shared"])
                pstmt.setString(9, row["run_id"] as String?)
                pstmt.setObject(10, row["computed_at"])
                pstmt.setObject(11, row["input_from"])
                pstmt.setObject(12, row["input_to"])
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_station_cluster 동기화 완료 {}건", rows.size)
    }

    fun syncClusterProfile(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM mart_cluster_profile")
        if (rows.isEmpty()) return
        val sql = "INSERT INTO mart_cluster_profile VALUES (?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setObject(1, row["cluster_id"])
                pstmt.setString(2, row["day_type"] as String?)
                pstmt.setObject(3, row["hour_of_day"])
                pstmt.setObject(4, row["avg_shared"])
                pstmt.setObject(5, row["station_count"])
                pstmt.setString(6, row["run_id"] as String?)
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_cluster_profile 동기화 완료 {}건", rows.size)
    }

    fun syncEveningRush(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM mart_evening_rush")
        if (rows.isEmpty()) return
        val sql = "INSERT INTO mart_evening_rush VALUES (?,?,?,?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setString(1, row["station_id"] as String?)
                pstmt.setString(2, row["station_name"] as String?)
                pstmt.setObject(3, row["station_latitude"])
                pstmt.setObject(4, row["station_longitude"])
                pstmt.setObject(5, row["shared_18"])
                pstmt.setObject(6, row["shared_20"])
                pstmt.setObject(7, row["rise_amount"])
                pstmt.setString(8, row["run_id"] as String?)
                pstmt.setObject(9, row["computed_at"])
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_evening_rush 동기화 완료 {}건", rows.size)
    }

    fun syncMorningRush(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM mart_morning_rush")
        if (rows.isEmpty()) return
        val sql = "INSERT INTO mart_morning_rush VALUES (?,?,?,?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setString(1, row["station_id"] as String?)
                pstmt.setString(2, row["station_name"] as String?)
                pstmt.setObject(3, row["station_latitude"])
                pstmt.setObject(4, row["station_longitude"])
                pstmt.setObject(5, row["shared_07"])
                pstmt.setObject(6, row["shared_09"])
                pstmt.setObject(7, row["drop_amount"])
                pstmt.setString(8, row["run_id"] as String?)
                pstmt.setObject(9, row["computed_at"])
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_morning_rush 동기화 완료 {}건", rows.size)
    }

    fun syncWeatherThreshold(rows: List<Map<String, Any?>>) {
        conn.createStatement().execute("DELETE FROM mart_weather_threshold")
        if (rows.isEmpty()) return
        val sql = "INSERT INTO mart_weather_threshold VALUES (?,?,?,?,?,?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { row ->
                pstmt.setString(1, row["factor"] as String?)
                pstmt.setString(2, row["bucket_label"] as String?)
                pstmt.setObject(3, row["bucket_order"])
                pstmt.setObject(4, row["avg_metric"])
                pstmt.setObject(5, row["sample_count"])
                pstmt.setString(6, row["run_id"] as String?)
                pstmt.setObject(7, row["computed_at"])
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("mart_weather_threshold 동기화 완료 {}건", rows.size)
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
