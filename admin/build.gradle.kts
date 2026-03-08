import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import java.net.URI

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val fontsDir = layout.projectDirectory.dir("src/wasmJsMain/resources/fonts")

tasks.register("downloadFonts") {
    outputs.dir(fontsDir)
    doLast {
        fontsDir.asFile.mkdirs()
        val target = fontsDir.file("NotoSansJP.ttf").asFile
        if (!target.exists()) {
            println("Downloading NotoSansJP.ttf...")
            val url = URI("https://github.com/google/fonts/raw/main/ofl/notosansjp/NotoSansJP%5Bwght%5D.ttf").toURL()
            target.writeBytes(url.readBytes())
            println("Downloaded to ${target.absolutePath}")
        } else {
            println("NotoSansJP.ttf already exists, skipping.")
        }
    }
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "admin.js"
                devServer = KotlinWebpackConfig.DevServer(
                    port = 8081,
                    proxy = mutableListOf(
                        KotlinWebpackConfig.DevServer.Proxy(
                            context = mutableListOf("/internal"),
                            target = "http://localhost:8080",
                            changeOrigin = true,
                        )
                    )
                )
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.js)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json.mp)
                implementation(libs.navigation3.ui)
            }
        }
    }
}
