package com.cinestream.tv

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.InputDevice
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import java.io.File

/** Deterministic layout/navigation checks using the production search and details composables. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
internal fun runSearchLayoutCheck(runner: Instrumentation) = with(runner) {
    val passed = mutableListOf<String>()
    val report = Bundle()
    fun nodes(): List<AccessibilityNodeInfo> {
        val found = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo?) {
            if (node == null) return
            found += node
            for (i in 0 until node.childCount) visit(node.getChild(i))
        }
        visit(uiAutomation.rootInActiveWindow)
        return found
    }
    fun waitFor(test: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15000
        while (SystemClock.uptimeMillis() < deadline) {
            if (test()) return
            SystemClock.sleep(100)
        }
        error("Timed out; visible: ${nodes().mapNotNull { it.text }}")
    }
    fun tag(value: String) = nodes().first { it.viewIdResourceName == value }
    fun bounds(value: String) = Rect().also { tag(value).getBoundsInScreen(it) }
    fun focusedTag(value: String) = nodes().any { it.viewIdResourceName == value && it.isFocused }
    fun key(value: Int) { sendKeyDownUpSync(value); SystemClock.sleep(250) }
    fun snapshot(name: String) {
        val file = File(targetContext.getExternalFilesDir(null), name)
        file.outputStream().use { uiAutomation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    try {
        val activity = startActivitySync(Intent().setClassName(targetContext.packageName, "${targetContext.packageName}.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        lateinit var state: StreamingState
        val filters = SearchFilters(mediaType = MediaTypeFilter.DRAMAS, genre = "古装", country = "大陆", year = "2026", sort = SortOption.YEAR)
        val titles = (1..6).map { index ->
            liveTitle(if (index % 2 == 0) "dbku" else "cineby", "https://example.com/tv/$index", "Layout title $index", "", year = 2026)
                .copy(seasons = listOf(DesignSeason(1, "Season 1", listOf(DesignEpisode("episode-$index", 1, 1, "Episode 1", "", "", 0, "")))))
        }
        runOnMainSync {
            state = StreamingState(ProvidersRepository())
            state.searchState.query = "Layout"
            state.searchState.filters = filters
            state.searchState.results = titles
            activity.setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    var detail by remember { mutableStateOf<DesignTitle?>(null) }
                    var exited by remember { mutableStateOf(false) }
                    BackHandler { if (detail != null) detail = null else exited = true }
                    Surface(Modifier.fillMaxSize()) {
                        DesignShell("search", 0, {}, {}) { compact ->
                            Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                                if (exited) Text("Exited search")
                                else if (detail != null) DesignDetails(detail!!, compact, { detail = null }, {})
                                else DesignSearch(titles, compact, state) { detail = it }
                            }
                        }
                    }
                }
            }
        }
        waitFor { focusedTag("search-filter-toggle") }
        val televisionLayout = nodes().any { it.viewIdResourceName == "search-sidebar" }
        val expandedWidth = if (televisionLayout) bounds("search-results").width() else 0
        if (televisionLayout) check(bounds("search-sidebar").right < bounds("search-results").left)
        snapshot("phase2-search-expanded.png")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        waitFor { nodes().any { it.text?.toString() == "Filters" } }
        check(focusedTag("search-filter-toggle"))
        if (televisionLayout) check(bounds("search-results").width() > expandedWidth)
        runOnMainSync { check(state.searchState.filters == filters); check(state.searchState.results == titles) }
        passed += "Collapse preserves filters/results and focus; ${if (televisionLayout) "results widen" else "compact panel"}"
        snapshot("phase2-search-collapsed.png")
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        waitFor { nodes().any { it.isFocused && !it.isEditable && it.viewIdResourceName != "search-filter-toggle" } }
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        waitFor { nodes().any { it.text?.toString() == "Play Episode 1" } }
        key(KeyEvent.KEYCODE_BACK)
        waitFor { nodes().any { it.viewIdResourceName == "search-filter-toggle" } }
        check(!focusedTag("search-filter-toggle"))
        runOnMainSync { check(state.searchState.focusedResultId == titles[1].id) }
        passed += "Remote Right/Center opens details; Back restores the selected second result"
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        waitFor { focusedTag("search-filter-toggle") }
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        waitFor { nodes().any { it.text?.toString() == "Hide filters" } }
        runOnMainSync { check(state.searchState.filters == filters); check(state.searchState.results == titles) }
        passed += "Remote reopens filters without changing the selection"
        key(KeyEvent.KEYCODE_DPAD_UP)
        waitFor { nodes().any { it.isEditable && it.isFocused } }
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        SystemClock.sleep(1000)
        key(KeyEvent.KEYCODE_BACK)
        check(nodes().none { it.text?.toString() == "Exited search" })
        passed += "Search input is reachable; first Back dismisses keyboard and retains search"
        fun tapToggle() {
            val rect = bounds("search-filter-toggle")
            val now = SystemClock.uptimeMillis()
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                val event = MotionEvent.obtain(now, SystemClock.uptimeMillis(), action, rect.exactCenterX(), rect.exactCenterY(), 0)
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                check(uiAutomation.injectInputEvent(event, true))
                event.recycle()
            }
            SystemClock.sleep(300)
        }
        tapToggle()
        waitFor { nodes().any { it.text?.toString() == "Filters" } }
        tapToggle()
        waitFor { nodes().any { it.text?.toString() == "Hide filters" } }
        runOnMainSync { check(state.searchState.filters == filters); check(state.searchState.results == titles) }
        passed += "Touch collapse/expand preserves filters and results"
        report.putString("stream", passed.joinToString("\nPASS: ", "PASS: ") + "\n")
        finish(Activity.RESULT_OK, report)
    } catch (error: Throwable) {
        report.putString("stream", passed.joinToString("\nPASS: ", "PASS: ") + "\nFAIL: ${error.stackTraceToString()}\n")
        finish(Activity.RESULT_CANCELED, report)
    }
}
