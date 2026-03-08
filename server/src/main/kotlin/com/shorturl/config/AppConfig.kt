package com.shorturl.config

private val DEV_SECRET = "shorturl-dev-secret-key-32bytes!!"

/**
 * @param port バインドポート
 * @param host バインドホスト
 * @param dataDir Xodus データディレクトリ
 * @param sessionSecret セッション署名キー（32文字以上推奨）
 * @param geoipMmdb MaxMind GeoLite2 Country mmdb パス
 * @param adminDist Compose Wasm ビルド出力ディレクトリ
 * @param cookieSecure セッション Cookie に Secure 属性を付与（HTTPS 本番環境向け）
 */
data class AppConfig(
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080,
    val host: String = System.getenv("HOST") ?: "0.0.0.0",
    val dataDir: String = System.getenv("DATA_DIR") ?: "./.data",
    val sessionSecret: String = System.getenv("SESSION_SECRET") ?: DEV_SECRET,
    val geoipMmdb: String = System.getenv("GEOIP_MMDB") ?: "./GeoLite2-Country.mmdb",
    val adminDist: String = System.getenv("ADMIN_DIST") ?: "./admin-dist",
    val cookieSecure: Boolean = System.getenv("COOKIE_SECURE")?.toBooleanStrictOrNull() ?: false,
) {
    val isDevSecret: Boolean get() = sessionSecret == DEV_SECRET

    fun printSummary() {
        println("=".repeat(50))
        println("ShortURL Server Configuration")
        println("=".repeat(50))
        println("  PORT          = $port")
        println("  HOST          = $host")
        println("  DATA_DIR      = $dataDir")
        println("  ADMIN_DIST    = $adminDist")
        println("  GEOIP_MMDB    = $geoipMmdb")
        println("  COOKIE_SECURE = $cookieSecure")
        println("  SESSION_SECRET= ${if (isDevSecret) "⚠ 開発用デフォルト値（本番では SESSION_SECRET 環境変数を設定してください）" else "設定済み"}")
        println("=".repeat(50))
    }
}
