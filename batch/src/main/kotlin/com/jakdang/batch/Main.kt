package com.jakdang.batch

import com.jakdang.batch.client.DdareungiClient
import com.jakdang.batch.db.DuckDbClient
import com.jakdang.batch.job.DdareungiRealtimeSyncJob
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

fun main(args: Array<String>){

    val params = args.associate {
        val (k,v) = it.removePrefix("--").split("=", limit = 2)
        k to v
    }

    val job     = params["job"]    ?: error("--job 필수")
    val date    = params["date"]   ?: error("--date 필수")
    val runId   = params["run-id"] ?: error("--run-id 필수")
    val apiKey  = System.getenv("DDAREUNGI_API_KEY") ?: error("DDAREUNGI_API_KEY 환경변수 필수")

    val client = DdareungiClient(apiKey)
    val collectedAt = LocalDateTime.now().toString()

    val dbPath = System.getenv("DUCKDB_PATH") ?: error("DUCKDB_PATH 환경변수 필수")
    val db = DuckDbClient(dbPath)

    runBlocking {
        when (job) {
            "ddareungiRealtimeSync" -> DdareungiRealtimeSyncJob(client, db, runId, collectedAt).execute()
            "check"   -> db.check()
            else -> error("알 수 없는 job: $job")
        }
    }

    db.close()
    client.close()
}
