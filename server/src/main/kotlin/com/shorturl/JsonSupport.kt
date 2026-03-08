package com.shorturl

import com.shorturl.model.ErrorResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

val ServerJson = Json { ignoreUnknownKeys = true }

suspend fun <T> ApplicationCall.receiveJson(serializer: KSerializer<T>): T {
    val text = receiveText()
    return try {
        ServerJson.decodeFromString(serializer, text)
    } catch (e: SerializationException) {
        throw BadRequestException("リクエストの形式が不正です", e)
    } catch (e: IllegalArgumentException) {
        throw BadRequestException("リクエストの形式が不正です", e)
    }
}

suspend fun <T> ApplicationCall.respondJson(
    serializer: KSerializer<T>,
    value: T,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    respondText(
        text = ServerJson.encodeToString(serializer, value),
        contentType = ContentType.Application.Json,
        status = status,
    )
}

suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    message: String,
) {
    respondJson(
        serializer = ErrorResponse.serializer(),
        value = ErrorResponse(message),
        status = status,
    )
}
