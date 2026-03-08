plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.xodus:xodus-openAPI:2.0.1")
    implementation("org.jetbrains.xodus:xodus-entity-store:2.0.1")
    implementation("org.mindrot:jbcrypt:0.4")
}

gradlePlugin {
    plugins {
        register("createuser") {
            id = "com.shorturl.gradle.createuser"
            implementationClass = "com.shorturl.gradle.CreateUserPlugin"
        }
    }
}
