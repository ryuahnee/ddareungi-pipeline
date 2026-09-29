package com.jakdang.batch.job

import com.jakdang.batch.db.MySqlClient
import com.jakdang.batch.db.PostgresClient
import org.slf4j.LoggerFactory

/**
 * 누락 run backfill: 원장 기준으로 run_id 단위 "해당 run 삭제 후 삽입".
 * 대상은 누적 경보 mart(mart_depletion_alert, mart_congestion_alert) 두 개.
 * (snapshot·weather mart는 매 run 전체 교체형이라 과거 run backfill 대상이 아님)
 * syncMartAlert가 run별 DELETE 후 INSERT라 재실행해도 멱등하다.
 */
class MartAlertBackfillJob(
    private val mysql: MySqlClient,
    private val postgres: PostgresClient,
    private val runIds: List<String>
) {
    private val log = LoggerFactory.getLogger(MartAlertBackfillJob::class.java)

    fun execute() {
        log.info("mart 경보 backfill 시작: 대상 run {}개", runIds.size)
        var depletionTotal = 0
        var congestionTotal = 0
        runIds.forEachIndexed { i, runId ->
            val depletion = mysql.readMartDepletionAlert(runId)
            val congestion = mysql.readMartCongestionAlert(runId)
            postgres.syncMartAlert("mart_depletion_alert", depletion)
            postgres.syncMartAlert("mart_congestion_alert", congestion)
            depletionTotal += depletion.size
            congestionTotal += congestion.size
            if ((i + 1) % 50 == 0) log.info("backfill 진행 {}/{}", i + 1, runIds.size)
        }
        log.info("mart 경보 backfill 완료: run {}개, depletion {}건, congestion {}건",
            runIds.size, depletionTotal, congestionTotal)
    }
}
