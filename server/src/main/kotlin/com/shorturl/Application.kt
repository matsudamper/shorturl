package com.shorturl

import com.shorturl.db.XodusDatabase
import com.shorturl.model.UserSession
import com.shorturl.routes.adminApiRoutes
import com.shorturl.routes.redirectRoutes
import com.shorturl.service.GeoIpService
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import java.io.File

fun main() {
    val dataDir = System.getenv("DATA_DIR") ?: "./data"
    val geoipPath = System.getenv("GEOIP_MMDB") ?: "./GeoLite2-Country.mmdb"
    val adminDist = System.getenv("ADMIN_DIST") ?: "./admin-dist"

    XodusDatabase.init(dataDir)
    GeoIpService.init(geoipPath)

    val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module(adminDist)
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        XodusDatabase.close()
        GeoIpService.close()
    })
    server.start(wait = true)
}

fun Application.module(adminDist: String = "./admin-dist") {
    install(ContentNegotiation) {
        json()
    }

    val sessionSecret = System.getenv("SESSION_SECRET") ?: "shorturl-dev-secret-key-32bytes!!"
    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.httpOnly = true
            cookie.extensions["SameSite"] = "Strict"
            transform(SessionTransportTransformerMessageAuthentication(sessionSecret.toByteArray(Charsets.UTF_8)))
        }
    }

    routing {
        get("/health") {
            call.respondText("OK")
        }

        // 管理画面（Compose Wasm）の静的ファイル配信
        // ビルド手順: ./gradlew admin:wasmJsBrowserProductionWebpack
        //             cp -r admin/build/dist/wasmJs/productionExecutable/ admin-dist/
        staticFiles("/admin", File(adminDist)) {
            default("index.html")
        }

        adminApiRoutes()
        redirectRoutes()
    }
}
