package com.jakdang.batch.job

import com.jakdang.batch.db.DuckDbClient
import org.slf4j.LoggerFactory

class MartCongestionAlertJob(
    private val db: DuckDbClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartCongestionAlertJob::class.java)

    fun execute() {
        log.info("mart congestion alert 적재 시작 runId=$runId")
        db.loadMartCongestionAlert(runId)
    }
}
