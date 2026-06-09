package com.jakdang.batch

import com.jakdang.batch.client.DdareungiClient
import com.jakdang.batch.client.WeatherClient
import com.jakdang.batch.db.DuckDbClient
import com.jakdang.batch.db.PostgresClient
import com.jakdang.batch.job.DdareungiRealtimeSyncJob
import com.jakdang.batch.job.MartBikeMovementJob
import com.jakdang.batch.job.MartDepletionWithWeatherJob
import com.jakdang.batch.job.MartHourlyWeatherBikeJob
import com.jakdang.batch.job.MartCongestionAlertJob
import com.jakdang.batch.job.MartDepletionAlertJob
import com.jakdang.batch.job.MartSnapshotJob
import com.jakdang.batch.job.MartSyncJob
import com.jakdang.batch.job.MartWeatherBikeStatsJob
import com.jakdang.batch.job.MartWeatherDepletionJob
import com.jakdang.batch.job.StagingLoadJob
import com.jakdang.batch.job.WeatherCollectJob
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

fun main(args: Array<String>){

    val params = args.associate {
        val (k,v) = it.removePrefix("--").split("=", limit = 2)
        k to v
    }

    val job     = params["job"]    ?: error("--job 필수")
    val runId   = params["run-id"] ?: error("--run-id 필수")
    val dbPath = System.getenv("DUCKDB_PATH") ?: error("DUCKDB_PATH 환경변수 필수")
    val db     = DuckDbClient(dbPath)

    runBlocking {
        when (job) {
            "ddareungiRealtimeSync" -> {
                val client = DdareungiClient(
                    System.getenv("DDAREUNGI_API_KEY") ?: error("DDAREUNGI_API_KEY 환경변수 필수")
                )
                DdareungiRealtimeSyncJob(client, db, runId, LocalDateTime.now().toString()).execute()
                client.close()
            }
            "check"              -> db.check()
            "stagingLoad"        -> StagingLoadJob(db, runId).execute()
            "martSnapshot"       -> MartSnapshotJob(db, runId).execute()
            "martDepletionAlert" -> MartDepletionAlertJob(db, runId).execute()
            "martCongestionAlert"-> MartCongestionAlertJob(db, runId).execute()
            "martSync" -> {
                val postgres = PostgresClient(
                    System.getenv("GRAFANA_DB_URL")      ?: error("GRAFANA_DB_URL 환경변수 필수"),
                    System.getenv("GRAFANA_DB_USER")     ?: error("GRAFANA_DB_USER 환경변수 필수"),
                    System.getenv("GRAFANA_DB_PASSWORD") ?: error("GRAFANA_DB_PASSWORD 환경변수 필수")
                )
                MartSyncJob(db, postgres, runId).execute()
                postgres.close()
            }
            "martHourlyWeatherBike"    -> MartHourlyWeatherBikeJob(db, runId).execute()
            "martDepletionWithWeather" -> MartDepletionWithWeatherJob(db, runId).execute()
            "martWeatherBikeStats"  -> MartWeatherBikeStatsJob(db, runId).execute()
            "martWeatherDepletion"  -> MartWeatherDepletionJob(db, runId).execute()
            "martBikeMovement"      -> MartBikeMovementJob(db, runId).execute()
            "weatherCollect" -> {
                val weatherClient = WeatherClient(
                    System.getenv("WEATHER_API_KEY") ?: error("WEATHER_API_KEY 환경변수 필수")
                )
                WeatherCollectJob(weatherClient, db, runId).execute()
                weatherClient.close()
            }
            else -> error("알 수 없는 job: $job")
        }
    }

    db.close()
}
