package com.shorturl.config

import java.io.File

/**
 * @param port バインドポート
 * @param domain 実行ドメイン
 * @param dataDir SQLite データ配置先ディレクトリまたは DB ファイル
 * @param sessionSecret セッション署名キー（32文字以上推奨）
 * @param geoipMmdb DB-IP Country Lite mmdb パス（CC BY 4.0）
 * @param adminDist 管理画面の外部配信ディレクトリ。指定されると埋め込みリソースより優先
 * @param cookieSecure セッション Cookie に Secure 属性を付与（HTTPS 本番環境向け）
 */
data class AppConfig(
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080,
    val domain: String? = System.getenv("DOMAIN"),
    val dataDir: String = System.getenv("DATA_DIR") ?: "../.data",
    val sessionSecret: String = System.getenv("SESSION_SECRET")!!,
    val geoipMmdb: String = System.getenv("GEOIP_MMDB") ?: "./dbip-country-lite.mmdb",
    val adminDist: String? = System.getenv("ADMIN_DIST")?.takeIf { it.isNotBlank() },
    val cookieSecure: Boolean = System.getenv("COOKIE_SECURE")?.toBooleanStrictOrNull() ?: false,
) {
    val adminDistDir: File? get() = adminDist?.let(::File)

    fun printSummary() {
        println("=".repeat(50))
        println("ShortURL Server Configuration")
        println("=".repeat(50))
        println("  PORT          = $port")
        println("  HOST          = $domain")
        println("  DATA_DIR      = $dataDir")
        println("  GEOIP_MMDB    = $geoipMmdb")
        println("  ADMIN_DIST    = ${adminDist ?: "(embedded resources)"}")
        println("  COOKIE_SECURE = $cookieSecure")
        println("  SESSION_SECRET= ***")
        println("=".repeat(50))
    }
}
