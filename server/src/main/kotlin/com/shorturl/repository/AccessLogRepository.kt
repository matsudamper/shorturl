package com.shorturl.repository

import com.shorturl.db.XodusDatabase
import com.shorturl.model.AccessLog
import jetbrains.exodus.entitystore.Entity
import java.time.Instant
import java.util.UUID

object AccessLogRepository {
    private const val TYPE = "AccessLog"

    fun record(
        slugId: String,
        ipAddress: String,
        userAgent: String,
        referer: String?,
        country: String?,
        deviceType: String,
        browser: String,
    ): Unit = XodusDatabase.write {
        val entity = newEntity(TYPE)
        entity.setProperty("id", UUID.randomUUID().toString())
        entity.setProperty("slugId", slugId)
        entity.setProperty("accessedAt", Instant.now().toEpochMilli())
        entity.setProperty("ipAddress", ipAddress)
        entity.setProperty("userAgent", userAgent)
        if (referer != null) entity.setProperty("referer", referer)
        if (country != null) entity.setProperty("country", country)
        entity.setProperty("deviceType", deviceType)
        entity.setProperty("browser", browser)
    }

    fun findBySlugId(slugId: String, limit: Int = 10000): List<AccessLog> = XodusDatabase.read {
        find(TYPE, "slugId", slugId).toList().take(limit).map { it.toModel() }
    }

    fun countBySlugId(slugId: String): Long = XodusDatabase.read {
        find(TYPE, "slugId", slugId).size()
    }

    private fun Entity.toModel() = AccessLog(
        id = getProperty("id") as String,
        slugId = getProperty("slugId") as String,
        accessedAt = getProperty("accessedAt") as Long,
        ipAddress = getProperty("ipAddress") as String,
        userAgent = getProperty("userAgent") as String,
        referer = getProperty("referer") as? String,
        country = getProperty("country") as? String,
        deviceType = getProperty("deviceType") as String,
        browser = getProperty("browser") as String,
    )
}
