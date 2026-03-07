package com.shorturl.repository

import com.shorturl.db.XodusDatabase
import com.shorturl.model.User
import jetbrains.exodus.entitystore.Entity
import java.time.Instant
import java.util.UUID

object UserRepository {
    private const val TYPE = "User"

    fun findByUsername(username: String): User? = XodusDatabase.read {
        find(TYPE, "username", username).firstOrNull()?.toModel()
    }

    fun findById(id: String): User? = XodusDatabase.read {
        find(TYPE, "id", id).firstOrNull()?.toModel()
    }

    fun create(username: String, passwordHash: String): User = XodusDatabase.writeReturning {
        val id = UUID.randomUUID().toString()
        val now = Instant.now().toEpochMilli()
        val entity = newEntity(TYPE)
        entity.setProperty("id", id)
        entity.setProperty("username", username)
        entity.setProperty("passwordHash", passwordHash)
        entity.setProperty("createdAt", now)
        entity.toModel()
    }

    private fun Entity.toModel() = User(
        id = getProperty("id") as String,
        username = getProperty("username") as String,
        passwordHash = getProperty("passwordHash") as String,
        createdAt = getProperty("createdAt") as Long,
    )
}
