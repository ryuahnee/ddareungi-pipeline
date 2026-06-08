package com.jakdang.batch.job

import com.jakdang.batch.db.DuckDbClient
import org.slf4j.LoggerFactory

class MartDepletionAlertJob(
    private val db: DuckDbClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartDepletionAlertJob::class.java)

    fun execute() {
        log.info("mart depletion alert 적재 시작 runId=$runId")
        db.loadMartDepletionAlert(runId)
    }
}
