package com.cinestream.tv

import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.UUID

class HongguoPlaybackMetadataTest {
    private val vid = "7695729320861387838"
    private val profile = HongguoRegistration.Profile("CineStream_Test_Phone35", "Android", "Android", 35, "15", "x86_64")
    private val id = HongguoRegistration.Identity("1234567890123456789", "9876543210987654321", UUID.randomUUID().toString(), 1)
    private fun payload(row: JSONObject = JSONObject().put("definition", "360p").put("codec_type", "bytevc1")
        .put("main_url", "https://media.example.test/video.mp4?signature=synthetic").put("spade_a", "synthetic-wrapped-metadata"), asString: Boolean = true): ByteArray {
        val model = JSONObject().put("video_duration", 140.714).put("video_list", JSONObject().put("video_1", row))
        return JSONObject().put("code", 0).put("data", JSONObject().put(vid, JSONObject().put("video_model", if (asString) model.toString() else model))).toString().toByteArray()
    }

    @Test fun directAndNestedProtectionAreDetectedWithoutReturningKeyMaterial() {
        for (nested in listOf(false, true)) {
            val row = JSONObject().put("definition", "360p").put("codec_type", "bytevc1").put("main_url", "https://media.example.test/video.mp4")
            if (nested) row.put("encrypt_info", JSONObject().put("spade_a", "synthetic-wrapped-metadata")) else row.put("spade_a", "synthetic-wrapped-metadata")
            val result = HongguoPlaybackMetadata.parse(payload(row, nested), vid)
            assertEquals(140.714, result.durationSeconds, 0.001)
            assertEquals(HongguoPlaybackMetadata.Protection.PROTECTED, result.options.single().protection)
            assertFalse(result.toString().contains("synthetic-wrapped"))
            assertFalse(result.options.single().toString().contains("https://"))
            assertThrows(IOException::class.java) { result.requirePlayable() }
        }
    }

    @Test fun missingProtectionFlagsNeverMeanVerifiedClearPlayback() {
        val row = JSONObject().put("definition", "360p").put("codec_type", "h264").put("main_url", "https://media.example.test/video.mp4").put("encrypt", false)
        val result = HongguoPlaybackMetadata.parse(payload(row), vid)
        assertEquals(HongguoPlaybackMetadata.Protection.UNVERIFIED, result.options.single().protection)
        assertThrows(IOException::class.java) { result.requirePlayable() }
    }

    @Test fun wrongEpisodeMalformedEmptyAndRejectedResponsesNeverProduceSampleSources() {
        val wrong = JSONObject(String(payload())).apply { getJSONObject("data").remove(vid) }.toString()
        for (value in listOf("", "{}", "invalid sensitive response", wrong, """{"code":403,"message":"private upstream detail"}""")) {
            val error = assertThrows(IOException::class.java) { HongguoPlaybackMetadata.parse(value.toByteArray(), vid) }
            assertFalse(error.message.orEmpty().contains("sensitive"))
            assertFalse(error.message.orEmpty().contains("private upstream detail"))
        }
    }

    @Test fun invalidUrlsAndUnsupportedCodecCannotReachPlayback() {
        for (address in listOf("http://media.example.test/video.mp4", "https://user:password@media.example.test/video.mp4", "file:///video.mp4", "https://media.example.test:8443/video.mp4")) {
            val row = JSONObject().put("main_url", address).put("codec_type", "h264")
            assertThrows(IOException::class.java) { HongguoPlaybackMetadata.parse(payload(row), vid) }
        }
        assertThrows(IOException::class.java) { HongguoPlaybackMetadata.parse(payload(JSONObject().put("main_url", "https://media.example.test/video.mp4").put("codec_type", "bytevc2")), vid) }
    }

    @Test fun requestUsesExactEpisodeAndOurProfileAndRedactsIdentity() {
        val request = HongguoPlaybackMetadata.request(vid, id, profile, 1700000000000)
        assertTrue(request.url.contains("package=com.cinestream.tv"))
        assertFalse(request.url.contains("com.phoenix.read"))
        assertEquals(vid, JSONObject(String(request.body)).getJSONObject("mixed_video_id_map").getJSONArray("1004").getString(0))
        assertFalse(request.toString().contains(id.deviceId))
        assertThrows(IllegalArgumentException::class.java) { HongguoPlaybackMetadata.request("0", id, profile, 1) }
    }

    @Test fun retriesEmptyResponseOnceWithoutChangingIdentityAndRejectsHttpErrors() = runBlocking {
        var calls = 0; var identities = 0
        val resolver = HongguoPlaybackMetadata({ identities++; id }, profile, { calls++; HongguoPlaybackMetadata.Response(200, if (calls == 1) byteArrayOf() else payload()) })
        assertEquals(vid, resolver.resolve(vid).episodeId)
        assertEquals(2, calls); assertEquals(1, identities)
        calls = 0
        val rejected = HongguoPlaybackMetadata({ id }, profile, { calls++; HongguoPlaybackMetadata.Response(403, payload()) })
        try { rejected.resolve(vid); fail("HTTP rejection must fail") } catch (_: IOException) { }
        assertEquals(1, calls)
        calls = 0
        val unavailable = HongguoPlaybackMetadata({ id }, profile, { calls++; throw IOException("sensitive transport detail") })
        try { unavailable.resolve(vid); fail("Network failure must fail") }
        catch (e: IOException) { assertFalse(e.message.orEmpty().contains("sensitive")) }
        assertEquals(2, calls)
    }

    @Test fun cancellationBeforeResolutionDoesNotRegisterOrFetch() = runBlocking {
        var calls = 0
        val resolver = HongguoPlaybackMetadata({ calls++; id }, profile, { calls++; HongguoPlaybackMetadata.Response(200, payload()) })
        val cancelled = Job().apply { cancel() }
        try { withContext(cancelled) { resolver.resolve(vid) }; fail("Cancellation must propagate") }
        catch (_: CancellationException) { }
        assertEquals(0, calls)
    }

    @Test fun liveMetadataOnlyWhenExplicitlyRequested() = runBlocking {
        org.junit.Assume.assumeTrue(System.getenv("CINEDBKTV_METADATA_LIVE") == "1")
        var saved: HongguoRegistration.Identity? = null
        val store = object : HongguoRegistration.Store {
            override fun read() = saved
            override fun write(identity: HongguoRegistration.Identity) { saved = identity }
            override fun clear() { saved = null }
        }
        val registration = HongguoRegistration(store, profile)
        val result = HongguoPlaybackMetadata(registration::identity, profile).resolve(vid)
        assertEquals(vid, result.episodeId)
        assertTrue(result.durationSeconds in 100.0..300.0)
        assertTrue(result.options.isNotEmpty())
        assertTrue(result.options.all { it.protection == HongguoPlaybackMetadata.Protection.PROTECTED })
        println("HONGGUO_METADATA_VERIFIED: requested episode, duration=${result.durationSeconds}, options=${result.options.size}; protected playback remains disabled")
        registration.invalidate()
    }
}
