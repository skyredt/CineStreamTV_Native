package com.cinestream.tv

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.UUID

class HongguoRegistrationTest {
    private val profile = HongguoRegistration.Profile("CineStream_Test_Phone35", "Android", "Android", 35, "15", "x86_64")
    private val cdid = UUID.randomUUID().toString()
    private val accepted = """{"device_id_str":"1234567890123456789","install_id_str":"9876543210987654321"}""".toByteArray()
    private class MemoryStore : HongguoRegistration.Store {
        var value: HongguoRegistration.Identity? = null
        var failWrite = false
        override fun read() = value
        override fun write(identity: HongguoRegistration.Identity) { if (failWrite) error("storage unavailable"); value = identity }
        override fun clear() { value = null }
    }

    @Test fun envelopeRoundTripRejectsCorruptionAndMalformedInput() {
        val plain = "红果 · independent client".toByteArray()
        val encrypted = HongguoRegistration.encrypt(plain)
        assertArrayEquals(plain, HongguoRegistration.decryptEnvelope(encrypted))
        encrypted[50] = (encrypted[50].toInt() xor 1).toByte()
        assertThrows(Exception::class.java) { HongguoRegistration.decryptEnvelope(encrypted) }
        assertThrows(Exception::class.java) { HongguoRegistration.decryptEnvelope(ByteArray(16)) }
    }

    @Test fun requestIdentifiesOurApplicationAndNeverContainsAccountCredentials() {
        val request = HongguoRegistration.request(profile, cdid, UUID.randomUUID().toString(), "0102030405060708", 1700000000000)
        assertTrue(request.url.startsWith("https://log.snssdk.com/service/2/device_register/?"))
        assertFalse(request.url.contains("com.phoenix.read"))
        val payload = JSONObject(String(HongguoRegistration.decryptEnvelope(request.body), Charsets.UTF_8)).getJSONObject("header")
        assertEquals("com.cinestream.tv", payload.getString("package"))
        assertEquals("1.3.2", payload.getString("app_version"))
        assertEquals(cdid, payload.getString("cdid"))
        assertEquals(52, request.headers.getValue("X-Gorgon").length)
        assertFalse(request.headers.keys.any { it.equals("Cookie", true) || it.equals("Authorization", true) })
        assertFalse(request.toString().contains(cdid))
    }

    @Test fun acceptanceRequiresBothValidIdentifiersAndNeverLogsThem() {
        val identity = HongguoRegistration.parse(accepted, cdid, 123)
        assertEquals("1234567890123456789", identity.deviceId)
        assertFalse(identity.toString().contains(identity.deviceId))
        assertFalse(identity.toString().contains(identity.installId))
        for (body in listOf("", "{}", "{\"device_id\":0,\"install_id\":12}", "{\"device_id\":12}", "invalid")) {
            val error = assertThrows(IOException::class.java) { HongguoRegistration.parse(body.toByteArray(), cdid, 123) }
            assertFalse(error.message.orEmpty().contains(body.takeIf { it.isNotEmpty() } ?: "credential"))
        }
    }

    @Test fun persistedIdentityReusedUntilExplicitInvalidation() {
        val store = MemoryStore()
        var calls = 0
        val engine = HongguoRegistration(store, profile, { calls++; HongguoRegistration.Response(200, accepted) })
        val first = engine.identity()
        assertSame(first, engine.identity())
        assertEquals(1, calls)
        engine.invalidate()
        assertNotEquals(first.cdid, engine.identity().cdid)
        assertEquals(2, calls)
    }

    @Test fun retriesAreBoundedAndRejectionCannotBecomeSuccess() {
        val store = MemoryStore()
        var calls = 0
        val engine = HongguoRegistration(store, profile, { calls++; throw IOException("sensitive upstream detail") })
        val error = assertThrows(IOException::class.java) { engine.identity() }
        assertEquals(2, calls)
        assertFalse(error.message.orEmpty().contains("sensitive"))
        assertNull(store.value)
        calls = 0
        val rejected = HongguoRegistration(store, profile, { calls++; HongguoRegistration.Response(403, accepted) })
        assertThrows(IOException::class.java) { rejected.identity() }
        assertEquals(1, calls)
        assertNull(store.value)
        val empty = HongguoRegistration(store, profile, { HongguoRegistration.Response(200, byteArrayOf()) })
        assertThrows(IOException::class.java) { empty.identity() }
    }

    @Test fun storageFailureAndCancellationNeverReportSuccessfulRetention() {
        val store = MemoryStore().apply { failWrite = true }
        val engine = HongguoRegistration(store, profile, { HongguoRegistration.Response(200, accepted) })
        assertThrows(IllegalStateException::class.java) { engine.identity() }
        assertNull(store.value)
        Thread.currentThread().interrupt()
        try { assertThrows(InterruptedException::class.java) { engine.identity() } }
        finally { Thread.interrupted() }
    }

    /** Explicit local opt-in; no network request in the ordinary unit suite. */
    @Test fun liveRegistrationWhenExplicitlyRequested() {
        org.junit.Assume.assumeTrue(System.getenv("CINEDBKTV_REGISTER_LIVE") == "1")
        val store = MemoryStore()
        val engine = HongguoRegistration(store, profile)
        val identity = engine.identity()
        assertSame(identity, store.value)
        assertSame(identity, engine.identity())
        println("HONGGUO_REGISTRATION_ACCEPTED: valid IDs returned; memory retention verified; no identifiers logged")
        engine.invalidate()
        assertNull(store.value)
    }
}
