package com.shorturl

import com.shorturl.config.AppConfig
import com.shorturl.db.AppDatabase
import com.shorturl.model.UserSession
import com.shorturl.routes.adminApiRoutes
import com.shorturl.routes.redirectRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.path
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.sessions.serialization.*
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import io.opentelemetry.semconv.ServiceAttributes
import java.io.File
import java.time.ZonedDateTime
import java.time.ZoneOffset

fun main() {
    val config = AppConfig()
    config.printSummary()

    AppDatabase.init(config.dataDir)

    val server = embeddedServer(CIO, port = config.port, host = "0.0.0.0") {
        module(config)
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        AppDatabase.close()
    })
    server.start(wait = true)
}

fun getOpenTelemetry(serviceName: String): OpenTelemetry =
    AutoConfiguredOpenTelemetrySdk.builder()
        .addResourceCustomizer { oldResource, _ ->
            oldResource.toBuilder()
                .putAll(oldResource.attributes)
                .put(ServiceAttributes.SERVICE_NAME, serviceName)
                .build()
        }
        .build()
        .openTelemetrySdk

fun Application.module(config: AppConfig = AppConfig()) {
    install(KtorServerTelemetry) {
        setOpenTelemetry(getOpenTelemetry(serviceName = "shorturl"))
    }

    install(CachingHeaders) {
        options { _, outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Text.Html ->
                    CachingOptions(CacheControl.NoCache(null), ZonedDateTime.now(ZoneOffset.UTC))
                ContentType.parse("application/wasm"),
                ContentType.Application.JavaScript,
                ContentType.Text.CSS ->
                    CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 3600, visibility = CacheControl.Visibility.Public), ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(3600))
                ContentType.parse("font/ttf"),
                ContentType.parse("font/woff"),
                ContentType.parse("font/woff2") ->
                    CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 2592000, visibility = CacheControl.Visibility.Public), ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(2592000))
                else -> null
            }
        }
    }

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
            cookie.domain = config.domain
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
                registerExternalAdminResources(externalAdminDir)
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
        call.respondEmbeddedAdminResource("")
    }
    get("/admin/{resourcePath...}") {
        val relativePath = call.parameters.getAll("resourcePath").orEmpty().joinToString("/")
        call.respondEmbeddedAdminResource(relativePath)
    }
    get("/fonts/{resourcePath...}") {
        val relativePath = call.parameters.getAll("resourcePath").orEmpty().joinToString("/")
        val normalizedPath = normalizeAdminRelativePath(relativePath)
        if (normalizedPath.isNullOrBlank()) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        call.respondEmbeddedResource("admin/fonts/$normalizedPath")
    }
}

private fun Routing.registerExternalAdminResources(externalAdminDir: File) {
    get("/admin") {
        call.respondExternalAdminResource(externalAdminDir, "")
    }
    get("/admin/{resourcePath...}") {
        val relativePath = call.parameters.getAll("resourcePath").orEmpty().joinToString("/")
        call.respondExternalAdminResource(externalAdminDir, relativePath)
    }

    val fontsDir = externalAdminDir.resolve("fonts")
    if (fontsDir.isDirectory) {
        staticFiles("/fonts", fontsDir)
    }
}

private suspend fun ApplicationCall.respondEmbeddedAdminResource(relativePath: String) {
    val normalizedPath = normalizeAdminRelativePath(relativePath)
        ?: run {
            respond(HttpStatusCode.NotFound)
            return
        }
    val resourcePath = normalizedPath.takeIf { it.isNotBlank() }?.let { "admin/$it" } ?: "admin/index.html"
    when {
        embeddedResourceExists(resourcePath) -> respondEmbeddedResource(resourcePath)
        shouldServeAdminIndex(normalizedPath) -> respondEmbeddedResource("admin/index.html")
        else -> respond(HttpStatusCode.NotFound)
    }
}

private suspend fun ApplicationCall.respondExternalAdminResource(
    externalAdminDir: File,
    relativePath: String,
) {
    val normalizedPath = normalizeAdminRelativePath(relativePath)
        ?: run {
            respond(HttpStatusCode.NotFound)
            return
        }
    val requestedPath = normalizedPath.ifBlank { "index.html" }
    val requestedFile = externalAdminDir.resolve(requestedPath)
    if (requestedFile.isSafeChildOf(externalAdminDir) && requestedFile.isFile) {
        respondExternalResource(requestedFile, requestedPath)
        return
    }
    if (shouldServeAdminIndex(normalizedPath)) {
        val indexFile = externalAdminDir.resolve("index.html")
        if (indexFile.isSafeChildOf(externalAdminDir) && indexFile.isFile) {
            respondExternalResource(indexFile, "index.html")
            return
        }
    }
    respond(HttpStatusCode.NotFound)
}

private suspend fun ApplicationCall.respondExternalResource(file: File, resourcePath: String) {
    respondBytes(file.readBytes(), contentTypeForResourcePath(resourcePath))
}

internal suspend fun ApplicationCall.respondEmbeddedResource(
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

private fun ApplicationCall.embeddedResourceExists(resourcePath: String): Boolean {
    val normalizedPath = resourcePath.trimStart('/').replace('\\', '/')
    return application.environment.classLoader.getResource(normalizedPath) != null
}

private fun normalizeAdminRelativePath(relativePath: String): String? {
    val normalizedPath = relativePath.trimStart('/').replace('\\', '/')
    if (normalizedPath.isBlank()) {
        return ""
    }
    if (normalizedPath.split('/').any { it == "." || it == ".." }) {
        return null
    }
    return normalizedPath
}

private fun shouldServeAdminIndex(relativePath: String): Boolean {
    if (relativePath.isBlank()) {
        return true
    }
    return !relativePath.substringAfterLast('/').contains('.')
}

private fun File.isSafeChildOf(root: File): Boolean {
    val rootPath = root.canonicalFile.toPath()
    val filePath = canonicalFile.toPath()
    return filePath.startsWith(rootPath)
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
