package com.shorturl.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class CreateUserPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register<CreateUserTask>("createUser") {
            group = "application"
            description = "Creates an admin user in the SQLite datastore."
            username.convention(project.providers.gradleProperty("username"))
            password.convention(project.providers.gradleProperty("password"))
            dataDir.convention(
                project.providers.gradleProperty("dataDir")
                    .orElse(project.providers.environmentVariable("DATA_DIR"))
                    .orElse(project.rootProject.layout.projectDirectory.dir(".data/exodus").asFile.absolutePath)
            )
        }
    }
}
