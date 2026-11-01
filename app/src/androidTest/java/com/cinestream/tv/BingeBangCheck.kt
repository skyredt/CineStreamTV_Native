package com.cinestream.tv

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Opt-in live provider/WebView check; fixture unit tests remain independent of the website. */
internal fun runBingeBangCheck(runner: Instrumentation) = with(runner) {
    val passed = mutableListOf<String>()
    val report = Bundle()
    try {
        val repository = ProvidersRepository()
        val selection = runBlocking {
            val home = repository.home()
            check(home.catalog.rows.any { it.id == "bingebang-movie" && it.titles.isNotEmpty() })
            check(home.catalog.rows.any { it.id == "bingebang-tv" && it.titles.isNotEmpty() })
            passed += "Live BingeBang movie and TV catalog rows"
            val search = repository.searchWithFilters("Lanterns")
            check(search.statuses.single { it.provider == "bingebang" }.error == null)
            val title = search.titles.single { it.provider == "bingebang" && it.title == "Lanterns" && it.isTv }
            val detail = repository.details(title)
            val episode = detail.seasons.single { it.number == 1 }.episodes.single { it.number == 2 }
            passed += "Live unified search, details and exact S01E02 selection"
            repository.sources(detail, episode).single()
        }
        val activity = startActivitySync(Intent().setClassName(targetContext.packageName, "${targetContext.packageName}.MainActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        var backRequests = 0
        runOnMainSync {
            activity.setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    EmbeddedPlayer(selection, "BingeBang live verification", onBack = { backRequests++ })
                }
            }
        }
        fun findWeb(view: View): WebView? {
            if (view is WebView) return view
            if (view is ViewGroup) for (i in 0 until view.childCount) findWeb(view.getChildAt(i))?.let { return it }
            return null
        }
        fun evaluate(expression: String): JSONObject {
            val latch = CountDownLatch(1)
            var result = JSONObject()
            runOnMainSync {
                val web = findWeb(activity.window.decorView)
                if (web == null) latch.countDown()
                else web.evaluateJavascript("JSON.stringify($expression)") { raw ->
                    runCatching { result = JSONObject(JSONTokener(raw).nextValue() as String) }
                    latch.countDown()
                }
            }
            check(latch.await(4, TimeUnit.SECONDS)) { "WebView evaluation timed out" }
            return result
        }
        fun waitFor(seconds: Int = 45, predicate: () -> Boolean) {
            val deadline = SystemClock.uptimeMillis() + seconds * 1000
            while (SystemClock.uptimeMillis() < deadline) {
                if (predicate()) return
                SystemClock.sleep(300)
            }
            error("Timed out waiting for the live BingeBang player")
        }
        val state = "({player:window.cinedbkPlayer?cinedbkPlayer():{},servers:window.BB_PLAYER?.engine.servers.length||0,season:window.__BB_PLAYER__?.season,episode:window.__BB_PLAYER__?.episode})"
        waitFor(60) { evaluate(state).optJSONObject("player")?.optBoolean("playing") == true }
        val initial = evaluate(state)
        check(initial.getInt("season") == 1 && initial.getInt("episode") == 2)
        check(initial.getInt("servers") > 1)
        val before = initial.getJSONObject("player").getDouble("position")
        waitFor(30) { evaluate(state).getJSONObject("player").getDouble("position") > before + 1 }
        passed += "Actual Android WebView episode playback advances; multiple published servers"
        fun key(code: Int) { sendKeyDownUpSync(code); SystemClock.sleep(150) }
        fun serverMenu() {
            repeat(18) {
                key(KeyEvent.KEYCODE_DPAD_RIGHT)
                if (evaluate("({label:document.querySelector('.cinedbk-selected')?.getAttribute('aria-label')})").optString("label") == "Server") {
                    key(KeyEvent.KEYCODE_DPAD_CENTER)
                    waitFor(5) { evaluate("({open:document.querySelector('[data-gate]')?.hidden===false})").optBoolean("open") }
                    return
                }
            }
            error("Remote could not reach Server")
        }
        serverMenu()
        var target = -1
        repeat(12) {
            if (target != 1) {
                key(KeyEvent.KEYCODE_DPAD_DOWN)
                target = evaluate("({index:document.querySelector('.cinedbk-selected')?.getAttribute('data-v')})").optString("index").toIntOrNull() ?: -1
            }
        }
        check(target == 1) { "Remote could not select the second server" }
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        waitFor(30) { evaluate("({index:window.BB_PLAYER?.engine.serverIndex,playing:window.cinedbkPlayer&&cinedbkPlayer().playing})").let { it.optInt("index", -1) == 1 && it.optBoolean("playing") } }
        passed += "D-pad/Center selects the second streaming server and playback continues"
        serverMenu()
        key(KeyEvent.KEYCODE_BACK)
        waitFor(5) { evaluate("({closed:document.querySelector('[data-gate]')?.hidden===true})").optBoolean("closed") }
        runOnMainSync { check(backRequests == 0) }
        key(KeyEvent.KEYCODE_BACK)
        runOnMainSync { check(backRequests == 1) }
        passed += "Back closes server settings before returning to native details"
        runOnMainSync { findWeb(activity.window.decorView)?.evaluateJavascript("window.cinedbkPause&&cinedbkPause()", null) }
        report.putString("stream", passed.joinToString("\nPASS: ", "PASS: ") + "\n")
        finish(Activity.RESULT_OK, report)
    } catch (error: Throwable) {
        report.putString("stream", passed.joinToString("\nPASS: ", "PASS: ") + "\nFAIL: ${error.stackTraceToString()}\n")
        finish(Activity.RESULT_CANCELED, report)
    }
}
