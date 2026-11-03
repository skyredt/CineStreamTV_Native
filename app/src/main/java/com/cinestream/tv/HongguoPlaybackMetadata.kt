package com.cinestream.tv

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.Base64

/** Metadata only: no key values, DRM bypass, source fetching or provider-routing changes. */
internal class HongguoPlaybackMetadata(
    private val identity: () -> HongguoRegistration.Identity,
    private val profile: HongguoRegistration.Profile,
    private val transport: (Request) -> Response = ::send,
    private val now: () -> Long = System::currentTimeMillis,
) {
    class Request(val url: String, val body: ByteArray) {
        override fun toString() = "HongguoMetadataRequest(redacted)"
    }
    class Response(val status: Int, val body: ByteArray)
    enum class Protection { PROTECTED, UNVERIFIED }
    class Option(val quality: String, val codec: String, val mediaUrl: String, val protection: Protection) {
        override fun toString() = "HongguoOption(quality=$quality, codec=$codec, protection=$protection, url=redacted)"
    }
    class Result(val episodeId: String, val durationSeconds: Double, val options: List<Option>) {
        // Metadata is not evidence of decodable media, even when the encryption flag is absent/false.
        fun requirePlayable(): Nothing = throw IOException("Hongguo playback is not enabled: a supported playback authorization and media path must be verified")
        override fun toString() = "HongguoMetadata(episode=$episodeId, duration=$durationSeconds, options=${options.size})"
    }

    suspend fun resolve(episodeId: String): Result = runInterruptible(Dispatchers.IO) {
        require(HongguoRegistration.validId(episodeId)) { "Invalid Hongguo episode ID" }
        val registered = identity()
        for (attempt in 0..1) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            val response = try { transport(request(episodeId, registered, profile, now())) }
            catch (_: IOException) {
                if (attempt == 0 && !Thread.currentThread().isInterrupted) continue
                throw IOException("Hongguo playback metadata connection failed")
            }
            if (response.status != 200) throw IOException("Hongguo playback metadata HTTP ${response.status}")
            if (response.body.isEmpty() && attempt == 0) continue
            return@runInterruptible parse(response.body, episodeId)
        }
        throw IOException("Hongguo playback metadata was unavailable")
    }

    companion object {
        private const val MAX_RESPONSE = 2 * 1024 * 1024
        private const val ENDPOINT = "https://api5-normal-sinfonlineb.fqnovel.com/novel/player/multi_video_model/v1/"

        internal fun request(episode: String, id: HongguoRegistration.Identity, profile: HongguoRegistration.Profile, stamp: Long): Request {
            require(HongguoRegistration.validId(episode))
            val params = sortedMapOf(
                "aid" to "8662", "package" to "com.cinestream.tv", "app_name" to "CineDBKTV",
                "version_code" to "9", "manifest_version_code" to "9", "update_version_code" to "9",
                "channel" to "development", "device_platform" to "android", "os_api" to profile.api.toString(),
                "os_version" to profile.os, "language" to "zh", "cdid" to id.cdid, "device_id" to id.deviceId,
                "iid" to id.installId, "device_type" to profile.model, "host_abi" to profile.abi,
                "dragon_device_type" to "tv", "is_android_pad_screen" to "0", "_rticket" to stamp.toString(), "ts" to (stamp / 1000).toString(),
            )
            val query = params.entries.joinToString("&") { "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}" }
            val body = JSONObject().put("mixed_video_id_map", JSONObject().put("1004", JSONArray().put(episode)))
                .put("biz_param", JSONObject().put("need_all_video_definition", true).put("video_platform", 1024)
                    .put("use_os_player", true).put("need_mp4_align", false))
            return Request("$ENDPOINT?$query", body.toString().toByteArray(Charsets.UTF_8))
        }

        internal fun parse(bytes: ByteArray, episode: String): Result {
            require(HongguoRegistration.validId(episode))
            if (bytes.isEmpty() || bytes.size > MAX_RESPONSE) throw IOException("Hongguo playback metadata was empty or too large")
            try {
                val root = JSONObject(String(bytes, Charsets.UTF_8))
                if (root.optString("code", root.optString("Code")) != "0") throw IOException("Hongguo playback metadata request was rejected")
                // Require the requested episode association; a generic unrelated model is not a fallback.
                val entry = root.optJSONObject("data")?.optJSONObject(episode)
                    ?: throw IOException("Hongguo did not return the requested episode")
                val raw = entry.opt("video_model")
                val model = when (raw) {
                    is JSONObject -> raw
                    is String -> JSONObject(raw)
                    else -> throw IOException("Hongguo playback model was missing")
                }
                val duration = model.optDouble("video_duration", Double.NaN)
                if (!duration.isFinite() || duration <= 0) throw IOException("Hongguo playback duration was invalid")
                val value = model.opt("video_list")
                val rows = when (value) {
                    is JSONObject -> value.keys().asSequence().mapNotNull { value.optJSONObject(it) }.toList()
                    is JSONArray -> (0 until value.length()).mapNotNull { value.optJSONObject(it) }
                    else -> emptyList()
                }
                val options = rows.mapNotNull { row ->
                    val meta = row.optJSONObject("video_meta")
                    val codec = row.optString("codec_type").ifBlank { meta?.optString("codec_type").orEmpty() }
                    if (codec.equals("bytevc2", true)) return@mapNotNull null
                    val address = row.optString("main_url").ifBlank { row.optString("backup_url") }.ifBlank { row.optString("backup_url_1") }
                    if (address.isBlank()) return@mapNotNull null
                    val decoded = if (address.startsWith("https://")) address else {
                        try { String(Base64.getDecoder().decode(address), Charsets.UTF_8) }
                        catch (_: IllegalArgumentException) { return@mapNotNull null }
                    }
                    val uri = try { URI(decoded) } catch (_: Exception) { return@mapNotNull null }
                    if (uri.scheme != "https" || uri.host.isNullOrBlank() || uri.rawUserInfo != null || uri.rawFragment != null || uri.port !in listOf(-1, 443)) return@mapNotNull null
                    val nested = row.optJSONObject("encrypt_info")
                    // Inspect presence only. Never decode, return, persist or log protected key values.
                    val keyPresent = listOf("spade_a", "gear_des_key").any {
                        row.optString(it).isNotBlank() || nested?.optString(it).orEmpty().isNotBlank()
                    }
                    val isProtected = keyPresent || row.optBoolean("encrypt") || row.optString("encryption_method").isNotBlank()
                    Option(row.optString("definition").ifBlank { meta?.optString("definition").orEmpty() }, codec, decoded,
                        if (isProtected) Protection.PROTECTED else Protection.UNVERIFIED)
                }.distinctBy { it.mediaUrl }.sortedBy { Regex("\\d+").find(it.quality)?.value?.toIntOrNull() ?: Int.MAX_VALUE }
                if (options.isEmpty()) throw IOException("Hongguo returned no usable metadata options")
                return Result(episode, duration, options)
            } catch (e: IOException) { throw e }
            catch (_: Exception) { throw IOException("Hongguo playback metadata was invalid") }
        }

        private fun send(request: Request): Response {
            val connection = URL(request.url).openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15000; connection.readTimeout = 25000
                connection.requestMethod = "POST"; connection.doOutput = true
                connection.setRequestProperty("User-Agent", "CineDBKTV/1.3.2 (Android)")
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.outputStream.use { it.write(request.body) }
                val status = connection.responseCode
                if (status != 200) return Response(status, byteArrayOf())
                val out = ByteArrayOutputStream()
                connection.inputStream.use { stream ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        if (Thread.currentThread().isInterrupted) throw InterruptedException()
                        val count = stream.read(buffer)
                        if (count < 0) break
                        if (out.size() + count > MAX_RESPONSE) throw IOException("Hongguo metadata response too large")
                        out.write(buffer, 0, count)
                    }
                }
                return Response(status, out.toByteArray())
            } finally { connection.disconnect() }
        }

        fun forApplication(context: Context): HongguoPlaybackMetadata {
            val registration = HongguoRegistration.forApplication(context)
            val profile = HongguoRegistration.Profile(Build.MODEL, Build.BRAND, Build.MANUFACTURER, Build.VERSION.SDK_INT, Build.VERSION.RELEASE, Build.SUPPORTED_ABIS.first())
            return HongguoPlaybackMetadata(registration::identity, profile)
        }
    }
}
