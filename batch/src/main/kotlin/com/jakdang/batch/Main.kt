package com.jakdang.batch

import com.jakdang.batch.client.AsosClient
import com.jakdang.batch.client.BikeUseDailyClient
import com.jakdang.batch.client.DdareungiClient
import com.jakdang.batch.client.StationMasterClient
import com.jakdang.batch.db.MySqlClient
import com.jakdang.batch.db.PostgresClient
import com.jakdang.batch.job.AsosBackfillJob
import com.jakdang.batch.job.BikeUseDailyBackfillJob
import com.jakdang.batch.job.DdareungiRealtimeSyncJob
import com.jakdang.batch.job.MartRealtimeSyncJob
import com.jakdang.batch.job.MartStationScdSyncJob
import com.jakdang.batch.job.MartUseHistorySyncJob
import com.jakdang.batch.job.StationMasterSyncJob
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

fun main(args: Array<String>){

    val params = args.associate {
        val (k,v) = it.removePrefix("--").split("=", limit = 2)
        k to v
    }

    val job     = params["job"]    ?: error("--job 필수")
    val runId   = params["run-id"] ?: error("--run-id 필수")

    runBlocking {
        when (job) {
            "ddareungiRealtimeSync" -> {
                val client = DdareungiClient(
                    System.getenv("DDAREUNGI_API_KEY") ?: error("DDAREUNGI_API_KEY 환경변수 필수")
                )
                val mysql = MySqlClient(
                    System.getenv("MYSQL_URL")      ?: error("MYSQL_URL 환경변수 필수"),
                    System.getenv("MYSQL_USER")     ?: error("MYSQL_USER 환경변수 필수"),
                    System.getenv("MYSQL_PASSWORD") ?: error("MYSQL_PASSWORD 환경변수 필수")
                )
                DdareungiRealtimeSyncJob(client, mysql, runId, LocalDateTime.now().toString()).execute()
                client.close()
                mysql.close()
            }
            "martRealtimeSync" -> {
                val mysql = MySqlClient(
                    System.getenv("MYSQL_URL")      ?: error("MYSQL_URL 환경변수 필수"),
                    System.getenv("MYSQL_USER")     ?: error("MYSQL_USER 환경변수 필수"),
                    System.getenv("MYSQL_PASSWORD") ?: error("MYSQL_PASSWORD 환경변수 필수")
                )
                val postgres = PostgresClient(
                    System.getenv("GRAFANA_DB_URL")      ?: error("GRAFANA_DB_URL 환경변수 필수"),
                    System.getenv("GRAFANA_DB_USER")     ?: error("GRAFANA_DB_USER 환경변수 필수"),
                    System.getenv("GRAFANA_DB_PASSWORD") ?: error("GRAFANA_DB_PASSWORD 환경변수 필수")
                )
                MartRealtimeSyncJob(mysql, postgres, runId).execute()
                mysql.close()
                postgres.close()
            }
            "martUseHistorySync" -> {
                val mysql = MySqlClient(
                    System.getenv("MYSQL_URL")      ?: error("MYSQL_URL 환경변수 필수"),
                    System.getenv("MYSQL_USER")     ?: error("MYSQL_USER 환경변수 필수"),
                    System.getenv("MYSQL_PASSWORD") ?: error("MYSQL_PASSWORD 환경변수 필수")
                )
                val postgres = PostgresClient(
                    System.getenv("GRAFANA_DB_URL")      ?: error("GRAFANA_DB_URL 환경변수 필수"),
                    System.getenv("GRAFANA_DB_USER")     ?: error("GRAFANA_DB_USER 환경변수 필수"),
                    System.getenv("GRAFANA_DB_PASSWORD") ?: error("GRAFANA_DB_PASSWORD 환경변수 필수")
                )
                // 파라미터 없으면 전월~당월 (증분)
                val ymFmt = java.time.format.DateTimeFormatter.ofPattern("yyyyMM")
                val now = java.time.LocalDate.now()
                val from = params["from"] ?: now.minusMonths(1).format(ymFmt)
                val to   = params["to"]   ?: now.format(ymFmt)
                MartUseHistorySyncJob(mysql, postgres, runId, from, to).execute()
                mysql.close()
                postgres.close()
            }
            "martStationScdSync" -> {
                val mysql = MySqlClient(
                    System.getenv("MYSQL_URL")      ?: error("MYSQL_URL 환경변수 필수"),
                    System.getenv("MYSQL_USER")     ?: error("MYSQL_USER 환경변수 필수"),
                    System.getenv("MYSQL_PASSWORD") ?: error("MYSQL_PASSWORD 환경변수 필수")
                )
                val postgres = PostgresClient(
                    System.getenv("GRAFANA_DB_URL")      ?: error("GRAFANA_DB_URL 환경변수 필수"),
                    System.getenv("GRAFANA_DB_USER")     ?: error("GRAFANA_DB_USER 환경변수 필수"),
                    System.getenv("GRAFANA_DB_PASSWORD") ?: error("GRAFANA_DB_PASSWORD 환경변수 필수")
                )
                MartStationScdSyncJob(mysql, postgres, runId).execute()
                mysql.close()
                postgres.close()
            }
            "stationMasterSync" -> {
                val client = StationMasterClient(
                    System.getenv("DDAREUNGI_API_KEY") ?: error("DDAREUNGI_API_KEY 환경변수 필수")
                )
                val mysql = MySqlClient(
                    System.getenv("MYSQL_URL")      ?: error("MYSQL_URL 환경변수 필수"),
                    System.getenv("MYSQL_USER")     ?: error("MYSQL_USER 환경변수 필수"),
                    System.getenv("MYSQL_PASSWORD") ?: error("MYSQL_PASSWORD 환경변수 필수")
                )
                StationMasterSyncJob(client, mysql, runId, LocalDateTime.now().toString()).execute()
                client.close()
                mysql.close()
            }
            "bikeUseDailyBackfill" -> {
                val client = BikeUseDailyClient(
                    System.getenv("DDAREUNGI_API_KEY") ?: error("DDAREUNGI_API_KEY 환경변수 필수")
                )
                val mysql = MySqlClient(
                    System.getenv("MYSQL_URL")      ?: error("MYSQL_URL 환경변수 필수"),
                    System.getenv("MYSQL_USER")     ?: error("MYSQL_USER 환경변수 필수"),
                    System.getenv("MYSQL_PASSWORD") ?: error("MYSQL_PASSWORD 환경변수 필수")
                )
                val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
                val collectedAt = LocalDateTime.now().toString()
                val from = params["from"]
                val to = params["to"]
                if (from != null && to != null) {
                    // 날짜 범위 백필: --from~--to (yyyyMMdd) 구간을 일별 루프
                    var d = java.time.LocalDate.parse(from, fmt)
                    val end = java.time.LocalDate.parse(to, fmt)
                    require(!d.isAfter(end)) { "--from 이 --to 보다 이후일 수 없음" }
                    while (!d.isAfter(end)) {
                        BikeUseDailyBackfillJob(client, mysql, runId, d.format(fmt), collectedAt).execute()
                        d = d.plusDays(1)
                    }
                } else {
                    val date = params["date"] ?: error("--date 또는 --from/--to 필수")
                    BikeUseDailyBackfillJob(client, mysql, runId, date, collectedAt).execute()
                }
                client.close()
                mysql.close()
            }
            "asosBackfill" -> {
                val client = AsosClient(
                    System.getenv("WEATHER_API_KEY") ?: error("WEATHER_API_KEY 환경변수 필수")
                )
                val mysql = MySqlClient(
                    System.getenv("MYSQL_URL")      ?: error("MYSQL_URL 환경변수 필수"),
                    System.getenv("MYSQL_USER")     ?: error("MYSQL_USER 환경변수 필수"),
                    System.getenv("MYSQL_PASSWORD") ?: error("MYSQL_PASSWORD 환경변수 필수")
                )
                val stn = (params["stn"] ?: "108").toInt()
                val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
                val from = params["from"]
                val to = params["to"]
                if (from != null && to != null) {
                    // 날짜 범위 백필: --from~--to (yyyyMMdd) 구간을 일별 루프
                    var d = java.time.LocalDate.parse(from, fmt)
                    val end = java.time.LocalDate.parse(to, fmt)
                    require(!d.isAfter(end)) { "--from 이 --to 보다 이후일 수 없음" }
                    while (!d.isAfter(end)) {
                        AsosBackfillJob(client, mysql, runId, d.format(fmt), stn).execute()
                        d = d.plusDays(1)
                    }
                } else {
                    // 단일 일자: --date (없으면 오늘)
                    val date = params["date"] ?: java.time.LocalDate.now().format(fmt)
                    AsosBackfillJob(client, mysql, runId, date, stn).execute()
                }
                client.close()
                mysql.close()
            }
            else -> error("알 수 없는 job: $job")
        }
    }
}
