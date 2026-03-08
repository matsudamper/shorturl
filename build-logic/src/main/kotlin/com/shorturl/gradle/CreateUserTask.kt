package com.shorturl.gradle

import jetbrains.exodus.entitystore.PersistentEntityStores
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.mindrot.jbcrypt.BCrypt
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

        val store = PersistentEntityStores.newInstance(dataDir.get())
        store.use { store ->
            val exists = store.computeInReadonlyTransaction { txn ->
                txn.find("User", "username", user).firstOrNull() != null
            }
            if (exists) {
                throw GradleException("ユーザー '$user' は既に存在します")
            }

            val id = UUID.randomUUID().toString()
            store.executeInTransaction { txn ->
                val entity = txn.newEntity("User")
                entity.setProperty("id", id)
                entity.setProperty("username", user)
                entity.setProperty("passwordHash", BCrypt.hashpw(pass, BCrypt.gensalt()))
                entity.setProperty("createdAt", Instant.now().toEpochMilli())
            }
            logger.lifecycle("SUCCESS: ユーザーを作成しました (id=$id, username=$user)")
        }
    }
}
