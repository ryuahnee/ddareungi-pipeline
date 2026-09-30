package com.jakdang.batch.job

import com.jakdang.batch.client.HolidayClient
import com.jakdang.batch.db.MySqlClient
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * 공휴일 수집: 한국천문연구원 특일 정보 API로 해당 연·월 공휴일을 MySQL holiday에 적재.
 * 멱등: 같은 (locdate, seq) 재수집 시 UPDATE.
 */
class HolidayCollectJob(
    private val client: HolidayClient,
    private val mysql: MySqlClient,
    private val runId: String,
    private val year: String,
    private val month: String
) {
    private val log = LoggerFactory.getLogger(HolidayCollectJob::class.java)

    suspend fun execute() {
        log.info("공휴일 수집 시작 {}-{} runId={}", year, month, runId)
        val items = client.fetch(year, month)
        mysql.insertHoliday(items, LocalDateTime.now().toString(), runId)
        log.info("공휴일 수집 완료 {}-{} {}건", year, month, items.size)
    }
}
