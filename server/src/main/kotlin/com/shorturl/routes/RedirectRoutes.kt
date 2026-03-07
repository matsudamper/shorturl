package com.shorturl.routes

import com.shorturl.repository.AccessLogRepository
import com.shorturl.repository.UrlRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.redirectRoutes() {
    get("/{slug}") {
        val rawSlug = call.parameters["slug"] ?: run {
            call.respond(HttpStatusCode.NotFound, "Not Found")
            return@get
        }
        val decodedSlug = runCatching { java.net.URLDecoder.decode(rawSlug, Charsets.UTF_8) }.getOrElse { rawSlug }

        val shortened = UrlRepository.findBySlug(rawSlug)
            ?: UrlRepository.findBySlug(decodedSlug)
            ?: run {
                call.respond(HttpStatusCode.NotFound, "短縮URLが見つかりませんでした")
                return@get
            }

        val userAgent = call.request.userAgent() ?: "unknown"
        UrlRepository.incrementClickCount(shortened.id)
        AccessLogRepository.record(
            slugId = shortened.id,
            ipAddress = call.request.origin.remoteAddress,
            userAgent = userAgent,
            referer = call.request.headers[HttpHeaders.Referrer],
            country = null,
            deviceType = parseDeviceType(userAgent),
            browser = parseBrowser(userAgent),
        )

        call.respondRedirect(shortened.originalUrl, permanent = true)
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
