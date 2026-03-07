package com.shorturl.service

import com.shorturl.model.AnalyticsSummary
import com.shorturl.repository.AccessLogRepository
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object AnalyticsService {
    private val dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)

    fun getSummary(slugId: String): AnalyticsSummary {
        val logs = AccessLogRepository.findBySlugId(slugId)

        return AnalyticsSummary(
            totalClicks = logs.size.toLong(),
            dailyStats = logs.groupBy {
                dayFormatter.format(Instant.ofEpochMilli(it.accessedAt))
            }.mapValues { it.value.size.toLong() },
            hourlyStats = logs.groupBy {
                Instant.ofEpochMilli(it.accessedAt).atZone(ZoneOffset.UTC).hour.toString()
            }.mapValues { it.value.size.toLong() },
            referrers = logs.groupBy { it.referer ?: "(direct)" }
                .mapValues { it.value.size.toLong() },
            deviceTypes = logs.groupBy { it.deviceType }
                .mapValues { it.value.size.toLong() },
            browsers = logs.groupBy { it.browser }
                .mapValues { it.value.size.toLong() },
        )
    }
}
