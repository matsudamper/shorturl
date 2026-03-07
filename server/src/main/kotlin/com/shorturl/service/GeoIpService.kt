package com.shorturl.service

import com.maxmind.geoip2.DatabaseReader
import java.io.File
import java.net.InetAddress

object GeoIpService {
    private var reader: DatabaseReader? = null

    fun init(mmdbPath: String) {
        val file = File(mmdbPath)
        if (file.exists()) {
            reader = DatabaseReader.Builder(file).build()
        } else {
            println("[GeoIP] mmdbファイルが見つかりません: $mmdbPath (国情報は記録されません)")
        }
    }

    fun lookupCountry(ip: String): String? {
        val r = reader ?: return null
        return try {
            val addr = InetAddress.getByName(ip)
            r.country(addr)?.country?.isoCode
        } catch (_: Exception) {
            null
        }
    }

    fun close() {
        reader?.close()
    }
}
