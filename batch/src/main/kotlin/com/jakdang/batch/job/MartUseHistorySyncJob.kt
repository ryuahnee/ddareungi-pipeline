package com.jakdang.batch.job

import com.jakdang.batch.db.MySqlClient
import com.jakdang.batch.db.PostgresClient
import org.slf4j.LoggerFactory

/**
 * 과거 이용정보 mart 동기화 (MySQL 원장 → Postgres).
 * - mart_use_daily_agg: 일×권종×성별×연령 (ym 범위를 월별 청크로)
 * - mart_weather_daily: 일별 날씨 요약 (전체 교체)
 * - dim_holiday: 공휴일 (전체 교체)
 */
class MartUseHistorySyncJob(
    private val mysql: MySqlClient,
    private val postgres: PostgresClient,
    private val runId: String,
    private val fromYm: String,
    private val toYm: String
) {
    private val log = LoggerFactory.getLogger(MartUseHistorySyncJob::class.java)

    fun execute() {
        log.info("과거 이용 mart 동기화 시작 [{} ~ {}] runId={}", fromYm, toYm, runId)

        var ym = fromYm
        while (ym <= toYm) {
            postgres.syncUseDailyAgg(ym, mysql.readUseDailyAgg(ym))
            ym = nextYm(ym)
        }

        postgres.syncUseMonthlyAgg(mysql.readUseMonthlyAgg())
        postgres.syncWeatherDaily(mysql.readWeatherDaily())
        postgres.syncDimHoliday(mysql.readHoliday())

        log.info("과거 이용 mart 동기화 완료 runId={}", runId)
    }

    private fun nextYm(ym: String): String {
        val y = ym.substring(0, 4).toInt()
        val m = ym.substring(4, 6).toInt()
        return if (m == 12) "${y + 1}01" else "%04d%02d".format(y, m + 1)
    }
}
