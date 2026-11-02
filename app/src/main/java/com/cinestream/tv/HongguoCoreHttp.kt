package com.cinestream.tv

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Bounded loopback-only transport. Upstream status has no bearer-authentication contract. */
internal object HongguoCoreHttp {
    fun get(address: String, headers: Map<String, String>): HongguoCoreEndpoint.Response {
        val url = URL(address)
        require(url.protocol == "http" && url.host == "127.0.0.1" && url.port in 1..65535 && url.userInfo == null)
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 2000; connection.readTimeout = 2000
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            val status = connection.responseCode
            if (status != 200) return HongguoCoreEndpoint.Response(status, byteArrayOf())
            val output = ByteArrayOutputStream()
            connection.inputStream.use { stream ->
                val bytes = ByteArray(4096)
                while (true) {
                    if (Thread.currentThread().isInterrupted) throw InterruptedException()
                    val count = stream.read(bytes)
                    if (count < 0) break
                    if (output.size() + count > 65536) throw IOException("Local engine response too large")
                    output.write(bytes, 0, count)
                }
            }
            return HongguoCoreEndpoint.Response(status, output.toByteArray())
        } finally { connection.disconnect() }
    }
}
