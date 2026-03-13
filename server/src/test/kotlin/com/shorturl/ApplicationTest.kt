package com.shorturl

import com.shorturl.config.AppConfig
import com.shorturl.db.AppDatabase
import com.shorturl.model.*
import com.shorturl.repository.UserRepository
import com.shorturl.service.AuthService
import com.shorturl.service.GeoIpService
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.builtins.ListSerializer
import io.ktor.server.testing.*
import java.nio.file.Files
import kotlin.test.*

class ApplicationTest {

    private val tempDir = Files.createTempDirectory("shorturl-test").toFile()
    private lateinit var testConfig: AppConfig

    @BeforeTest
    fun setup() {
        testConfig = AppConfig(sessionSecret = "test-secret-key-for-testing-only-32ch")
        AppDatabase.init(tempDir.absolutePath)
        GeoIpService.init("nonexistent") // ファイルなし → no-op
        UserRepository.create("admin", AuthService.hashPassword("password"))
    }

    @AfterTest
    fun teardown() {
        AppDatabase.close()
        tempDir.deleteRecursively()
    }

    // ────────────────────────────────────────────
    // ヘルスチェック
    // ────────────────────────────────────────────

    @Test
    fun `GET healthz returns OK`() = testApplication {
        application { module(testConfig) }
        val res = client.get("/healthz")
        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals("OK", res.body<String>())
    }

    // ────────────────────────────────────────────
    // 管理画面
    // ────────────────────────────────────────────

