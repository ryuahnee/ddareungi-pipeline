package com.jakdang.batch.job

import com.jakdang.batch.db.DuckDbClient
import org.slf4j.LoggerFactory

class MartBikeMovementJob(
    private val db: DuckDbClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartBikeMovementJob::class.java)

    fun execute() {
        log.info("mart bike_movement 적재 시작 runId=$runId")
        db.loadMartBikeMovement(runId)
    }
}
