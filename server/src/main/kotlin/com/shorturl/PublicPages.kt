package com.shorturl

import io.ktor.http.*
import io.ktor.server.application.*

suspend fun ApplicationCall.respondNotFoundPage() {
    respondEmbeddedResource("public/404.html", status = HttpStatusCode.NotFound)
}
