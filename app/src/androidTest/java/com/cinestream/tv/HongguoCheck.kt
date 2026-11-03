package com.cinestream.tv

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.WebView
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import kotlinx.coroutines.runBlocking

/** Live catalog-only check: never opens a player or requests a playback source. */
internal fun runHongguoCheck(runner: Instrumentation) = with(runner) {
    val passed = mutableListOf<String>()
    val report = Bundle()
    try {
        val detail = runBlocking {
            val repository = ProvidersRepository()
            val results = repository.searchWithFilters("和离")
            check(results.statuses.single { it.provider == "hongguo" }.error == null)
            val selected = results.titles.first { it.provider == "hongguo" }
            check(selected.title.contains("和离"))
            passed += "Live Chinese unified search retains native titles and provider IDs"
            repository.details(selected)
        }
        check(detail.seasons.single().episodes.all { it.pageUrl.isBlank() && it.availability.contains("未知") })
        check(detail.seasons.single().episodes.size >= 90)
        passed += "Complete ordered episode IDs, unknown lock states and no playback URLs"
        val activity = startActivitySync(Intent().setClassName(targetContext.packageName, "${targetContext.packageName}.MainActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        var playbackRequests = 0
        runOnMainSync {
            activity.setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    Surface { DesignDetails(detail, false, {}, { playbackRequests++ }) }
                }
            }
        }
        fun nodes(): List<AccessibilityNodeInfo> {
            val result = mutableListOf<AccessibilityNodeInfo>()
            fun visit(node: AccessibilityNodeInfo?) {
                if (node == null) return
                result += node
                for (i in 0 until node.childCount) visit(node.getChild(i))
            }
            visit(uiAutomation.rootInActiveWindow)
            return result
        }
        fun find(text: String) = nodes().firstOrNull { it.text?.toString() == text }
        fun scrollFind(text: String) {
            repeat(120) {
                if (find(text) != null) return
                nodes().firstOrNull { node -> val r = Rect(); node.getBoundsInScreen(r); node.isScrollable && r.width() > 500 && r.height() > 200 }
                    ?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_DOWN)
                SystemClock.sleep(200)
            }
            error("Missing $text")
        }
        fun click(text: String) {
            var node = find(text) ?: error("Missing $text")
            while (!node.isClickable) node = node.parent ?: error("Not clickable $text")
            check(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            SystemClock.sleep(250)
        }
        SystemClock.sleep(1200)
        check(nodes().none { it.text?.toString()?.startsWith("Play ") == true })
        scrollFind("Go / 跳转")
        val field = nodes().first { it.isEditable }
        check(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "90")
        }))
        waitForIdleSync()
        SystemClock.sleep(300)
        click("Go / 跳转")
        scrollFind("第90集")
        click("第90集")
        runOnMainSync { check(playbackRequests == 0) }
        passed += "Details remain usable; range/jump reaches episode 90 without launching playback"
        report.putString("stream", passed.joinToString("\nPASS: ", "PASS: ") + "\n")
        finish(Activity.RESULT_OK, report)
    } catch (error: Throwable) {
        report.putString("stream", passed.joinToString("\nPASS: ", "PASS: ") + "\nFAIL: ${error.stackTraceToString()}\n")
        finish(Activity.RESULT_CANCELED, report)
    }
}
