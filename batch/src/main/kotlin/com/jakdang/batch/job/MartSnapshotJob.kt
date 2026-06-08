package com.jakdang.batch.job

import com.jakdang.batch.db.DuckDbClient
import org.slf4j.LoggerFactory

class MartSnapshotJob(
    private val db: DuckDbClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartSnapshotJob::class.java)

    fun execute() {
        log.info("mart snapshot 적재 시작 runId=$runId")
        db.loadMartSnapshot(runId)
    }
}
