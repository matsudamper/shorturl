package com.shorturl.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class CreateUserPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register<CreateUserTask>("createUser") {
            group = "application"
            description = "Creates a user in the SQLite datastore from a bcrypt password hash."
            username.convention(project.providers.gradleProperty("username"))
            passwordHash.convention(project.providers.gradleProperty("passwordHash"))
            dataDir.convention(
                project.providers.gradleProperty("dataDir")
                    .orElse(project.providers.environmentVariable("DATA_DIR"))
                    .orElse(project.rootProject.layout.projectDirectory.dir(".data").asFile.absolutePath)
            )
        }
    }
}
