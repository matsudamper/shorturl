package com.shorturl.db

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File

object AppDatabase {
    private var database: Database? = null

    fun init(dataDir: String) {
        close()

        val dbFile = resolveDatabaseFile(dataDir)
        dbFile.parentFile?.mkdirs()

        val connected = Database.connect(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            setupConnection = { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("PRAGMA busy_timeout = 5000")
                    statement.execute("PRAGMA journal_mode = WAL")
                    statement.execute("PRAGMA synchronous = NORMAL")
                    statement.execute("PRAGMA foreign_keys = ON")
                }
            }
        )
        transaction(connected) {
            @Suppress("DEPRECATION")
            SchemaUtils.createMissingTablesAndColumns(UsersTable, UrlsTable, AccessLogsTable)
        }
        database = connected
    }

    fun close() {
        database?.let(TransactionManager::closeAndUnregister)
        database = null
    }

    fun <T> read(block: JdbcTransaction.() -> T): T =
        transaction(requireDatabase(), statement = block)

    fun <T> write(block: JdbcTransaction.() -> T): T =
        transaction(requireDatabase(), statement = block)

    private fun requireDatabase(): Database =
        database ?: error("Database is not initialized")

    private fun resolveDatabaseFile(dataDir: String): File {
        val configured = File(dataDir)
        val looksLikeFile = configured.exists() && configured.isFile ||
            configured.extension.lowercase() in setOf("db", "sqlite", "sqlite3")
        return if (looksLikeFile) configured else configured.resolve("shorturl.db")
    }
}
