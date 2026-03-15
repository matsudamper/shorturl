package com.shorturl.repository

import com.shorturl.db.AccessLogsTable
import com.shorturl.db.AppDatabase
import com.shorturl.model.AccessLog
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant
import java.util.UUID

object AccessLogRepository {
    fun record(
        slugId: String,
        ipAddress: String,
        userAgent: String,
        referer: String?,
        deviceType: String,
        browser: String,
    ): Unit = AppDatabase.write {
        AccessLogsTable.insert {
            it[id] = UUID.randomUUID().toString()
            it[AccessLogsTable.slugId] = slugId
            it[accessedAt] = Instant.now().toEpochMilli()
            it[AccessLogsTable.ipAddress] = ipAddress
            it[AccessLogsTable.userAgent] = userAgent
            it[AccessLogsTable.referer] = referer
            it[AccessLogsTable.deviceType] = deviceType
            it[AccessLogsTable.browser] = browser
        }
    }

    fun findBySlugId(slugId: String, limit: Int = 10000): List<AccessLog> = AppDatabase.read {
        AccessLogsTable.selectAll()
            .where { AccessLogsTable.slugId eq slugId }
            .orderBy(AccessLogsTable.accessedAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toModel() }
    }

    fun countBySlugId(slugId: String): Long = AppDatabase.read {
        AccessLogsTable.selectAll()
            .where { AccessLogsTable.slugId eq slugId }
            .count()
    }

    private fun ResultRow.toModel() = AccessLog(
        id = this[AccessLogsTable.id],
        slugId = this[AccessLogsTable.slugId],
        accessedAt = this[AccessLogsTable.accessedAt],
        ipAddress = this[AccessLogsTable.ipAddress],
        userAgent = this[AccessLogsTable.userAgent],
        referer = this[AccessLogsTable.referer],
        deviceType = this[AccessLogsTable.deviceType],
        browser = this[AccessLogsTable.browser],
    )
}
