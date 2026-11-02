package com.cinestream.tv

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Owns one installed child. Never discovers/reuses another application's local core. */
internal class HongguoCoreRuntime(
    private val root: File,
    private val version: String,
    private val launch: (instance: String, secret: String, directory: File) -> Process,
    private val startupTimeoutMs: Long = 8_000,
    private val bearerAuthentication: Boolean = false,
) {
    enum class State { STOPPED, STARTING, READY, FAILED }
    @Volatile var state = State.STOPPED
        private set
    private val mutex = Mutex()
    private var process: Process? = null
    private var directory: File? = null
    private var session: Session? = null
    private var logs: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    internal class Session internal constructor(
        val endpoint: HongguoCoreEndpoint,
        private val child: Process,
        private val secret: String,
    ) {
        fun requireAlive() { if (!child.isAlive) throw IOException("Hongguo local engine stopped") }
        internal fun headers(): Map<String, String> {
            requireAlive()
            return if (secret.isEmpty()) emptyMap() else mapOf("Authorization" to "Bearer $secret")
        }
        override fun toString() = "HongguoCoreSession(identity=redacted)"
    }

    /** Explicit restart after a crash; no retry loop or orphan process adoption. */
    suspend fun start(): Session = mutex.withLock {
        session?.takeIf { process?.isAlive == true }?.let { return@withLock it }
        cleanup()
        state = State.STARTING
        try {
            val instance = UUID.randomUUID().toString()
            val secret = if (bearerAuthentication) UUID.randomUUID().toString() + UUID.randomUUID().toString() else ""
            val work = File(root, instance).also { check(it.mkdirs()) }
            directory = work
            // Keep ownership even if cancellation arrives while ProcessBuilder.start is returning.
            val child = withContext(NonCancellable + Dispatchers.IO) {
                launch(instance, secret, work).also { process = it }
            }
            currentCoroutineContext().ensureActive()
            val ready = CompletableDeferred<HongguoCoreEndpoint>()
            logs = scope.launch {
                try {
                    child.inputStream.bufferedReader().use { reader ->
                        // Bound lines rather than readLine(): untrusted stdout cannot allocate indefinitely.
                        val line = StringBuilder()
                        var overflow = false
                        while (true) {
                            val c = reader.read()
                            if (c < 0) break
                            if (c == 10) {
                                if (!overflow) HongguoCoreEndpoint.fromReadyLine(line.toString().trimEnd('\r'), instance, version)
                                    ?.let { ready.complete(it) }
                                line.setLength(0); overflow = false
                            } else if (line.length < 256) line.append(c.toChar()) else overflow = true
                        }
                    }
                } catch (_: IOException) { /* Only sanitized state escapes. */ }
                finally {
                    ready.completeExceptionally(IOException("Hongguo local engine exited before readiness"))
                    scope.launch {
                        mutex.withLock {
                            if (process === child && state == State.READY) {
                                state = State.FAILED
                                cleanup()
                            }
                        }
                    }
                }
            }
            val endpoint = withTimeout(startupTimeoutMs) { ready.await() }
            val result = Session(endpoint, child, secret)
            if (bearerAuthentication) withTimeout(startupTimeoutMs) { runInterruptible(Dispatchers.IO) {
                for (headers in listOf(emptyMap(), mapOf("Authorization" to "Bearer invalid"))) {
                    if (HongguoCoreHttp.get(endpoint.baseUrl + "api/v1/status", headers).status !in listOf(401, 403))
                        throw IOException("Hongguo engine does not enforce private IPC")
                }
            } }
            withTimeout(startupTimeoutMs) {
                endpoint.verify { url -> HongguoCoreHttp.get(url, result.headers()) }
            }
            result.requireAlive()
            session = result
            state = State.READY
            result
        } catch (error: Throwable) {
            withContext(NonCancellable) { cleanup() }
            state = State.FAILED
            if (error is CancellationException) throw error
            throw IOException("Hongguo local engine startup failed")
        }
    }

    suspend fun stop() = mutex.withLock {
        withContext(NonCancellable) { cleanup() }
        state = State.STOPPED
    }

    private suspend fun cleanup() = withContext(Dispatchers.IO) {
        session = null
        val child = process
        process = null
        if (child != null) {
            child.destroy()
            if (!child.waitFor(750, TimeUnit.MILLISECONDS)) {
                child.destroyForcibly()
                check(child.waitFor(750, TimeUnit.MILLISECONDS)) { "Local engine did not stop" }
            }
            child.inputStream.close(); child.errorStream.close(); child.outputStream.close()
        }
        logs?.cancelAndJoin(); logs = null
        directory?.let { check(it.deleteRecursively()) { "Local engine cleanup failed" } }
        directory = null
    }

    companion object {
        /** Installed nativeLibraryDir only: Android 10+ forbids execution from writable app data. */
        fun packagedExecutable(nativeLibraryDir: File, expectedSha256: String): File {
            require(expectedSha256.matches(Regex("[0-9a-f]{64}")))
            val root = nativeLibraryDir.canonicalFile
            val binary = File(root, "libhongguotv.so").canonicalFile
            if (binary.parentFile != root || !binary.isFile || !binary.canExecute())
                throw IOException("Approved Hongguo engine is not installed for this ABI")
            val digest = MessageDigest.getInstance("SHA-256")
            binary.inputStream().use { input ->
                val bytes = ByteArray(8192)
                while (true) { val n = input.read(bytes); if (n < 0) break; digest.update(bytes, 0, n) }
            }
            if (digest.digest().joinToString("") { "%02x".format(it) } != expectedSha256)
                throw IOException("Hongguo engine integrity mismatch")
            return binary
        }
    }
}
