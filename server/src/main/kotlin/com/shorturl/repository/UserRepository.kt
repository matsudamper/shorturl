package com.shorturl.repository

import com.shorturl.db.AppDatabase
import com.shorturl.db.UsersTable
import com.shorturl.model.User
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant
import java.util.UUID

object UserRepository {
    fun findByUsername(username: String): User? = AppDatabase.read {
        UsersTable.selectAll()
            .where { UsersTable.username eq username }
            .limit(1)
            .firstOrNull()
            ?.toModel()
    }

    fun findById(id: String): User? = AppDatabase.read {
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .limit(1)
            .firstOrNull()
            ?.toModel()
    }

    fun create(username: String, passwordHash: String): User = AppDatabase.write {
        val id = UUID.randomUUID().toString()
        val now = Instant.now().toEpochMilli()
        UsersTable.insert {
            it[UsersTable.id] = id
            it[UsersTable.username] = username
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.createdAt] = now
        }
        User(
            id = id,
            username = username,
            passwordHash = passwordHash,
            createdAt = now,
        )
    }

    private fun ResultRow.toModel() = User(
        id = this[UsersTable.id],
        username = this[UsersTable.username],
        passwordHash = this[UsersTable.passwordHash],
        createdAt = this[UsersTable.createdAt],
    )
}
