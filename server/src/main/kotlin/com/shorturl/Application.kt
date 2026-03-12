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

        val externalAdminDir = config.adminDistDir
        when {
            externalAdminDir?.isDirectory == true -> {
                staticFiles("/admin", externalAdminDir) {
                    default("index.html")
                }
                val fontsDir = externalAdminDir.resolve("fonts")
                if (fontsDir.isDirectory) {
                    staticFiles("/fonts", fontsDir)
                }
            }

            externalAdminDir != null -> {
                environment.log.warn("ADMIN_DIST is not a readable directory: {}", externalAdminDir.absolutePath)
                registerEmbeddedAdminResources()
            }

            else -> registerEmbeddedAdminResources()
        }

        adminApiRoutes()
        redirectRoutes()
    }
}

private fun Routing.registerEmbeddedAdminResources() {
    get("/admin") {
        call.respondEmbeddedResource("admin/index.html")
    }
    get("/admin/{resourcePath...}") {
        val relativePath = call.parameters.getAll("resourcePath").orEmpty().joinToString("/")
        if (relativePath.isBlank()) {
            call.respondEmbeddedResource("admin/index.html")
            return@get
        }
        call.respondEmbeddedResource("admin/$relativePath")
    }
    get("/fonts/{resourcePath...}") {
        val relativePath = call.parameters.getAll("resourcePath").orEmpty().joinToString("/")
        if (relativePath.isBlank()) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        call.respondEmbeddedResource("admin/fonts/$relativePath")
    }
}

suspend fun ApplicationCall.respondEmbeddedResource(
    resourcePath: String,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    val normalizedPath = resourcePath.trimStart('/').replace('\\', '/')
    val resourceStream = application.environment.classLoader.getResourceAsStream(normalizedPath)
        ?: run {
            application.environment.log.warn("Embedded resource not found: {}", normalizedPath)
            respond(HttpStatusCode.NotFound)
            return
        }

    resourceStream.use { stream ->
        respondBytes(stream.readBytes(), contentTypeForResourcePath(normalizedPath), status)
    }
}

private fun contentTypeForResourcePath(resourcePath: String): ContentType =
    when (resourcePath.substringAfterLast('.', "")) {
        "html" -> ContentType.Text.Html.withCharset(Charsets.UTF_8)
        "js", "mjs" -> ContentType.Application.JavaScript.withCharset(Charsets.UTF_8)
        "map", "json" -> ContentType.Application.Json.withCharset(Charsets.UTF_8)
        "wasm" -> ContentType.parse("application/wasm")
        "css" -> ContentType.Text.CSS.withCharset(Charsets.UTF_8)
        "ttf" -> ContentType.parse("font/ttf")
        "txt" -> ContentType.Text.Plain.withCharset(Charsets.UTF_8)
        else -> ContentType.Application.OctetStream
    }
