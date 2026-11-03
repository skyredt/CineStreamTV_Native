package com.cinestream.tv

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** Actual installed Go executable only. Never sends a valid media-preparation request. */
internal fun runHongguoRealCoreCheck(runner: Instrumentation) = with(runner) {
    val report = Bundle()
    val passed = mutableListOf<String>()
    val children = mutableListOf<Process>()
    var resultCode = Activity.RESULT_OK
    var runtime: HongguoCoreRuntime? = null
    try {
        runtime = HongguoCoreInstallation.runtime(targetContext) { children += it }
        runBlocking {
            val engine = runtime!!
            val first = engine.start()
            check(first.headers().isEmpty()) // Actual upstream does not implement our old proposed bearer contract.
            val status = HongguoCoreHttp.get(first.endpoint.baseUrl + "api/v1/status", emptyMap())
            check(status.status == 200)
            val json = JSONObject(String(status.body, Charsets.UTF_8))
            check(json.getString("version") == HongguoCoreInstallation.VERSION)
            check(json.getString("implementation") == "go-api-adapter+embedded-playback")
            check(json.getJSONArray("capabilities").toString().contains("play-info"))
            passed += "Real packaged Go core: SHA-256 verified; owned readiness/status; version=${json.getString("version")}; loopback HTTP 200"
            // Validation exits before any metadata resolution or protected preparation is dispatched.
            val invalid = HongguoCoreHttp.get(first.endpoint.baseUrl + "api/v1/play/info?quality=attempt1-invalid", emptyMap())
            check(invalid.status == 400)
            val wrongMethod = java.net.URL(first.endpoint.baseUrl + "api/v1/play/info").openConnection() as java.net.HttpURLConnection
            try {
                wrongMethod.requestMethod = "POST"; wrongMethod.connectTimeout = 2000; wrongMethod.readTimeout = 2000
                check(wrongMethod.responseCode == 405)
            } finally { wrongMethod.disconnect() }
            passed += "Real play/info interface: invalid quality rejected 400 and unsupported method rejected 405 before preparation"
            engine.stop()
            check(children.none { it.isAlive })
            passed += "Normal shutdown: real child exited"
            val second = engine.start()
            check(second !== first && children.size == 2 && children.last().isAlive)
            passed += "Restart: second real Go child passed readiness/status"
            children.last().destroyForcibly()
            check(children.last().waitFor(3, TimeUnit.SECONDS))
            withTimeout(5000) {
                while (engine.state != HongguoCoreRuntime.State.FAILED ||
                    File(targetContext.cacheDir, "hongguo-core").listFiles().orEmpty().isNotEmpty()) delay(50)
            }
            check(children.none { it.isAlive })
            passed += "Unexpected termination: FAILED observed; owned child reaped; transient files cleaned"
            engine.start(); check(children.size == 3 && children.last().isAlive)
            engine.stop(); check(children.none { it.isAlive })
            passed += "Recovery after crash: third real Go child started and stopped; all captured children dead"
        }
    } catch (error: Throwable) {
        resultCode = Activity.RESULT_CANCELED
        report.putString("error", error.stackTraceToString())
    } finally {
        runtime?.let { runBlocking { it.stop() } }
        children.forEach { if (it.isAlive) it.destroyForcibly() }
    }
    report.putString("stream", passed.joinToString("\nPASS: ", "PASS: ") + "\n" + report.getString("error", ""))
    finish(resultCode, report)
}
