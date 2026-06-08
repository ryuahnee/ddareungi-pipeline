package com.jakdang.batch

import com.jakdang.batch.client.DdareungiClient
import com.jakdang.batch.db.DuckDbClient
import com.jakdang.batch.db.PostgresClient
import com.jakdang.batch.job.DdareungiRealtimeSyncJob
import com.jakdang.batch.job.MartCongestionAlertJob
import com.jakdang.batch.job.MartDepletionAlertJob
import com.jakdang.batch.job.MartSnapshotJob
import com.jakdang.batch.job.MartSyncJob
import com.jakdang.batch.job.StagingLoadJob
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

fun main(args: Array<String>){

    val params = args.associate {
        val (k,v) = it.removePrefix("--").split("=", limit = 2)
        k to v
    }

    val job     = params["job"]    ?: error("--job 필수")
    val runId   = params["run-id"] ?: error("--run-id 필수")
    val apiKey  = System.getenv("DDAREUNGI_API_KEY") ?: error("DDAREUNGI_API_KEY 환경변수 필수")

    val client = DdareungiClient(apiKey)
    val collectedAt = LocalDateTime.now().toString()

    val dbPath    = System.getenv("DUCKDB_PATH")       ?: error("DUCKDB_PATH 환경변수 필수")
    val pgUrl     = System.getenv("GRAFANA_DB_URL")     ?: error("GRAFANA_DB_URL 환경변수 필수")
    val pgUser    = System.getenv("GRAFANA_DB_USER")    ?: error("GRAFANA_DB_USER 환경변수 필수")
    val pgPass    = System.getenv("GRAFANA_DB_PASSWORD") ?: error("GRAFANA_DB_PASSWORD 환경변수 필수")

    val db       = DuckDbClient(dbPath)
    val postgres = PostgresClient(pgUrl, pgUser, pgPass)

    runBlocking {
        when (job) {
            "ddareungiRealtimeSync" -> DdareungiRealtimeSyncJob(client, db, runId, collectedAt).execute()
            "check"              -> db.check()
            "stagingLoad"        -> StagingLoadJob(db, runId).execute()
            "martSnapshot"       -> MartSnapshotJob(db, runId).execute()
            "martDepletionAlert" -> MartDepletionAlertJob(db, runId).execute()
            "martCongestionAlert"-> MartCongestionAlertJob(db, runId).execute()
            "martSync"           -> MartSyncJob(db, postgres, runId).execute()
            else -> error("알 수 없는 job: $job")
        }
    }

    db.close()
    postgres.close()
    client.close()
}
