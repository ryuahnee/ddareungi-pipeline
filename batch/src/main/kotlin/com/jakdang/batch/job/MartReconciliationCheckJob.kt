package com.jakdang.batch.job

import com.jakdang.batch.db.MySqlClient
import com.jakdang.batch.db.PostgresClient
import org.slf4j.LoggerFactory

/**
 * 원천-mart 대조 체크: 해당 run의 원천 고갈 건수와 mart_depletion_alert 건수를 비교.
 * 고갈 정의(shared<10, run별 최신 collected_at)는 MySqlClient.readMartDepletionAlert 한 곳을 재사용한다.
 * 불일치 시 예외를 던져 non-zero exit → Airflow 태스크 실패로 이어진다.
 */
class MartReconciliationCheckJob(
    private val mysql: MySqlClient,
    private val postgres: PostgresClient,
    private val runId: String
) {
    private val log = LoggerFactory.getLogger(MartReconciliationCheckJob::class.java)

    fun execute() {
        log.info("원천-mart 대조 체크 시작 runId=$runId")

        val srcCount = mysql.readMartDepletionAlert(runId).size
        val martCount = postgres.countMartAlert("mart_depletion_alert", runId)

        if (srcCount != martCount) {
            log.error("대조 불일치 runId=$runId 원천={} mart={} (차이 {})", srcCount, martCount, srcCount - martCount)
            error("mart_depletion_alert 대조 불일치: 원천=$srcCount mart=$martCount runId=$runId")
        }

        log.info("원천-mart 대조 일치 runId=$runId 고갈={}", srcCount)
    }
}
