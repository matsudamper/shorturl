plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.54.1.0")
}

gradlePlugin {
    plugins {
        register("createuser") {
            id = "com.shorturl.gradle.createuser"
            implementationClass = "com.shorturl.gradle.CreateUserPlugin"
        }
    }
}
