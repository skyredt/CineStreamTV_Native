package com.cinestream.tv

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Anonymous registration only. Not connected to provider routing or protected media handling. */
internal class HongguoRegistration(
    private val store: Store,
    private val profile: Profile,
    private val transport: (Request) -> Response = ::send,
    private val now: () -> Long = System::currentTimeMillis,
) {
    interface Store { fun read(): Identity?; fun write(identity: Identity); fun clear() }
    class Identity(val deviceId: String, val installId: String, val cdid: String, val issuedAt: Long) {
        init { require(validId(deviceId) && validId(installId)); require(runCatching { UUID.fromString(cdid) }.isSuccess) }
        override fun toString() = "HongguoIdentity(redacted)"
    }
    data class Profile(val model: String, val brand: String, val manufacturer: String, val api: Int, val os: String, val abi: String)
    class Request(val url: String, val headers: Map<String, String>, val body: ByteArray) {
        override fun toString() = "HongguoRegistrationRequest(redacted)"
    }
    class Response(val status: Int, val body: ByteArray)

    /** Call off the main thread. At most two requests, and no retry after server rejection. */
    @Synchronized fun identity(): Identity {
        store.read()?.let { return it }
        val stamp = now()
        val cdid = UUID.randomUUID().toString()
        val client = UUID.randomUUID().toString()
        val open = hex(random(8))
        for (attempt in 0..1) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            val request = request(profile, cdid, client, open, stamp)
            val response = try { transport(request) } catch (e: IOException) {
                if (attempt == 0 && !Thread.currentThread().isInterrupted) continue
                throw IOException("Hongguo registration connection failed")
            }
            if (response.status != 200) throw IOException("Hongguo registration HTTP ${response.status}")
            val identity = parse(response.body, cdid, stamp)
            store.write(identity) // Never report persistence success if storage fails.
            return identity
        }
        throw IOException("Hongguo registration failed")
    }

    /** Upstream supplies no verified TTL. Invalidate on an explicit identity rejection, not a guessed expiry. */
    @Synchronized fun invalidate() = store.clear()

    companion object {
        private const val MAX_RESPONSE = 1024 * 1024
        private val rng = SecureRandom()
        internal fun validId(value: String) = value.matches(Regex("[1-9][0-9]{0,31}"))
        private fun random(size: Int) = ByteArray(size).also(rng::nextBytes)
        internal fun hex(value: ByteArray) = value.joinToString("") { "%02x".format(it.toInt() and 255) }
        private fun digest(type: String, value: ByteArray) = MessageDigest.getInstance(type).digest(value)
        private fun material(seed: ByteArray): ByteArray {
            val salt = "4dd4c2e6b83162090e52b3c7a6733ba41cb2462b829ab58a196b39db57177524f49baf7f08e8d68d26a72e37c1a95a2f1f05a51892aef2949732b62a38aadd58"
                .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            return digest("SHA-512", digest("SHA-512", seed) + salt)
        }
        internal fun encrypt(plain: ByteArray): ByteArray {
            val output = ByteArrayOutputStream()
            GZIPOutputStream(output).use { it.write(plain) }
            val zipped = output.toByteArray()
            byteArrayOf(31, -117, 8, 0, 0, 0, 0, 0, 0, 0).copyInto(zipped)
            val seed = random(32)
            val key = material(seed)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, 0, 16, "AES"), IvParameterSpec(key, 16, 16))
            return byteArrayOf(116, 99, 5, 16, 0, 0) + seed + cipher.doFinal(digest("SHA-512", zipped) + zipped)
        }
        internal fun decryptEnvelope(bytes: ByteArray): ByteArray {
            require(bytes.size in 118..MAX_RESPONSE && (bytes.size - 38) % 16 == 0) { "Invalid registration envelope" }
            require(bytes.take(6) == listOf<Byte>(116, 99, 5, 16, 0, 0)) { "Invalid registration envelope" }
            val key = material(bytes.copyOfRange(6, 38))
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, 0, 16, "AES"), IvParameterSpec(key, 16, 16))
            val plain = cipher.doFinal(bytes, 38, bytes.size - 38)
            require(plain.size >= 64) { "Invalid registration digest" }
            val zipped = plain.copyOfRange(64, plain.size)
            require(MessageDigest.isEqual(plain.copyOf(64), digest("SHA-512", zipped))) { "Invalid registration digest" }
            return GZIPInputStream(ByteArrayInputStream(zipped)).use(::limited)
        }
        internal fun sign(query: String, body: ByteArray, seconds: Int, nonce: Int): String {
            val buffer = ByteBuffer.allocate(20)
            buffer.put(digest("MD5", query.toByteArray(Charsets.UTF_8)), 0, 4)
            buffer.put(digest("MD5", body), 0, 4).putInt(0)
            buffer.order(ByteOrder.LITTLE_ENDIAN).putInt(67503104).order(ByteOrder.BIG_ENDIAN).putInt(seconds)
            val data = buffer.array()
            val schedule = intArrayOf(74, 64, 22, nonce ushr 8 and 255, 71, 108, 1, nonce and 255)
            val table = IntArray(256) { it }
            var cursor = 0
            for (index in table.indices) { cursor = (cursor + table[index] + schedule[index % 8]) and 255; table[index] = table[cursor] }
            cursor = 0
            for (index in data.indices) {
                cursor += table[index + 1]
                val value = table[cursor and 255]
                table[index + 1] = value
                data[index] = (data[index].toInt() xor table[value * 2 and 255]).toByte()
            }
            for (index in data.indices) {
                val value = data[index].toInt() and 255
                val rotated = ((value shl 4) or (value ushr 4)) and 255
                data[index] = rotated.toByte()
                val following = data[(index + 1) % 20].toInt() and 255
                data[index] = ((Integer.reverse(rotated xor following) ushr 24) xor 20).inv().toByte()
            }
            return hex(byteArrayOf(-124, 4, nonce.toByte(), (nonce ushr 8).toByte(), 64, 1) + data)
        }
        internal fun request(profile: Profile, cdid: String, client: String, open: String, stamp: Long): Request {
            val h = JSONObject().put("aid", 8662).put("package", "com.cinestream.tv").put("app_version", "1.3.2")
                .put("version_code", 9).put("manifest_version_code", 9).put("update_version_code", 9)
                .put("display_name", "CineDBKTV").put("device_platform", "android").put("os", "Android")
                .put("os_api", profile.api).put("os_version", profile.os).put("device_model", profile.model)
                .put("device_brand", profile.brand).put("device_manufacturer", profile.manufacturer).put("device_category", "tv")
                .put("cdid", cdid).put("clientudid", client).put("openudid", open).put("apk_first_install_time", stamp)
                .put("language", "zh").put("timezone", 8).put("tz_name", "Asia/Singapore").put("tz_offset", 28800)
                .put("access", "wifi").put("channel", "development").put("cpu_abi", profile.abi)
                .put("sdk_version", "3.7.2").put("sdk_target_version", 35).put("guest_mode", 1)
                .put("is_system_app", 0).put("not_request_sender", 0)
            val body = encrypt(JSONObject().put("magic_tag", "ss_app_log").put("header", h).put("_gen_time", stamp).toString().toByteArray(Charsets.UTF_8))
            val params = listOf("aid", "version_code", "manifest_version_code", "update_version_code", "device_platform", "os_api", "os_version", "language", "channel", "cdid", "openudid", "package")
                .associateWith { h.get(it).toString() }.toSortedMap()
            params.putAll(mapOf("app_name" to "CineDBKTV", "tt_data" to "a", "ts" to (stamp / 1000).toString(), "_rticket" to stamp.toString()))
            val query = params.entries.joinToString("&") { "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}" }
            return Request("https://log.snssdk.com/service/2/device_register/?$query", mapOf(
                "User-Agent" to "CineDBKTV/1.3.2 (Android)", "Content-Type" to "application/octet-stream;tt-data=a",
                "Content-Encoding" to "gzip", "Accept" to "application/json", "X-SS-DP" to "8662", "Sdk-Version" to "2",
                "X-Khronos" to (stamp / 1000).toString(), "X-SS-Req-Ticket" to stamp.toString(),
                "X-SS-STUB" to hex(digest("MD5", body)), "X-Gorgon" to sign(query, body, (stamp / 1000).toInt(), rng.nextInt(65536))), body)
        }
        internal fun parse(body: ByteArray, cdid: String, stamp: Long): Identity {
            try {
                require(body.isNotEmpty() && body.size <= MAX_RESPONSE)
                val data = if (body.take(2) == listOf<Byte>(116, 99)) decryptEnvelope(body) else body
                val json = JSONObject(String(data, Charsets.UTF_8))
                return Identity(json.optString("device_id_str").ifBlank { json.optString("device_id") },
                    json.optString("install_id_str").ifBlank { json.optString("install_id") }, cdid, stamp)
            } catch (e: InterruptedException) { throw e }
            catch (_: Exception) { throw IOException("Hongguo registration did not return valid identifiers") }
        }
        private fun limited(stream: InputStream): ByteArray {
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException()
                val count = stream.read(buffer)
                if (count < 0) break
                require(out.size() + count <= MAX_RESPONSE) { "Registration response too large" }
                out.write(buffer, 0, count)
            }
            return out.toByteArray()
        }
        private fun send(request: Request): Response {
            val connection = URL(request.url).openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15000; connection.readTimeout = 25000
                connection.requestMethod = "POST"; connection.doOutput = true
                request.headers.forEach(connection::setRequestProperty)
                connection.outputStream.use { it.write(request.body) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                return Response(status, stream?.use(::limited) ?: byteArrayOf())
            } finally { connection.disconnect() }
        }
        fun forApplication(context: Context): HongguoRegistration {
            val prefs = context.applicationContext.getSharedPreferences("hongguo_registration", Context.MODE_PRIVATE)
            val store = object : Store {
                override fun read(): Identity? = prefs.getString("identity", null)?.let {
                    try {
                        val json = JSONObject(it)
                        Identity(json.getString("deviceId"), json.getString("installId"), json.getString("cdid"), json.getLong("issuedAt"))
                    } catch (_: Exception) { clear(); null }
                }
                override fun write(identity: Identity) {
                    val json = JSONObject().put("deviceId", identity.deviceId).put("installId", identity.installId).put("cdid", identity.cdid).put("issuedAt", identity.issuedAt)
                    check(prefs.edit().putString("identity", json.toString()).commit()) { "Hongguo identity storage failed" }
                }
                override fun clear() { check(prefs.edit().remove("identity").commit()) { "Hongguo identity removal failed" } }
            }
            return HongguoRegistration(store, Profile(Build.MODEL, Build.BRAND, Build.MANUFACTURER, Build.VERSION.SDK_INT, Build.VERSION.RELEASE, Build.SUPPORTED_ABIS.first()))
        }
    }
}
