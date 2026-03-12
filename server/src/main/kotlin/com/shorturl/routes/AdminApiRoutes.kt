package com.shorturl.routes

import com.shorturl.receiveJson
import com.shorturl.respondError
import com.shorturl.respondJson
import com.shorturl.model.*
import com.shorturl.repository.UserRepository
import com.shorturl.repository.UrlRepository
import com.shorturl.service.AnalyticsService
import com.shorturl.service.AuthService
import com.shorturl.service.SlugGenerator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.builtins.ListSerializer

fun Route.adminApiRoutes() {
    route("/internal") {
        // --- 認証不要 ---
        post("/auth/login") {
            val req = call.receiveJson(LoginRequest.serializer())
            val user = AuthService.authenticate(req.username, req.password)
            if (user == null) {
                call.respondError(HttpStatusCode.Unauthorized, "ユーザー名またはパスワードが違います")
                return@post
            }
            call.sessions.set(UserSession(userId = user.id, username = user.username))
            call.respondJson(LoginResponse.serializer(), LoginResponse(user.id, user.username))
        }

        post("/auth/logout") {
            call.sessions.clear<UserSession>()
            call.respondJson(OkResponse.serializer(), OkResponse(true))
        }

        // bcrypt ハッシュ生成ツール（ユーザー登録用）
        post("/auth/hash") {
            val req = call.receiveJson(HashRequest.serializer())
            if (req.password.isBlank()) {
                call.respondError(HttpStatusCode.BadRequest, "パスワードを入力してください")
                return@post
            }
            call.respondJson(HashResponse.serializer(), HashResponse(AuthService.hashPassword(req.password)))
        }

        // --- 認証必須 ---
        route("/users") {
            get {
                call.requireSession() ?: return@get
                val users = UserRepository.findAll().map {
                    UserSummary(
                        id = it.id,
                        username = it.username,
                        createdAt = it.createdAt,
                    )
                }
                call.respondJson(ListSerializer(UserSummary.serializer()), users)
            }

            delete("/{id}") {
                val session = call.requireSession() ?: return@delete
                val id = call.parameters["id"]!!
                if (!UserRepository.delete(id)) {
                    call.respondError(HttpStatusCode.NotFound, "ユーザーが見つかりません")
                    return@delete
                }

                val deletedCurrentUser = id == session.userId
                if (deletedCurrentUser) {
                    call.sessions.clear<UserSession>()
                }
                call.respondJson(
                    DeleteUserResponse.serializer(),
                    DeleteUserResponse(ok = true, deletedCurrentUser = deletedCurrentUser),
                )
            }
        }

        route("/urls") {
            get {
                call.requireSession() ?: return@get
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 20).coerceIn(1, 100)
                val q = call.request.queryParameters["q"]

                if (q != null && q.isNotBlank()) {
                    call.respondJson(
                        PagedResponse.serializer(),
                        PagedResponse(
                            items = UrlRepository.search(q, offset, limit),
                            total = UrlRepository.searchCount(q),
                            offset = offset,
                            limit = limit,
                        )
                    )
                } else {
                    call.respondJson(
                        PagedResponse.serializer(),
                        PagedResponse(
                            items = UrlRepository.findAll(offset, limit),
                            total = UrlRepository.count(),
                            offset = offset,
                            limit = limit,
                        )
                    )
                }
            }

            post {
                val session = call.requireSession() ?: return@post
                val req = call.receiveJson(CreateUrlRequest.serializer())

                if (!isValidUrl(req.originalUrl)) {
                    call.respondError(HttpStatusCode.BadRequest, "URLは http:// または https:// で始まる必要があります")
                    return@post
                }
                if (req.slug.length < 2 || req.slug.length > 128) {
                    call.respondError(HttpStatusCode.BadRequest, "スラッグは2〜128文字にしてください")
                    return@post
                }
                if (isReservedSlug(req.slug)) {
                    call.respondError(HttpStatusCode.BadRequest, "このスラッグは予約済みです")
                    return@post
                }
                if (UrlRepository.existsBySlug(req.slug)) {
                    call.respondError(HttpStatusCode.Conflict, "このスラッグは既に使用されています")
                    return@post
                }

                val created = UrlRepository.create(
                    slug = req.slug,
                    originalUrl = req.originalUrl,
                    isAutoGenerated = req.isAutoGenerated,
                    createdBy = session.userId,
                )
                call.respondJson(ShortenedUrl.serializer(), created, HttpStatusCode.Created)
            }

            get("/{id}") {
                call.requireSession() ?: return@get
                val id = call.parameters["id"]!!
                val url = UrlRepository.findById(id)
                    ?: return@get call.respondError(HttpStatusCode.NotFound, "URLが見つかりません")
                call.respondJson(ShortenedUrl.serializer(), url)
            }

            put("/{id}") {
                call.requireSession() ?: return@put
                val id = call.parameters["id"]!!
                val req = call.receiveJson(UpdateUrlRequest.serializer())

                if (!isValidUrl(req.originalUrl)) {
                    call.respondError(HttpStatusCode.BadRequest, "URLは http:// または https:// で始まる必要があります")
                    return@put
                }

                val updated = UrlRepository.updateOriginalUrl(id, req.originalUrl)
                    ?: return@put call.respondError(HttpStatusCode.NotFound, "URLが見つかりません")
                call.respondJson(ShortenedUrl.serializer(), updated)
            }

            delete("/{id}") {
                call.requireSession() ?: return@delete
                val id = call.parameters["id"]!!
                if (!UrlRepository.delete(id)) {
                    call.respondError(HttpStatusCode.NotFound, "URLが見つかりません")
                    return@delete
                }
                call.respondJson(OkResponse.serializer(), OkResponse(true))
            }

            get("/{id}/analytics") {
                call.requireSession() ?: return@get
                val id = call.parameters["id"]!!
                UrlRepository.findById(id)
                    ?: return@get call.respondError(HttpStatusCode.NotFound, "URLが見つかりません")
                call.respondJson(AnalyticsSummary.serializer(), AnalyticsService.getSummary(id))
            }
        }

        // スラッグ生成（プレビュー用・DBに保存しない）
        post("/slugs/generate") {
            call.requireSession() ?: return@post
            val req = call.receiveJson(GenerateSlugRequest.serializer())
            val type = SlugGenerator.parseType(req.type)
                ?: return@post call.respondError(HttpStatusCode.BadRequest, "不正なタイプです")

            val minLen = SlugGenerator.minLength(type)
            val maxLen = SlugGenerator.maxLength(type)
            if (req.length !in minLen..maxLen) {
                call.respondError(HttpStatusCode.BadRequest, "文字数は $minLen〜$maxLen の範囲で指定してください")
                return@post
            }

            val slug = SlugGenerator.generateUnique(type, req.length)
                ?: return@post call.respondError(HttpStatusCode.Conflict, "スラッグの生成に失敗しました。再度お試しください")
            call.respondJson(GenerateSlugResponse.serializer(), GenerateSlugResponse(slug))
        }
    }
}

private suspend fun ApplicationCall.requireSession(): UserSession? {
    val session = sessions.get<UserSession>()
    if (session == null) {
        respondError(HttpStatusCode.Unauthorized, "ログインが必要です")
        return null
    }
    if (UserRepository.findById(session.userId) == null) {
        sessions.clear<UserSession>()
        respondError(HttpStatusCode.Unauthorized, "セッションが無効です。再度ログインしてください")
        return null
    }
    return session
}

private fun isValidUrl(url: String): Boolean =
    url.startsWith("http://") || url.startsWith("https://")

private fun isReservedSlug(slug: String): Boolean {
    val lower = slug.lowercase()
    return lower.startsWith("admin") || lower.startsWith("internal") ||
        lower in setOf("health", "favicon.ico", "robots.txt")
}
