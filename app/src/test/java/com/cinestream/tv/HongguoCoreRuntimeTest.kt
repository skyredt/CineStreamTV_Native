package com.cinestream.tv

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

class HongguoCoreRuntimeTest {
    private val java = File(System.getProperty("java.home"), "bin/java.exe").let {
        if (it.exists()) it else File(System.getProperty("java.home"), "bin/java")
    }
    private suspend fun fixture(mode: String = "ok", timeout: Long = 8000, test: suspend (HongguoCoreRuntime, () -> Process) -> Unit) {
        val root = Files.createTempDirectory("cinedbktv-core-test").toFile()
        var child: Process? = null
        val runtime = HongguoCoreRuntime(root, "fixture-v1", { instance, secret, work ->
            ProcessBuilder(java.path, "-XX:+UseSerialGC", "-Xmx32m", "-cp",
                File(FixtureCore::class.java.protectionDomain!!.codeSource.location.toURI()).path,
                FixtureCore::class.java.name, instance, mode).directory(work).redirectErrorStream(true).apply {
                environment()["CINEDBKTV_CORE_TOKEN"] = secret
            }.start().also { child = it }
        }, timeout)
        try { test(runtime) { child!! } }
        finally {
            runtime.stop()
            assertFalse(child?.isAlive ?: false)
            assertTrue(root.listFiles().orEmpty().isEmpty())
            root.delete()
        }
    }

    @Test fun ownedChildAuthenticatesServesUnprotectedMediaAndRestartsAfterCrash() = runBlocking {
        fixture { runtime, child ->
            val session = runtime.start()
            assertSame(session, runtime.start())
            assertEquals(HongguoCoreRuntime.State.READY, runtime.state)
            assertEquals(403, HongguoCoreHttp.get(session.endpoint.baseUrl + "api/v1/status", emptyMap()).status)
            var released = 0
            val adapter = HongguoPreparedMedia(session, object : HongguoMediaPreparation {
                override suspend fun prepare(seriesId: String, episodeId: String, progress: (Int) -> Unit): HongguoMediaPreparation.Prepared {
                    progress(50); progress(100)
                    return HongguoMediaPreparation.Prepared(seriesId, episodeId, session.endpoint.baseUrl + "fixture.wav")
                }
                override suspend fun release() { released++ }
            })
            val progress = mutableListOf<Int>()
            val selected = adapter.select("123", "51") { progress += it }
            val response = HongguoCoreHttp.get(selected.address, adapter.playbackHeaders())
            assertEquals(200, response.status)
            assertEquals("RIFF", String(response.body.copyOfRange(0, 4)))
            assertEquals(16044, response.body.size)
            assertEquals(listOf(50, 100), progress)
            adapter.release(); assertEquals(1, released)
            child().destroyForcibly().waitFor()
            assertThrows(IOException::class.java) { session.requireAlive() }
            val restarted = runtime.start()
            assertNotSame(session, restarted)
            runtime.stop(); runtime.stop()
            assertEquals(HongguoCoreRuntime.State.STOPPED, runtime.state)
        }
    }

    @Test fun badVersionEarlyExitAndTimeoutReapChildAndPrivateFiles() = runBlocking {
        for (mode in listOf("wrong-version", "no-auth", "exit", "timeout")) fixture(mode, 2000) { runtime, _ ->
            try { runtime.start(); fail("Startup must fail: $mode") } catch (_: IOException) { }
            catch (_: TimeoutCancellationException) { }
            assertEquals(HongguoCoreRuntime.State.FAILED, runtime.state)
        }
    }

    @Test fun cancellationReapsChildAndOversizedLogIsDrained() = runBlocking {
        fixture("timeout") { runtime, child ->
            val job = launch { runtime.start() }
            withTimeout(2000) { while (runCatching { child() }.isFailure) delay(10) }
            job.cancelAndJoin()
            assertFalse(child().isAlive)
        }
        fixture("flood") { runtime, _ -> assertNotNull(runtime.start()) }
    }

    @Test fun preparationRejectsDifferentEpisodeAndForeignMediaOriginAndReleases() = runBlocking {
        fixture { runtime, _ ->
            val session = runtime.start()
            for ((episode, url) in listOf("52" to session.endpoint.baseUrl + "fixture.wav",
                "51" to "https://example.test/media.mp4", "51" to "http://127.0.0.1:1/media.mp4",
                "51" to session.endpoint.baseUrl.replace("127.0.0.1", "user@127.0.0.1") + "fixture.wav")) {
                var released = false
                val adapter = HongguoPreparedMedia(session, object : HongguoMediaPreparation {
                    override suspend fun prepare(seriesId: String, episodeId: String, progress: (Int) -> Unit) =
                        HongguoMediaPreparation.Prepared(seriesId, episode, url)
                    override suspend fun release() { released = true }
                })
                try { adapter.select("123", "51") {}; fail("Unsafe prepared source") } catch (_: IOException) { }
                assertTrue(released)
            }
        }
    }

    @Test fun missingPackagedExecutableFailsClosed() {
        val root = Files.createTempDirectory("core-absent").toFile()
        try { assertThrows(IOException::class.java) { HongguoCoreRuntime.packagedExecutable(root, "0".repeat(64)) } }
        finally { root.delete() }
    }
}
