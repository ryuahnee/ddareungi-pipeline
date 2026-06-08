package com.jakdang.batch.job

import com.jakdang.batch.db.DuckDbClient
import org.slf4j.LoggerFactory

class StagingLoadJob(
    private val db: DuckDbClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(StagingLoadJob::class.java)

    fun execute() {
        log.info("staging 적재 시작 runId=$runId")
        val count = db.loadStaging(runId)
        log.info("staging 적재 완료 {}건", count)
    }
}

