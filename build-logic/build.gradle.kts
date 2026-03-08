plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
}

gradlePlugin {
    plugins {
        register("createuser") {
            id = "com.shorturl.gradle.createuser"
            implementationClass = "com.shorturl.gradle.CreateUserPlugin"
        }
    }
}
