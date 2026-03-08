package com.shorturl.config

private val DEV_SECRET = "shorturl-dev-secret-key-32bytes!!"

data class AppConfig(
    /** バインドポート。環境変数: PORT（デフォルト: 8080） */
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080,
    /** バインドホスト。環境変数: HOST（デフォルト: 0.0.0.0） */
    val host: String = System.getenv("HOST") ?: "0.0.0.0",
    /** Xodus データディレクトリ。環境変数: DATA_DIR（デフォルト: ./data） */
    val dataDir: String = System.getenv("DATA_DIR") ?: "./.data",
    /** セッション署名キー（32文字以上推奨）。環境変数: SESSION_SECRET */
    val sessionSecret: String = System.getenv("SESSION_SECRET") ?: DEV_SECRET,
    /** MaxMind GeoLite2 Country mmdb パス。環境変数: GEOIP_MMDB */
    val geoipMmdb: String = System.getenv("GEOIP_MMDB") ?: "./GeoLite2-Country.mmdb",
    /** Compose Wasm ビルド出力ディレクトリ。環境変数: ADMIN_DIST（デフォルト: ./admin-dist） */
    val adminDist: String = System.getenv("ADMIN_DIST") ?: "./admin-dist",
    /** セッション Cookie に Secure 属性を付与（HTTPS 本番環境向け）。環境変数: COOKIE_SECURE */
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
