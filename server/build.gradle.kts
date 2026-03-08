plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    id("org.graalvm.buildtools.native")
    id("com.shorturl.gradle.createuser")
}

application {
    mainClass.set("com.shorturl.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
}

val graalVmLanguageVersion = JavaLanguageVersion.of(24)

java {
    toolchain {
        languageVersion = graalVmLanguageVersion
        vendor = JvmVendorSpec.GRAAL_VM
    }
}

kotlin {
    jvmToolchain {
        languageVersion = graalVmLanguageVersion
        vendor = JvmVendorSpec.GRAAL_VM
    }
}

val graalVmLauncher = javaToolchains.launcherFor {
    languageVersion = graalVmLanguageVersion
    vendor = JvmVendorSpec.GRAAL_VM
}

graalvmNative {
    toolchainDetection.set(true)
    binaries.configureEach {
        javaLauncher.set(graalVmLauncher)
    }

    // GraalVM Reachability Metadata Repository を有効化
    // logback / netty / jackson 等の人気ライブラリのリフレクション設定を自動取得
    metadataRepository {
        enabled = true
    }
    binaries {
        named("main") {
            mainClass.set("com.shorturl.ApplicationKt")
            buildArgs.addAll(
                "--no-fallback",
                "-H:+ReportExceptionStackTraces",
            )
        }
    }
}

repositories {
    mavenCentral()
}

tasks.named<Test>("test") {
    workingDir = rootProject.projectDir
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.sessions)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.ktor.serialization.json)
    implementation(libs.logback.classic)
    implementation(libs.xodus.open.api)
    implementation(libs.xodus.entity.store)
    implementation(libs.jbcrypt)
    implementation(libs.maxmind.geoip2)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.kotlin.test)
}
