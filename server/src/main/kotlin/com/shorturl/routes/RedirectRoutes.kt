package com.shorturl.routes

import com.shorturl.respondNotFoundPage
import com.shorturl.repository.AccessLogRepository
import com.shorturl.repository.UrlRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.redirectRoutes() {
    get("/") {
        call.respondNotFoundPage()
    }

    get("/{slug}") {
        val rawSlug = call.parameters["slug"] ?: run {
            call.respondNotFoundPage()
            return@get
        }
        val decodedSlug = runCatching { java.net.URLDecoder.decode(rawSlug, Charsets.UTF_8) }.getOrElse { rawSlug }

        val shortened = UrlRepository.findBySlug(rawSlug)
            ?: UrlRepository.findBySlug(decodedSlug)
            ?: run {
                call.respondNotFoundPage()
                return@get
            }

        val userAgent = call.request.userAgent() ?: "unknown"
        UrlRepository.incrementClickCount(shortened.id)
        AccessLogRepository.record(
            slugId = shortened.id,
            ipAddress = call.request.origin.remoteAddress,
            userAgent = userAgent,
            referer = call.request.headers[HttpHeaders.Referrer],
            deviceType = parseDeviceType(userAgent),
            browser = parseBrowser(userAgent),
        )

        call.respondRedirect(shortened.originalUrl, permanent = false)
    }

    get("{...}") {
        val path = call.request.path()
        val looksLikeAsset = path.substringAfterLast('/').contains('.')
        if (path.startsWith("/admin") || path.startsWith("/fonts") || looksLikeAsset) {
            call.respondText("Not Found", status = HttpStatusCode.NotFound)
            return@get
        }
        call.respondNotFoundPage()
    }
}

private fun parseDeviceType(userAgent: String): String {
    val ua = userAgent.lowercase()
    return when {
        "ipad" in ua || "tablet" in ua -> "Tablet"
        "mobile" in ua || "android" in ua -> "Mobile"
        else -> "PC"
    }
}

private fun parseBrowser(userAgent: String): String {
    val ua = userAgent.lowercase()
    return when {
        "edg/" in ua -> "Edge"
        "chrome/" in ua -> "Chrome"
        "firefox/" in ua -> "Firefox"
        "safari/" in ua && "chrome" !in ua -> "Safari"
        "opr/" in ua || "opera" in ua -> "Opera"
        else -> "Other"
    }
}
