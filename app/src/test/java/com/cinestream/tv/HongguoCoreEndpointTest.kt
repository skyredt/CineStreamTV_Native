package com.cinestream.tv

import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.UUID

class HongguoCoreEndpointTest {
    private val instance = UUID.randomUUID().toString()
    private val version = "0.2.138-e1af6e0"
    private fun endpoint() = HongguoCoreEndpoint.fromReadyLine("HONGGUO_TV_READY 45678 $instance", instance, version)!!
    private fun status() = JSONObject().put("ok", true).put("service", "hongguo-tv-core").put("version", version).put("instance_id", instance)

    @Test fun readinessBindsOnlyMatchingChildAndLoopbackPort() {
        val endpoint = endpoint()
        assertEquals(45678, endpoint.port)
        assertEquals("http://127.0.0.1:45678/", endpoint.baseUrl)
        assertFalse(endpoint.toString().contains(instance))
        assertNull(HongguoCoreEndpoint.fromReadyLine("HONGGUO_TV_READY 45678 $instance", UUID.randomUUID().toString(), version))
    }

    @Test fun malformedReadinessCannotSelectAnotherHostOrInvalidPort() {
        for (port in listOf("0", "65536", "-1", "+1", "127.0.0.1:80", "https://example.test", "99999999")) {
            assertNull(HongguoCoreEndpoint.fromReadyLine("HONGGUO_TV_READY $port $instance", instance, version))
        }
        for (line in listOf("ready 45678 $instance", "HONGGUO_TV_READY 45678 $instance extra", "HONGGUO_TV_READY 45678 $instance\n", "HONGGUO_TV_READY 45678 not-an-instance")) {
            assertNull(HongguoCoreEndpoint.fromReadyLine(line, instance, version))
        }
        assertNull(HongguoCoreEndpoint.fromReadyLine("HONGGUO_TV_READY 45678 $instance", instance, ""))
    }

    @Test fun healthSuccessAloneIsNotEnoughToTrustEndpoint() {
        endpoint().requireMatchingStatus(status().toString().toByteArray())
        assertThrows(IOException::class.java) { endpoint().requireMatchingStatus("""{"ok":true}""".toByteArray()) }
        for ((field, value) in listOf("service" to "other-service", "version" to "other-version", "instance_id" to UUID.randomUUID().toString(), "ok" to false)) {
            assertThrows(IOException::class.java) { endpoint().requireMatchingStatus(status().put(field, value).toString().toByteArray()) }
        }
    }

    @Test fun malformedOversizedStatusAndSensitiveDetailsAreNotReturned() {
        for (bytes in listOf(byteArrayOf(), "invalid private session detail".toByteArray(), ByteArray(65537))) {
            val error = assertThrows(IOException::class.java) { endpoint().requireMatchingStatus(bytes) }
            assertFalse(error.message.orEmpty().contains("private session"))
            assertFalse(error.message.orEmpty().contains(instance))
        }
    }

    @Test fun probeUsesOnlyStatusAndRejectsRedirectOrFailedTransport() = runBlocking {
        var requests = 0
        endpoint().verify { address ->
            requests++
            assertEquals("http://127.0.0.1:45678/api/v1/status", address)
            HongguoCoreEndpoint.Response(200, status().toString().toByteArray())
        }
        assertEquals(1, requests)
        try { endpoint().verify { HongguoCoreEndpoint.Response(302, status().toString().toByteArray()) }; fail("Redirect must fail") }
        catch (_: IOException) { }
        try { endpoint().verify { throw IOException("private upstream detail") }; fail("Failure must propagate safely") }
        catch (e: IOException) { assertFalse(e.message.orEmpty().contains("private upstream")) }
    }

    @Test fun cancellationBeforeProbeDoesNotContactEngine() = runBlocking {
        var called = false
        val job = Job().apply { cancel() }
        try {
            withContext(job) { endpoint().verify { called = true; HongguoCoreEndpoint.Response(200, status().toString().toByteArray()) } }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
        assertFalse(called)
    }
}
