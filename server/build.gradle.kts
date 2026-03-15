import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.GradleException
import org.gradle.api.tasks.Sync
import org.gradle.jvm.tasks.Jar
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.createLinkPointingTo
import kotlin.io.path.deleteExisting
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    id("org.graalvm.buildtools.native")
    id("com.shorturl.gradle.createuser")
}

val serverBuildProfile = providers.gradleProperty("serverBuildProfile")
    .orElse("")
    .map(String::trim)
    .map(String::lowercase)

val requestedTaskNames = gradle.startParameter.taskNames.map { it.substringAfterLast(':') }
val runLikeTaskNames = setOf("run", "nativeRun", "runShadow", "runFatJar")
val artifactTaskNames = setOf("jar", "shadowJar", "buildFatJar", "nativeCompile", "assemble", "build")
val skipEmbeddedAdminResourcesForLocalRun =
    serverBuildProfile.get() == "" ||
            (requestedTaskNames.any { it in runLikeTaskNames } &&
                    requestedTaskNames.none { it in artifactTaskNames })

val adminWebpackTask = provider {
    val adminWebpackTaskName = when (val profile = serverBuildProfile.get()) {
        "prod" -> "wasmJsBrowserDistribution"
        "dev" -> "wasmJsBrowserDevelopmentExecutableDistribution"
        "" -> return@provider null
        else -> throw GradleException(
            "Unsupported serverBuildProfile=$profile. Use -PserverBuildProfile=dev or -PserverBuildProfile=prod."
        )
    }
    project(":admin").tasks.named(adminWebpackTaskName)
}

val adminDistDirectory = provider {
    val adminDistDirectory = when (serverBuildProfile.get()) {
        "prod" -> "productionExecutable"
        "dev" -> "developmentExecutable"
        else -> return@provider null
    }
    project(":admin").layout.buildDirectory.dir("dist/wasmJs/$adminDistDirectory")
}

val embedAdminResources by tasks.registering(Sync::class) {
    description = "Builds the admin UI for '${serverBuildProfile.get()}' and embeds it into the server resources."
    dependsOn(adminWebpackTask)
    val serverProfile = serverBuildProfile.get().ifBlank { null }
    if (serverProfile != null) {
        into(layout.buildDirectory.dir("generated/admin-resources/${serverProfile}"))
        inputs.property("serverBuildProfile", serverProfile)
    }
    from(adminDistDirectory) {
        into("admin")
    }
}

sourceSets.named("main") {
    if (!skipEmbeddedAdminResourcesForLocalRun) {
        resources.srcDir(embedAdminResources)
    }
}

application {
    mainClass.set("com.shorturl.ApplicationKt")
    applicationDefaultJvmArgs = listOf(
        "-Dio.ktor.development=${
            when (serverBuildProfile.get()) {
                "dev", "" -> true
                else -> false
            }
        }",
        "--enable-native-access=ALL-UNNAMED",
    )
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

// Workaround: https://github.com/gradle/gradle/issues/28583
// Gradle のコピー処理で GraalVM JDK 内のシンボリックリンクが空ファイルに化ける問題を修正する。
// 空ファイルになった native-image を削除し、実体へのハードリンクとして再作成する。
fun fixSymlink(target: Path, expectedSrc: Path) {
    if (!expectedSrc.isRegularFile(LinkOption.NOFOLLOW_LINKS)) {
        logger.info("fixSymlink: expected is not regular, skip (expected: {})", expectedSrc)
        return
    }
    if (!target.isRegularFile(LinkOption.NOFOLLOW_LINKS) || target.fileSize() > 0) {
        logger.info("fixSymlink: target is not regular or the file size > 0, skip (target: {})", target)
        return
    }
    logger.warn("fixSymlink: {} -> {}", target, expectedSrc)
    target.deleteExisting()
    target.createLinkPointingTo(expectedSrc)
}

graalvmNative {
    toolchainDetection.set(true)
    binaries.configureEach {
        javaLauncher.set(graalVmLauncher)
    }

    // GraalVM Reachability Metadata Repository を有効化
    // logback / jackson 等の人気ライブラリのリフレクション設定を自動取得
    metadataRepository {
        enabled = true
    }
    binaries {
        named("main") {
            mainClass.set("com.shorturl.ApplicationKt")
            imageName.set("shorturl-${serverBuildProfile.get()}")
            buildArgs.addAll(
                "--no-fallback",
                "-H:+ReportExceptionStackTraces",
            )
            resources {
                autodetect()
                includedPatterns.add("admin/.*")
            }
        }
    }
}

repositories {
    mavenCentral()
}

tasks.named<Test>("test") {
    workingDir = rootProject.projectDir
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.named<Jar>("jar") {
    archiveClassifier.set(serverBuildProfile.get())
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${project.name}-${serverBuildProfile.get()}-all.jar")
}

// Workaround: https://github.com/gradle/gradle/issues/28583
// nativeCompile / nativeTestCompile の前に壊れたシンボリックリンクを修復する
tasks.matching { it.name == "nativeCompile" || it.name == "nativeTestCompile" }.configureEach {
    doFirst {
        val binPath = graalVmLauncher.get().executablePath.asFile.toPath().parent
        val svmBinPath = binPath.resolve("../lib/svm/bin")
        fixSymlink(binPath.resolve("native-image"), svmBinPath.resolve("native-image"))
    }
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.sessions)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.ktor.serialization.json)
    implementation(libs.logback.classic)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.sqlite.jdbc)
    implementation(libs.jbcrypt)
    implementation(libs.maxmind.db)
    implementation(libs.opentelemetry.ktor)
    implementation(libs.opentelemetry.sdk.autoconfigure)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(libs.opentelemetry.semconv)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.kotlin.test)
}