    @Test
    fun `GET admin returns 200 from embedded resources`() = testApplication {
        application { module(testConfig) }
        val res = client.get("/admin/")
        assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun `GET admin font returns 200 from embedded resources`() = testApplication {
        application { module(testConfig) }
        val res = client.get("/fonts/NotoSansJP-Regular.ttf")
        assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun `GET admin javascript returns 200 from embedded resources`() = testApplication {
        application { module(testConfig) }
        val res = client.get("/admin/admin.js")
        assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun `GET admin SPA route returns embedded index`() = testApplication {
        application { module(testConfig) }

        val listRes = client.get("/admin/urls")
        assertEquals(HttpStatusCode.OK, listRes.status)
        assertTrue(listRes.bodyAsText().contains("admin.js"))

        val editRes = client.get("/admin/urls/test-id/edit")
        assertEquals(HttpStatusCode.OK, editRes.status)
        assertTrue(editRes.bodyAsText().contains("admin.js"))
    }

    @Test
    fun `GET missing admin asset returns 404`() = testApplication {
        application { module(testConfig) }
        val res = client.get("/admin/missing.js")
        assertEquals(HttpStatusCode.NotFound, res.status)
    }

    @Test
    fun `GET admin prefers external adminDist when specified`() = testApplication {
        val externalAdminDir = tempDir.resolve("admin-dist").apply { mkdirs() }
        externalAdminDir.resolve("index.html").writeText("external-admin", Charsets.UTF_8)
        externalAdminDir.resolve("fonts").mkdirs()
        externalAdminDir.resolve("fonts").resolve("NotoSansJP-Regular.ttf").writeText("external-font", Charsets.UTF_8)

        application { module(testConfig.copy(adminDist = externalAdminDir.absolutePath)) }

        val adminRes = client.get("/admin/")
        assertEquals(HttpStatusCode.OK, adminRes.status)
        assertEquals("external-admin", adminRes.bodyAsText())

        val spaRes = client.get("/admin/users")
        assertEquals(HttpStatusCode.OK, spaRes.status)
        assertEquals("external-admin", spaRes.bodyAsText())

        val fontRes = client.get("/fonts/NotoSansJP-Regular.ttf")
        assertEquals(HttpStatusCode.OK, fontRes.status)
        assertEquals("external-font", fontRes.bodyAsText())
    }

    // ────────────────────────────────────────────
    // 認証
    // ────────────────────────────────────────────

    @Test
    fun `login with wrong password returns 401`() = testApplication {
        application { module(testConfig) }
        val res = client.post("/internal/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"admin","password":"wrong"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `login with correct credentials returns 200`() = testApplication {
        application { module(testConfig) }
        val res = client.post("/internal/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"admin","password":"password"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun `login with malformed json returns json error`() = testApplication {
        application { module(testConfig) }
        val res = client.post("/internal/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":123}""")
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
        assertEquals(ContentType.Application.Json, res.contentType()?.withoutParameters())
        assertTrue(res.bodyAsText().contains("error"))
    }

    @Test
    fun `protected endpoint without session returns 401`() = testApplication {
        application { module(testConfig) }
        assertEquals(HttpStatusCode.Unauthorized, client.get("/internal/users").status)
        assertEquals(HttpStatusCode.Unauthorized, client.delete("/internal/users/nope").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/internal/urls").status)
        assertEquals(HttpStatusCode.Unauthorized, client.post("/internal/urls").status)
        assertEquals(HttpStatusCode.Unauthorized, client.post("/internal/slugs/generate").status)
    }

    @Test
    fun `logout clears session`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()
        assertEquals(HttpStatusCode.OK, c.get("/internal/urls").status)
        c.post("/internal/auth/logout")
        assertEquals(HttpStatusCode.Unauthorized, c.get("/internal/urls").status)
    }

    @Test
    fun `list users returns user summaries`() = testApplication {
        application { module(testConfig) }
        UserRepository.create("editor", AuthService.hashPassword("secret"))

        val c = jsonClient()
        c.login()

        val res = c.get("/internal/users")
        assertEquals(HttpStatusCode.OK, res.status)

        val body = res.bodyAsText()
        val users = ServerJson.decodeFromString(ListSerializer(UserSummary.serializer()), body)
        assertEquals(setOf("admin", "editor"), users.map { it.username }.toSet())
        assertFalse(body.contains("passwordHash"))
        assertFalse(body.contains("password_hash"))
    }

    @Test
    fun `delete user removes it from list`() = testApplication {
        application { module(testConfig) }
        val target = UserRepository.create("editor", AuthService.hashPassword("secret"))

        val c = jsonClient()
        c.login()

        val deleted = c.delete("/internal/users/${target.id}")
        assertEquals(HttpStatusCode.OK, deleted.status)
        assertFalse(deleted.body<DeleteUserResponse>().deletedCurrentUser)

        val users = c.get("/internal/users").userListBody()
        assertEquals(listOf("admin"), users.map { it.username })
    }

    @Test
    fun `delete current user clears session`() = testApplication {
        application { module(testConfig) }
        val admin = UserRepository.findByUsername("admin") ?: error("admin user not found")

        val c = jsonClient()
        c.login()

        val deleted = c.delete("/internal/users/${admin.id}")
        assertEquals(HttpStatusCode.OK, deleted.status)
        assertTrue(deleted.body<DeleteUserResponse>().deletedCurrentUser)
        assertEquals(HttpStatusCode.Unauthorized, c.get("/internal/urls").status)
    }

    // ────────────────────────────────────────────
    // ハッシュ生成（認証不要）
    // ────────────────────────────────────────────

    @Test
    fun `hash endpoint returns bcrypt hash`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        val res = c.post("/internal/auth/hash") {
            contentType(ContentType.Application.Json)
            setBody(HashRequest("mypassword"))
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val hash = res.body<HashResponse>().hash
        assertTrue(hash.startsWith("\$2a\$"), "bcryptハッシュが \$2a\$ で始まること")
    }

    // ────────────────────────────────────────────
    // URL 作成・一覧
    // ────────────────────────────────────────────

    @Test
    fun `create and list URLs`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        val created = c.post("/internal/urls") {
            contentType(ContentType.Application.Json)
            setBody(CreateUrlRequest("https://example.com", "test", false))
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val url = created.body<ShortenedUrl>()
        assertEquals("test", url.slug)
        assertEquals("https://example.com", url.originalUrl)
        assertEquals(0L, url.clickCount)
        assertFalse(url.isAutoGenerated)

        val list = c.get("/internal/urls").body<PagedResponse>()
        assertEquals(1L, list.total)
        assertEquals("test", list.items[0].slug)
    }

    @Test
    fun `create URL with duplicate slug returns 409`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        val req = CreateUrlRequest("https://example.com", "dup", false)
        c.post("/internal/urls") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        val res = c.post("/internal/urls") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        assertEquals(HttpStatusCode.Conflict, res.status)
    }

    @Test
    fun `create URL with invalid scheme returns 400`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        val res = c.post("/internal/urls") {
            contentType(ContentType.Application.Json)
            setBody(CreateUrlRequest("ftp://invalid.com", "ftp", false))
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `create URL with reserved slug returns 400`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        for (reserved in listOf("health", "admin/foo", "internal/bar")) {
            val res = c.post("/internal/urls") {
                contentType(ContentType.Application.Json)
                setBody(CreateUrlRequest("https://example.com", reserved, false))
            }
            assertEquals(HttpStatusCode.BadRequest, res.status, "/$reserved は予約済みなので400")
        }
    }

    // ────────────────────────────────────────────
    // URL 編集・削除
    // ────────────────────────────────────────────

    @Test
    fun `update URL changes originalUrl`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        val id = c.createUrl("https://old.com", "upd").id

        val updated = c.put("/internal/urls/$id") {
            contentType(ContentType.Application.Json)
            setBody(UpdateUrlRequest("https://new.com"))
        }
        assertEquals(HttpStatusCode.OK, updated.status)
        assertEquals("https://new.com", updated.body<ShortenedUrl>().originalUrl)
    }

    @Test
    fun `update nonexistent URL returns 404`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        val res = c.put("/internal/urls/no-such-id") {
            contentType(ContentType.Application.Json)
            setBody(UpdateUrlRequest("https://example.com"))
        }
        assertEquals(HttpStatusCode.NotFound, res.status)
    }

    @Test
    fun `delete URL removes it from list`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        val id = c.createUrl("https://example.com", "del").id
        assertEquals(HttpStatusCode.OK, c.delete("/internal/urls/$id").status)
        assertEquals(0L, c.get("/internal/urls").body<PagedResponse>().total)
    }

    @Test
    fun `delete nonexistent URL returns 404`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        assertEquals(HttpStatusCode.NotFound, c.delete("/internal/urls/no-such-id").status)
    }

    // ────────────────────────────────────────────
    // 検索
    // ────────────────────────────────────────────

    @Test
    fun `search filters by slug`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        c.createUrl("https://example.com", "abc")
        c.createUrl("https://example.com", "xyz")

        val res = c.get("/internal/urls?q=ab").body<PagedResponse>()
        assertEquals(1L, res.total)
        assertEquals("abc", res.items[0].slug)
    }

    @Test
    fun `search filters by original URL`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        c.createUrl("https://alpha.com", "s1")
        c.createUrl("https://beta.com", "s2")

        val res = c.get("/internal/urls?q=alpha").body<PagedResponse>()
        assertEquals(1L, res.total)
        assertEquals("s1", res.items[0].slug)
    }

    // ────────────────────────────────────────────
    // リダイレクト
    // ────────────────────────────────────────────

    @Test
    fun `redirect returns 302 with Location header`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()
        c.createUrl("https://example.com", "rdr")

        val raw = rawClient()
        val res = raw.get("/rdr")
        assertEquals(HttpStatusCode.Found, res.status)
        assertEquals("https://example.com", res.headers[HttpHeaders.Location])
    }

    @Test
    fun `redirect with URL-encoded slug works`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()
        c.createUrl("https://example.com", "日本語")

        val encoded = java.net.URLEncoder.encode("日本語", "UTF-8")
        val res = rawClient().get("/$encoded")
        assertEquals(HttpStatusCode.Found, res.status)
    }

    @Test
    fun `redirect to nonexistent slug returns 404`() = testApplication {
        application { module(testConfig) }
        val res = rawClient().get("/doesnotexist")
        assertEquals(HttpStatusCode.NotFound, res.status)
        assertEquals(ContentType.Text.Html, res.contentType()?.withoutParameters())
        assertTrue(res.bodyAsText().contains("Page Not Found"))
    }

    @Test
    fun `root returns 404 page`() = testApplication {
        application { module(testConfig) }
        val res = rawClient().get("/")
        assertEquals(HttpStatusCode.NotFound, res.status)
        assertEquals(ContentType.Text.Html, res.contentType()?.withoutParameters())
        assertTrue(res.bodyAsText().contains("Requested path: /"))
    }

    @Test
    fun `redirect increments click count`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()
        val created = c.createUrl("https://example.com", "clk")

        val raw = rawClient()
        repeat(3) { raw.get("/clk") }

        val got = c.get("/internal/urls/${created.id}").body<ShortenedUrl>()
        assertEquals(3L, got.clickCount)
    }

    // ────────────────────────────────────────────
    // スラッグ自動生成
    // ────────────────────────────────────────────

    @Test
    fun `generate ALPHANUMERIC slug has correct length`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        val res = c.post("/internal/slugs/generate") {
            contentType(ContentType.Application.Json)
            setBody(GenerateSlugRequest("ALPHANUMERIC", 5))
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val slug = res.body<GenerateSlugResponse>().slug
        assertEquals(5, slug.length)
        assertTrue(slug.all { it.isLetterOrDigit() })
    }

    @Test
    fun `generate DIGITS slug contains only digits`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        val slug = c.post("/internal/slugs/generate") {
            contentType(ContentType.Application.Json)
            setBody(GenerateSlugRequest("DIGITS", 4))
        }.body<GenerateSlugResponse>().slug

        assertEquals(4, slug.length)
        assertTrue(slug.all { it.isDigit() })
    }

    @Test
    fun `generate EMOJI slug has correct count`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        val res = c.post("/internal/slugs/generate") {
            contentType(ContentType.Application.Json)
            setBody(GenerateSlugRequest("EMOJI", 2))
        }
        assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun `generate with invalid type returns 400`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        val res = c.post("/internal/slugs/generate") {
            contentType(ContentType.Application.Json)
            setBody(GenerateSlugRequest("INVALID", 3))
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `generate with out-of-range length returns 400`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        val res = c.post("/internal/slugs/generate") {
            contentType(ContentType.Application.Json)
            setBody(GenerateSlugRequest("ALPHANUMERIC", 99))
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    // ────────────────────────────────────────────
    // アクセス解析
    // ────────────────────────────────────────────

    @Test
    fun `analytics returns correct click stats`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()
        val created = c.createUrl("https://example.com", "ana")

        val raw = rawClient()
        repeat(4) { raw.get("/ana") }

        val summary = c.get("/internal/urls/${created.id}/analytics").body<AnalyticsSummary>()
        assertEquals(4L, summary.totalClicks)
        assertTrue(summary.deviceTypes.containsKey("PC"))
        assertTrue(summary.browsers.isNotEmpty())
    }

    @Test
    fun `analytics for nonexistent URL returns 404`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()
        assertEquals(HttpStatusCode.NotFound, c.get("/internal/urls/no-such-id/analytics").status)
    }

    // ────────────────────────────────────────────
    // ページネーション
    // ────────────────────────────────────────────

    @Test
    fun `pagination offset and limit work`() = testApplication {
        application { module(testConfig) }
        val c = jsonClient()
        c.login()

        repeat(5) { i -> c.createUrl("https://example.com", "p$i") }

        val page1 = c.get("/internal/urls?offset=0&limit=3").body<PagedResponse>()
        assertEquals(5L, page1.total)
        assertEquals(3, page1.items.size)

        val page2 = c.get("/internal/urls?offset=3&limit=3").body<PagedResponse>()
        assertEquals(5L, page2.total)
        assertEquals(2, page2.items.size)
    }

    // ────────────────────────────────────────────
    // ヘルパー
    // ────────────────────────────────────────────

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(HttpCookies)
        install(ContentNegotiation) { json() }
    }

    /** リダイレクトを追わないクライアント */
    private fun ApplicationTestBuilder.rawClient() = createClient {
        followRedirects = false
    }

    private suspend fun io.ktor.client.HttpClient.login(
        username: String = "admin",
        password: String = "password",
    ) {
        val res = post("/internal/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"$password"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status, "ログイン失敗")
    }

    private suspend fun io.ktor.client.HttpClient.createUrl(
        originalUrl: String,
        slug: String,
        isAutoGenerated: Boolean = false,
    ): ShortenedUrl {
        val res = post("/internal/urls") {
            contentType(ContentType.Application.Json)
            setBody(CreateUrlRequest(originalUrl, slug, isAutoGenerated))
        }
        assertEquals(HttpStatusCode.Created, res.status, "URL作成失敗: ${res.body<String>()}")
        return res.body()
    }

    private suspend fun HttpResponse.userListBody(): List<UserSummary> =
        ServerJson.decodeFromString(ListSerializer(UserSummary.serializer()), bodyAsText())
}
