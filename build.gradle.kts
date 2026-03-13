plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

/**
 * DB-IP Country Lite MMDB をダウンロードするタスク。
 * ライセンス: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0/)
 * 配布元: https://db-ip.com/db/download/ip-to-country-lite
 *
 * 使い方: ./gradlew downloadGeoIpDb
 * 出力先: プロジェクトルートの dbip-country-lite.mmdb
 */
tasks.register("downloadGeoIpDb") {
    description = "Downloads the latest DB-IP Country Lite MMDB (CC BY 4.0) to the project root"
    group = "setup"
    doLast {
        val cal = java.util.Calendar.getInstance()
        val year = cal.get(java.util.Calendar.YEAR)
        val month = String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1)
        val url = "https://download.db-ip.com/free/dbip-country-lite-$year-$month.mmdb.gz"
        val outputFile = rootProject.file("dbip-country-lite.mmdb")
        println("[GeoIP] Downloading: $url")
        val conn = java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection
        conn.instanceFollowRedirects = true
        java.util.zip.GZIPInputStream(conn.inputStream).use { gz ->
            outputFile.writeBytes(gz.readBytes())
        }
        println("[GeoIP] Saved to: ${outputFile.absolutePath}")
        println("[GeoIP] Attribution: This product includes IP to Country data created by DB-IP.com, available from https://db-ip.com (CC BY 4.0)")
    }
}
