package com.jakdang.batch.db

import com.jakdang.batch.model.AsosObservation
import com.jakdang.batch.model.BikeStationRow
import com.jakdang.batch.model.BikeUseDayRow
import com.jakdang.batch.model.StationMasterRow
import com.jakdang.batch.model.SubwayStation
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
                CREATE TABLE IF NOT EXISTS subway_station (
                    out_stn_num VARCHAR(10)  NOT NULL,
                    stn_kr_nm   VARCHAR(50)  NOT NULL,
                    line_nm     VARCHAR(50)  NOT NULL,
                    conv_x      DOUBLE       NOT NULL,
                    conv_y      DOUBLE       NOT NULL,
                    PRIMARY KEY (out_stn_num)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """.trimIndent())
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bike_station_subway_map (
                    station_id  VARCHAR(20) NOT NULL,
                    out_stn_num VARCHAR(10) NOT NULL,
                    distance_m  DOUBLE      NOT NULL,
                    PRIMARY KEY (station_id, out_stn_num)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """.trimIndent())
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

    /**
     * 부족 전환 지표 (지표 정의 단일 출처).
     * 평일(월~금) 하루에 대해, 07시대 스냅샷 평균 거치율(shared)>=10 이고 09시대 평균<10 인 대여소.
     * 두 시간대 모두 관측된 대여소만(평균이 NULL이면 비교 결과 false → 자연 제외).
     * date: yyyy-MM-dd. 주말이면 DAYOFWEEK 조건으로 빈 결과.
     */
    fun readDepletionTransition(date: String): List<Map<String, Any?>> = readTable(
        """
        SELECT station_id, station_name,
               ROUND(AVG(CASE WHEN HOUR(collected_at) = 7 THEN shared END), 2) AS rate_0700,
               ROUND(AVG(CASE WHEN HOUR(collected_at) = 9 THEN shared END), 2) AS rate_0900,
               SUM(HOUR(collected_at) = 7) AS snapshots_0700,
               SUM(HOUR(collected_at) = 9) AS snapshots_0900
        FROM bike_status
        WHERE DATE(collected_at) = ?
          AND HOUR(collected_at) IN (7, 9)
          AND DAYOFWEEK(collected_at) BETWEEN 2 AND 6
        GROUP BY station_id, station_name
        HAVING AVG(CASE WHEN HOUR(collected_at) = 7 THEN shared END) >= 10
           AND AVG(CASE WHEN HOUR(collected_at) = 9 THEN shared END) < 10
        """.trimIndent(), date
    )

    /** 기간 내 원장에 존재하는 run_id 목록 (collected_at 기준, mart 백필 대상 산정용) */
    fun readRunIdsInRange(fromDate: String, toDate: String): List<String> = readTable(
        """
        SELECT DISTINCT run_id
        FROM bike_status
        WHERE collected_at >= ? AND collected_at < DATE_ADD(?, INTERVAL 1 DAY)
        ORDER BY run_id
        """.trimIndent(), fromDate, toDate
    ).map { it["run_id"] as String }

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

    /** 지하철역 전체 교체 적재 */
    fun upsertSubwayStations(stations: List<SubwayStation>): Int {
        conn.createStatement().execute("DELETE FROM subway_station")
        if (stations.isEmpty()) return 0
        conn.prepareStatement("INSERT INTO subway_station VALUES (?,?,?,?,?)").use { pstmt ->
            stations.forEach { s ->
                pstmt.setString(1, s.outStnNum)
                pstmt.setString(2, s.stnKrNm)
                pstmt.setString(3, s.lineNm)
                pstmt.setDouble(4, s.convX.toDouble())
                pstmt.setDouble(5, s.convY.toDouble())
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("subway_station 저장 완료 {}건", stations.size)
        return stations.size
    }

    /** 대여소-지하철역 매핑 전체 교체 적재 */
    fun upsertBikeStationSubwayMap(mappings: List<Triple<String, String, Double>>): Int {
        conn.createStatement().execute("DELETE FROM bike_station_subway_map")
        if (mappings.isEmpty()) return 0
        conn.prepareStatement("INSERT INTO bike_station_subway_map VALUES (?,?,?)").use { pstmt ->
            mappings.forEach { (stationId, outStnNum, distM) ->
                pstmt.setString(1, stationId)
                pstmt.setString(2, outStnNum)
                pstmt.setDouble(3, distM)
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
        log.info("bike_station_subway_map 저장 완료 {}건", mappings.size)
        return mappings.size
    }

    /** 따릉이 대여소별 현재 좌표 (bike_station SCD에서 로드, 빠름) */
    fun readDistinctBikeStations(): List<Triple<String, Double, Double>> =
        readTable(
            "SELECT rntls_id AS station_id, lat, lot AS lng FROM bike_station WHERE is_current = 1 AND lat IS NOT NULL AND lot IS NOT NULL"
        ).map {
            Triple(
                it["station_id"] as String,
                (it["lat"] as Number).toDouble(),
                (it["lng"] as Number).toDouble()
            )
        }

    /** 지하철역별 × 시간대별 반경 500m 내 따릉이 고갈율 (전체 재계산) */
    fun readSubwayRushDepletion(): List<Map<String, Any?>> = readTable(
        """
        SELECT s.out_stn_num, s.stn_kr_nm, s.line_nm,
               s.conv_y AS subway_lat, s.conv_x AS subway_lng,
               HOUR(bs.collected_at) AS hour_of_day,
               ROUND(
                   COUNT(DISTINCT CASE WHEN bs.shared < 10 THEN bs.station_id END) * 100.0
                   / NULLIF(COUNT(DISTINCT bs.station_id), 0), 1
               ) AS depletion_rate,
               COUNT(DISTINCT bs.station_id) AS nearby_station_count,
               COUNT(*) AS sample_count
        FROM subway_station s
        JOIN bike_station_subway_map m ON m.out_stn_num = s.out_stn_num
        JOIN bike_status bs ON bs.station_id = m.station_id
        WHERE bs.collected_at < DATE_ADD(DATE(NOW()), INTERVAL HOUR(NOW()) HOUR)
        GROUP BY s.out_stn_num, s.stn_kr_nm, s.line_nm, s.conv_y, s.conv_x, HOUR(bs.collected_at)
        """.trimIndent()
    )

    /** run_id 기준 최신 스냅샷의 날씨별 고갈 카운트 */
    fun readWeatherDepletion(runId: String): List<Map<String, Any?>> = readTable(
        """
        SELECT CASE WHEN COALESCE(wa.precipitation, 0) > 0 THEN 1 ELSE 0 END AS precip_type,
               CASE WHEN COALESCE(wa.precipitation, 0) > 0 THEN '비' ELSE '맑음' END AS precip_label,
               wa.temperature,
               COUNT(DISTINCT CASE WHEN bs.shared < 10 THEN bs.station_id END) AS depletion_count,
               ? AS run_id,
               MAX(bs.collected_at) AS collected_at
        FROM bike_status bs
        LEFT JOIN weather_asos wa
            ON DATE(bs.collected_at) = DATE(wa.observed_at)
            AND HOUR(bs.collected_at) = HOUR(wa.observed_at)
            AND wa.stn = 108
        WHERE bs.run_id = ?
          AND bs.collected_at = (SELECT MAX(collected_at) FROM bike_status WHERE run_id = ?)
        GROUP BY precip_type, precip_label, wa.temperature
        """.trimIndent(),
        runId, runId, runId
    )

    /** run_id 기준 고갈 대여소 + 날씨 JOIN */
    fun readDepletionWithWeather(runId: String): List<Map<String, Any?>> = readTable(
        """
        SELECT bs.station_id, bs.station_name,
               bs.parking_bike_tot_cnt, bs.shared,
               bs.station_latitude, bs.station_longitude,
               CASE WHEN COALESCE(wa.precipitation, 0) > 0 THEN 1 ELSE 0 END AS precip_type,
               CASE WHEN COALESCE(wa.precipitation, 0) > 0 THEN '비' ELSE '맑음' END AS precip_label,
               wa.temperature, wa.wind_speed,
               CAST(wa.humidity AS SIGNED) AS humidity,
               bs.collected_at, bs.run_id
        FROM bike_status bs
        LEFT JOIN weather_asos wa
            ON DATE(bs.collected_at) = DATE(wa.observed_at)
            AND HOUR(bs.collected_at) = HOUR(wa.observed_at)
            AND wa.stn = 108
        WHERE bs.run_id = ?
          AND bs.shared < 10
          AND bs.collected_at = (SELECT MAX(collected_at) FROM bike_status WHERE run_id = ?)
        """.trimIndent(),
        runId, runId
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
