package com.shorturl

import com.shorturl.config.AppConfig
import com.shorturl.db.AppDatabase
import com.shorturl.model.UserSession
import com.shorturl.routes.adminApiRoutes
import com.shorturl.routes.redirectRoutes
import com.shorturl.service.GeoIpService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.path
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.sessions.serialization.*
import java.io.File

fun main() {
    val config = AppConfig()
    config.printSummary()

    AppDatabase.init(config.dataDir)
    GeoIpService.init(config.geoipMmdb)

    val server = embeddedServer(CIO, port = config.port, host = config.host) {
        module(config)
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        AppDatabase.close()
        GeoIpService.close()
    })
    server.start(wait = true)
}

fun Application.module(config: AppConfig = AppConfig()) {
    install(ContentNegotiation) {
        json(ServerJson)
    }

    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            if (call.request.path().startsWith("/internal")) {
                call.respondError(HttpStatusCode.BadRequest, cause.message ?: "不正なリクエストです")
            } else {
                call.respondText("Bad Request", status = HttpStatusCode.BadRequest)
            }
        }
        exception<ContentTransformationException> { call, cause ->
            if (call.request.path().startsWith("/internal")) {
                call.respondError(HttpStatusCode.BadRequest, cause.message ?: "リクエストの形式が不正です")
            } else {
                call.respondText("Bad Request", status = HttpStatusCode.BadRequest)
            }
        }
    }

    install(Sessions) {
        cookie<UserSession>("user_session") {
            serializer = KotlinxSessionSerializer(UserSession.serializer(), ServerJson)
            cookie.httpOnly = true
            cookie.secure = config.cookieSecure
            cookie.extensions["SameSite"] = "Strict"
            transform(
                SessionTransportTransformerMessageAuthentication(
                    config.sessionSecret.toByteArray(Charsets.UTF_8)
                )
            )
        }
    }

    routing {
        get("/healthz") {
            call.respondText("OK")
        }

        // 管理画面（Compose Wasm）
        // ビルド: ./gradlew admin:wasmJsBrowserProductionWebpack
        //         cp -r admin/build/dist/wasmJs/productionExecutable/ $ADMIN_DIST
        val adminDir = File(config.adminDist)
        if (adminDir.exists()) {
            staticFiles("/admin", adminDir) {
                default("index.html")
            }
            val fontsDir = adminDir.resolve("fonts")
            if (fontsDir.exists()) {
                staticFiles("/fonts", fontsDir)
            }
        }

        adminApiRoutes()
        redirectRoutes()
    }
}
