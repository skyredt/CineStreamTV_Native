package com.cinestream.tv

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.runBlocking

/** Opt-in emulator acceptance check. Resets only this test target's catalogue preferences. */
internal fun runCatalogueExperienceCheck(runner: Instrumentation) = with(runner) {
    val passed = mutableListOf<String>()
    val report = Bundle()
    fun nodes(): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo?) {
            if (node == null || result.size > 600) return
            result += node
            if (node.className?.toString() == "android.webkit.WebView") return
            for (index in 0 until node.childCount) visit(node.getChild(index))
        }
        visit(uiAutomation.rootInActiveWindow)
        return result
    }
    fun waitFor(label: String, seconds: Int = 45, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + seconds * 1000
        while (SystemClock.uptimeMillis() < deadline) { if (condition()) return; SystemClock.sleep(250) }
        error("Timed out: $label; visible=${nodes().mapNotNull { it.text }}")
    }
    fun launch(): Activity = startActivitySync(Intent().setClassName(targetContext.packageName, "${targetContext.packageName}.MainActivity")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    fun key(code: Int) { sendKeyDownUpSync(code); SystemClock.sleep(200) }
    try {
        targetContext.getSharedPreferences("catalogue", 0).edit().clear().commit()
        var activity = launch()
        waitFor("first-launch choices") { nodes().any { it.text?.contains("Choose your catalogue") == true } }
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        key(KeyEvent.KEYCODE_DPAD_UP)
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        waitFor("English home") { nodes().any { it.text?.toString() == "Details & Play" } }
        check(CataloguePreferences(targetContext).mode == CatalogueMode.ENGLISH)
        passed += "First launch; D-pad/OK selects English; live home renders"
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        waitFor("native details") { nodes().any { it.text?.toString()?.startsWith("Play ") == true } && nodes().none { it.text?.toString() == "Loading details and episodes…" } }
        key(KeyEvent.KEYCODE_BACK)
        waitFor("return to home") { nodes().any { it.text?.toString() == "Details & Play" } }
        check(nodes().any { it.isFocused })
        passed += "Poster/featured action opens native details; Back restores home focus"
        runOnMainSync { activity.finish() }
        activity = launch()
        waitFor("persisted English") { nodes().any { it.text?.toString() == "Details & Play" } }
        check(nodes().none { it.text?.contains("Choose your catalogue") == true })
        passed += "Catalogue preference persists across activity restart"
        // Actual sidebar traversal, with no pointer input.
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        var reached = false
        repeat(16) {
            if (!reached) {
                if (nodes().any { node -> node.isFocused && (node.text?.contains("English / 中文") == true || (0 until node.childCount).any { node.getChild(it)?.text?.contains("English / 中文") == true }) }) reached = true
                else key(KeyEvent.KEYCODE_DPAD_DOWN)
            }
        }
        check(reached) { "Catalogue switch unreachable with D-pad" }
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        waitFor("catalogue switch chooser") { nodes().any { it.text?.contains("Choose your catalogue") == true } }
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        waitFor("Chinese home") { CataloguePreferences(targetContext).mode == CatalogueMode.CHINESE && nodes().any { it.text?.toString() == "Details & Play" } }
        passed += "D-pad sidebar switches to Chinese without restart; DBKU home renders"
        val repository = ProvidersRepository()
        runBlocking {
            val english = repository.searchWithFilters("Lanterns", mode = CatalogueMode.ENGLISH)
            check(english.statuses.map { it.provider }.toSet() == setOf("cineby", "bingebang"))
            check(english.statuses.all { it.error == null })
            check(english.titles.any { it.provider == "cineby" } && english.titles.any { it.provider == "bingebang" })
            val chinese = repository.searchWithFilters("斗罗大陆", mode = CatalogueMode.CHINESE)
            check(chinese.titles.isNotEmpty() && chinese.titles.all { it.provider == "dbku" })
            passed += "Live isolated English and Chinese searches return real provider results"
            val detail = repository.details(liveTitle("dbku", "https://www.dbku.tv/voddetail/1951.html", "斗罗大陆", ""))
            check(detail.directors.isNotEmpty() && detail.cast.isNotEmpty() && detail.seasons.first().episodes.size == 40)
            passed += "Live DBKU reference details include cast, director and 40 episodes"
            val saved = CataloguePreferences(targetContext)
            saved.writeHistory(listOf(ViewingEntry(detail, detail.seasons.first().episodes[1].id, 123000, 1)))
            val read = CataloguePreferences(targetContext).readHistory().single()
            check(read.title.id == detail.id && read.position == 123000L && read.episodeId == detail.seasons.first().episodes[1].id)
            saved.writeHistory(emptyList())
            passed += "History round-trip preserves provider identity, exact episode and reported progress"
        }
        runOnMainSync { activity.finish() }
        report.putString("stream", passed.joinToString("\nPASS: ", "PASS: ") + "\n")
        finish(Activity.RESULT_OK, report)
    } catch (error: Throwable) {
        report.putString("stream", passed.joinToString("\nPASS: ", "PASS: ") + "\nFAIL: ${error.stackTraceToString()}\n")
        finish(Activity.RESULT_CANCELED, report)
    }
}
