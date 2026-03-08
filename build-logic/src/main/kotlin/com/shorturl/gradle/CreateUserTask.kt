package com.shorturl.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.mindrot.jbcrypt.BCrypt
import java.io.File
import java.sql.DriverManager
import java.time.Instant
import java.util.UUID

abstract class CreateUserTask : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val username: Property<String>

    @get:Input
    @get:Optional
    abstract val password: Property<String>

    @get:Input
    abstract val dataDir: Property<String>

    @TaskAction
    fun execute() {
        val user = username.orNull
            ?: throw GradleException("username が指定されていません。-Pusername=<name> を付けてください")
        val pass = password.orNull
            ?: throw GradleException("password が指定されていません。-Ppassword=<pass> を付けてください")

        val dbFile = resolveDatabaseFile(dataDir.get())
        dbFile.parentFile?.mkdirs()

        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT PRIMARY KEY NOT NULL,
                        username TEXT NOT NULL,
                        password_hash TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS users_username ON users (username)")
            }

            connection.prepareStatement("SELECT 1 FROM users WHERE username = ? LIMIT 1").use { statement ->
                statement.setString(1, user)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        throw GradleException("ユーザー '$user' は既に存在します")
                    }
                }
            }

            val id = UUID.randomUUID().toString()
            connection.prepareStatement(
                """
                INSERT INTO users (id, username, password_hash, created_at)
                VALUES (?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, id)
                statement.setString(2, user)
                statement.setString(3, BCrypt.hashpw(pass, BCrypt.gensalt()))
                statement.setLong(4, Instant.now().toEpochMilli())
                statement.executeUpdate()
            }
            logger.lifecycle("SUCCESS: ユーザーを作成しました (id=$id, username=$user)")
        }
    }

    private fun resolveDatabaseFile(dataDir: String): File {
        val configured = File(dataDir)
        val looksLikeFile = configured.exists() && configured.isFile ||
            configured.extension.lowercase() in setOf("db", "sqlite", "sqlite3")
        return if (looksLikeFile) configured else configured.resolve("shorturl.db")
    }
}
