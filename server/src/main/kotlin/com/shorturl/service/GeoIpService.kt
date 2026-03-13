package com.shorturl.service

import com.maxmind.db.Reader
import java.io.File
import java.net.InetAddress

object GeoIpService {
    private var reader: Reader? = null

    fun init(mmdbPath: String) {
        val file = File(mmdbPath)
        if (file.exists()) {
            reader = Reader(file)
        } else {
            println("[GeoIP] mmdbファイルが見つかりません: $mmdbPath (国情報は記録されません)")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun lookupCountry(ip: String): String? {
        val r = reader ?: return null
        return try {
            val addr = InetAddress.getByName(ip)
            val record: Map<*, *>? = r.get(addr, Map::class.java)
            val country = record?.get("country") as? Map<*, *> ?: return null
            country["iso_code"] as? String
        } catch (_: Exception) {
            null
        }
    }

    fun close() {
        reader?.close()
    }
}
