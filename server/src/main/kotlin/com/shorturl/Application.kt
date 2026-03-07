package com.shorturl

import com.shorturl.db.XodusDatabase
import com.shorturl.model.UserSession
import com.shorturl.routes.adminApiRoutes
import com.shorturl.routes.redirectRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

fun main() {
    val dataDir = System.getenv("DATA_DIR") ?: "./data"
    XodusDatabase.init(dataDir)

    val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
    Runtime.getRuntime().addShutdownHook(Thread { XodusDatabase.close() })
    server.start(wait = true)
}

fun Application.module() {
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
        adminApiRoutes()
        redirectRoutes()
    }
}
