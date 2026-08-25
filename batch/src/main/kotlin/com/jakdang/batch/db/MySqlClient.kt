package com.jakdang.batch.db

import com.jakdang.batch.model.AsosObservation
import com.jakdang.batch.model.BikeStationRow
import com.jakdang.batch.model.BikeUseDayRow
import com.jakdang.batch.model.StationMasterRow
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Types

class MySqlClient(url: String, user: String, password: String) {
    private val log = LoggerFactory.getLogger(MySqlClient::class.java)
    private val conn: Connection = run {
        Class.forName("com.mysql.cj.jdbc.Driver")
        DriverManager.getConnection(url, user, password)
    }

    init {
        conn.createStatement().use { stmt ->
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS weather_asos (
                    id            BIGINT       NOT NULL AUTO_INCREMENT,
                    observed_at   DATETIME     NOT NULL,
                    stn           INT          NOT NULL,
                    temperature   DOUBLE       NULL,
                    precipitation DOUBLE       NULL,
                    wind_speed    DOUBLE       NULL,
                    humidity      DOUBLE       NULL,
                    collected_at  DATETIME     NOT NULL,
                    run_id        VARCHAR(255) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_obs (observed_at, stn)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """.trimIndent())
        }
        log.info("MySQL 초기화 완료")
    }

    /** ASOS 관측 적재. 멱등: 같은 (observed_at, stn) 재실행 시 UPDATE */
    fun insertWeatherHistory(rows: List<AsosObservation>, collectedAt: String, runId: String): Int {
        if (rows.isEmpty()) return 0
        val sql = """
            INSERT INTO weather_asos
                (observed_at, stn, temperature, precipitation, wind_speed, humidity, collected_at, run_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                temperature   = VALUES(temperature),
                precipitation = VALUES(precipitation),
                wind_speed    = VALUES(wind_speed),
                humidity      = VALUES(humidity),
                collected_at  = VALUES(collected_at),
                run_id        = VALUES(run_id)
        """.trimIndent()
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { r ->
                pstmt.setString(1, r.observedAt)
                pstmt.setInt(2, r.stn)
                pstmt.setObject(3, r.temperature)
                pstmt.setObject(4, r.precipitation)
                pstmt.setObject(5, r.windSpeed)
                pstmt.setObject(6, r.humidity)
                pstmt.setString(7, collectedAt)
                pstmt.setString(8, runId)
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("weather_asos 적재 완료 {}건 (run_id={})", rows.size, runId)
        return rows.size
    }

    /** 실시간 거치현황 적재. 멱등: 같은 (collected_at, station_id) 재실행 시 UPDATE */
    fun insertBikeStatus(rows: List<BikeStationRow>, collectedAt: String, runId: String): Int {
        if (rows.isEmpty()) return 0
        val sql = """
            INSERT INTO bike_status
                (station_id, station_name, rack_tot_cnt, parking_bike_tot_cnt, shared,
                 station_latitude, station_longitude, collected_at, run_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                station_name         = VALUES(station_name),
                rack_tot_cnt         = VALUES(rack_tot_cnt),
                parking_bike_tot_cnt = VALUES(parking_bike_tot_cnt),
                shared               = VALUES(shared),
                station_latitude     = VALUES(station_latitude),
                station_longitude    = VALUES(station_longitude),
                run_id               = VALUES(run_id)
        """.trimIndent()
        conn.prepareStatement(sql).use { pstmt ->
            rows.forEach { r ->
                pstmt.setString(1, r.stationId)
                pstmt.setString(2, r.stationName)
                pstmt.setIntOrNull(3, r.rackTotCnt.toIntOrNull())
                pstmt.setIntOrNull(4, r.parkingBikeTotCnt.toIntOrNull())
                pstmt.setIntOrNull(5, r.shared.toIntOrNull())
                pstmt.setDoubleOrNull(6, r.stationLatitude.toDoubleOrNull())
                pstmt.setDoubleOrNull(7, r.stationLongitude.toDoubleOrNull())
                pstmt.setString(8, collectedAt)
                pstmt.setString(9, runId)
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("bike_status 적재 완료 {}건 (run_id={})", rows.size, runId)
        return rows.size
    }

    /** 일별 이용정보 적재. 멱등: 해당 일자 DELETE 후 INSERT (차원 조합 중복행 존재 → UNIQUE 미사용) */
    fun insertBikeUseDaily(rentDt: String, rows: List<BikeUseDayRow>, collectedAt: String, runId: String): Int {
        conn.prepareStatement("DELETE FROM bike_use_daily WHERE rent_dt = ?").use {
            it.setString(1, rentDt)
            val deleted = it.executeUpdate()
            if (deleted > 0) log.info("bike_use_daily {} 기존 {}건 삭제", rentDt, deleted)
        }
        if (rows.isEmpty()) {
            log.info("bike_use_daily {} 적재 데이터 없음", rentDt)
            return 0
        }
        val sql = """
            INSERT INTO bike_use_daily
                (rent_dt, station_no, station_name, rent_type, gender_cd, age_type,
                 use_cnt, exer_amt, carbon_amt, move_meter, move_time, collected_at, run_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        // 대량(하루 5만건+)을 단일 배치로 보내면 NAS가 불안정 → 3000건 청크로 분할
        conn.prepareStatement(sql).use { pstmt ->
            rows.chunked(3000).forEach { chunk ->
                chunk.forEach { r ->
                    pstmt.setString(1, r.rentDt)
                    pstmt.setString(2, r.rentId)
                    pstmt.setString(3, r.rentNm)
                    pstmt.setString(4, r.rentType)
                    pstmt.setString(5, r.genderCd)
                    pstmt.setString(6, r.ageType)
                    pstmt.setIntOrNull(7, r.useCnt?.toIntOrNull())
                    pstmt.setDoubleOrNull(8, r.exerAmt?.toDoubleOrNull())
                    pstmt.setDoubleOrNull(9, r.carbonAmt?.toDoubleOrNull())
                    pstmt.setDoubleOrNull(10, r.moveMeter?.toDoubleOrNull())
                    pstmt.setIntOrNull(11, r.moveTime?.toIntOrNull())
                    pstmt.setString(12, collectedAt)
                    pstmt.setString(13, runId)
                    pstmt.addBatch()
                }
                pstmt.executeBatch()
                pstmt.clearBatch()
            }
        }
        log.info("bike_use_daily {} 적재 완료 {}건 (run_id={})", rentDt, rows.size, runId)
        return rows.size
    }

    /** 대여소 마스터 SCD Type 2 적재. 변경분만 새 버전 추가, 폐지 대여소는 마감 */
    fun upsertStationScd(rows: List<StationMasterRow>, now: String, runId: String): Triple<Int, Int, Int> {
        if (rows.isEmpty()) return Triple(0, 0, 0)
        val END = "9999-12-31 23:59:59"

        // 1) 현재 버전(is_current=1) 전부 로드
        data class Cur(val addr1: String?, val addr2: String?, val lat: Double?, val lot: Double?)
        val current = HashMap<String, Cur>()
        conn.createStatement().use { stmt ->
            stmt.executeQuery(
                "SELECT rntls_id, addr1, addr2, lat, lot FROM bike_station WHERE is_current = 1"
            ).use { rs ->
                while (rs.next()) {
                    current[rs.getString("rntls_id")] = Cur(
                        rs.getString("addr1"), rs.getString("addr2"),
                        rs.getObject("lat") as Double?, rs.getObject("lot") as Double?
                    )
                }
            }
        }

        val closeSql = "UPDATE bike_station SET valid_to = ?, is_current = 0 WHERE rntls_id = ? AND is_current = 1"
        val insertSql = """
            INSERT INTO bike_station
                (rntls_id, addr1, addr2, lat, lot, valid_from, valid_to, is_current, collected_at, run_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
        """.trimIndent()

        var inserted = 0; var changed = 0; var kept = 0
        val closeStmt = conn.prepareStatement(closeSql)
        val insertStmt = conn.prepareStatement(insertSql)
        val apiIds = HashSet<String>()

        rows.forEach { r ->
            apiIds.add(r.rntlsId)
            val cur = current[r.rntlsId]
            when {
                cur == null -> {  // 신규 대여소
                    insertStmt.setString(1, r.rntlsId)
                    insertStmt.setString(2, r.addr1)
                    insertStmt.setString(3, r.addr2)
                    insertStmt.setDoubleOrNull(4, r.lat)
                    insertStmt.setDoubleOrNull(5, r.lot)
                    insertStmt.setString(6, now)
                    insertStmt.setString(7, END)
                    insertStmt.setString(8, now)
                    insertStmt.setString(9, runId)
                    insertStmt.addBatch()
                    inserted++
                }
                cur.addr1 == r.addr1 && cur.addr2 == r.addr2 &&
                    cur.lat == r.lat && cur.lot == r.lot -> {  // 변경 없음
                    kept++
                }
                else -> {  // 변경 감지: 옛 행 마감 + 새 행 추가
                    closeStmt.setString(1, now)
                    closeStmt.setString(2, r.rntlsId)
                    closeStmt.addBatch()
                    insertStmt.setString(1, r.rntlsId)
                    insertStmt.setString(2, r.addr1)
                    insertStmt.setString(3, r.addr2)
                    insertStmt.setDoubleOrNull(4, r.lat)
                    insertStmt.setDoubleOrNull(5, r.lot)
                    insertStmt.setString(6, now)
                    insertStmt.setString(7, END)
                    insertStmt.setString(8, now)
                    insertStmt.setString(9, runId)
                    insertStmt.addBatch()
                    changed++
                }
            }
        }

        // 폐지 대여소: DB엔 current인데 API 응답에 없음 → 마감
        var retired = 0
        current.keys.forEach { id ->
            if (id !in apiIds) {
                closeStmt.setString(1, now)
                closeStmt.setString(2, id)
                closeStmt.addBatch()
                retired++
            }
        }

        closeStmt.use { it.executeBatch() }
        insertStmt.use { it.executeBatch() }
        log.info("bike_station SCD 적재: 신규 {} 변경 {} 유지 {} 폐지 {} (run_id={})",
            inserted, changed, kept, retired, runId)
        return Triple(inserted, changed, retired)
    }

    private fun java.sql.PreparedStatement.setIntOrNull(idx: Int, v: Int?) =
        if (v == null) setNull(idx, Types.INTEGER) else setInt(idx, v)

    private fun java.sql.PreparedStatement.setDoubleOrNull(idx: Int, v: Double?) =
        if (v == null) setNull(idx, Types.DOUBLE) else setDouble(idx, v)

    // ---------- mart 집계 (원장 → Postgres mart용 Map 리스트) ----------

    /** run_id 내 최신 스냅샷 시각 (재시도 등으로 collected_at 여러 개일 때 중복 방지) */
    private fun latestCollectedAt(runId: String) =
        "collected_at = (SELECT MAX(collected_at) FROM bike_status WHERE run_id = ?)"

    /** 최신 스냅샷: 특정 run_id의 전 대여소 거치 현황 */
    fun readMartSnapshot(runId: String): List<Map<String, Any?>> = readTable(
        """
        SELECT station_id, station_name, rack_tot_cnt, parking_bike_tot_cnt,
               shared, station_latitude, station_longitude, collected_at, run_id
        FROM bike_status
        WHERE run_id = ? AND ${latestCollectedAt(runId)}
        """.trimIndent(), runId, runId
    )

    /** 고갈 경보: 거치율 10% 미만 */
    fun readMartDepletionAlert(runId: String): List<Map<String, Any?>> = readTable(
        """
        SELECT station_id, station_name, parking_bike_tot_cnt, shared, collected_at, run_id
        FROM bike_status
        WHERE run_id = ? AND shared < 10 AND ${latestCollectedAt(runId)}
        """.trimIndent(), runId, runId
    )

    /** 혼잡 경보: 거치율 90% 초과 */
    fun readMartCongestionAlert(runId: String): List<Map<String, Any?>> = readTable(
        """
        SELECT station_id, station_name, parking_bike_tot_cnt, shared, collected_at, run_id
        FROM bike_status
        WHERE run_id = ? AND shared > 90 AND ${latestCollectedAt(runId)}
        """.trimIndent(), runId, runId
    )

    /** SCD 버전별 평균 거치율 (변경 이력 있는 대여소만, 매일 1회 실행 전제) */
    fun readMartStationVersionUsage(): List<Map<String, Any?>> = readTable(
        """
        SELECT s.rntls_id, s.addr1, s.valid_from, s.valid_to,
               CAST(s.is_current AS SIGNED) AS is_current,
               ROUND(AVG(b.shared), 1) AS avg_shared, COUNT(*) AS snapshot_cnt
        FROM bike_station s
        JOIN bike_status b ON b.station_id = s.rntls_id
          AND b.collected_at >= s.valid_from AND b.collected_at < s.valid_to
        WHERE s.rntls_id IN (SELECT rntls_id FROM bike_station GROUP BY rntls_id HAVING COUNT(*) > 1)
        GROUP BY s.rntls_id, s.addr1, s.valid_from, s.valid_to, s.is_current
        ORDER BY s.rntls_id, s.valid_from
        """.trimIndent()
    )

    /** 월별 이용 집계: 일×권종×성별×연령 (rent_dt 클러스터 PK + 월별 파티션으로 순차 스캔) */
    fun readUseDailyAgg(ym: String): List<Map<String, Any?>> {
        val start = "${ym.substring(0, 4)}-${ym.substring(4, 6)}-01"
        return readTable(
            """
            SELECT rent_dt, rent_type, gender_cd, age_type,
                   SUM(use_cnt)    AS use_cnt,
                   SUM(move_meter) AS move_meter,
                   SUM(move_time)  AS move_time,
                   SUM(exer_amt)   AS exer_amt,
                   SUM(carbon_amt) AS carbon_amt
            FROM bike_use_daily
            WHERE rent_dt >= ? AND rent_dt < DATE_ADD(?, INTERVAL 1 MONTH)
            GROUP BY rent_dt, rent_type, gender_cd, age_type
            """.trimIndent(), start, start
        )
    }

    /**
     * 월별 이용 집계: rent_ym×권종×성별×연령.
     * bike_use_monthly 전체 + bike_use_daily에서 monthly에 없는 월(예: 최신월)만 월롤업하여 연속 확보.
     */
    fun readUseMonthlyAgg(): List<Map<String, Any?>> = readTable(
        """
        SELECT rent_ym, rent_type, gender_cd, age_type,
               SUM(use_cnt)    AS use_cnt,
               SUM(move_meter) AS move_meter,
               SUM(move_time)  AS move_time,
               SUM(exer_amt)   AS exer_amt,
               SUM(carbon_amt) AS carbon_amt
        FROM bike_use_monthly
        GROUP BY rent_ym, rent_type, gender_cd, age_type
        UNION ALL
        SELECT DATE_FORMAT(rent_dt, '%Y%m'), rent_type, gender_cd, age_type,
               SUM(use_cnt), SUM(move_meter), SUM(move_time), SUM(exer_amt), SUM(carbon_amt)
        FROM bike_use_daily
        WHERE DATE_FORMAT(rent_dt, '%Y%m') NOT IN (SELECT DISTINCT rent_ym FROM bike_use_monthly)
        GROUP BY DATE_FORMAT(rent_dt, '%Y%m'), rent_type, gender_cd, age_type
        """.trimIndent()
    )

    /** 일별 날씨 요약 (전체, 소량) */
    fun readWeatherDaily(): List<Map<String, Any?>> = readTable(
        """
        SELECT DATE(observed_at) AS obs_date,
               ROUND(AVG(temperature), 1) AS avg_temp,
               MIN(temperature)           AS min_temp,
               MAX(temperature)           AS max_temp,
               ROUND(SUM(COALESCE(precipitation, 0)), 1) AS total_precip,
               ROUND(AVG(wind_speed), 1)  AS avg_wind,
               ROUND(AVG(humidity), 1)    AS avg_humidity
        FROM weather_asos
        GROUP BY DATE(observed_at)
        """.trimIndent()
    )

    /** 공휴일 전체 (dim). locdate yyyyMMdd → DATE 변환 */
    fun readHoliday(): List<Map<String, Any?>> = readTable(
        "SELECT STR_TO_DATE(locdate, '%Y%m%d') AS locdate, date_name, is_holiday FROM holiday"
    )

    private fun readTable(sql: String, vararg params: Any?): List<Map<String, Any?>> {
        val rows = mutableListOf<Map<String, Any?>>()
        conn.prepareStatement(sql).use { pstmt ->
            params.forEachIndexed { i, p -> pstmt.setObject(i + 1, p) }
            pstmt.executeQuery().use { rs ->
                val meta = rs.metaData
                while (rs.next()) {
                    rows.add((1..meta.columnCount).associate { meta.getColumnLabel(it) to rs.getObject(it) })
                }
            }
        }
        return rows
    }

    fun close() = conn.close()
}
