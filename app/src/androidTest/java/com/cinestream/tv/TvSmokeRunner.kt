package com.cinestream.tv

import android.app.Instrumentation
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo

/** Device smoke test using the actual TV UI, live providers, and Unicode accessibility input. */
class TvSmokeRunner : Instrumentation() {
    private val passed = mutableListOf<String>()
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val report = Bundle()
        try {
            startActivitySync(Intent().setClassName(targetContext.packageName, "${targetContext.packageName}.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            waitUntil { all().any { it.text?.toString() == "Watch Now" } }
            check(all().none { it.text?.toString() == "Minimize Remote" })
            passed += "Live TV home without floating D-pad"
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
            waitUntil { all().any { it.isFocused } }
            passed += "Physical D-pad changes focus"
            click("搜索 Search")
            setQuery("Lanterns")
            SystemClock.sleep(3000)
            scrollFind("Lanterns")
            click("Lanterns")
            waitUntil { all().any { it.text?.toString() == "Play S1:E1" } }
            scrollFind("Season 1 (8 eps)")
            passed += "Lanterns live search and eight-episode list"
            scrollTop()
            click("Play S1:E1")
            waitUntil { all().any { it.text?.toString()?.startsWith("Cineby ·") == true } }
            SystemClock.sleep(12000)
            check(all().none { it.text?.toString()?.contains("Embedded player failed") == true || it.text?.toString()?.contains("Embedded player returned") == true })
            passed += "Lanterns selected episode opens Cineby player"
            sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            waitUntil { all().any { it.text?.toString() == "Play S1:E1" } }
            sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            scrollTop()
            setQuery("尚公主")
            SystemClock.sleep(3000)
            scrollFind("尚公主")
            click("尚公主")
            waitUntil { all().any { it.text?.toString()?.startsWith("Play 第") == true } }
            scrollFind("第2集")
            passed += "Chinese live search and DBKU episode list"
            click("第2集")
            waitUntil { all().any { it.text?.toString()?.contains("尚公主 · 第2集") == true } }
            SystemClock.sleep(12000)
            check(all().none { it.text?.toString()?.contains("Playback failed") == true })
            sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_PAUSE)
            sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
            sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
            passed += "Selected DBKU episode opens player without immediate playback error"
            sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            waitUntil { all().any { it.text?.toString()?.startsWith("Play 第") == true } }
            passed += "Back returns to drama details"
            report.putString("stream", passed.joinToString("\nPASS: ", "PASS: ") + "\n")
            finish(Activity.RESULT_OK, report)
        } catch (e: Throwable) {
            report.putString("stream", passed.joinToString("\nPASS: ", "PASS: ") + "\nFAIL: ${e.stackTraceToString()}\nVISIBLE: ${all().mapNotNull { it.text?.toString() }}\n")
            finish(Activity.RESULT_CANCELED, report)
        }
    }
    private fun all(): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo?) { if (node == null) return; result += node; if (node.className?.toString() == "android.webkit.WebView" || result.size > 400) return; for (i in 0 until node.childCount) visit(node.getChild(i)) }
        visit(uiAutomation.rootInActiveWindow)
        return result
    }
    private fun waitUntil(test: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + 60000
        while (SystemClock.uptimeMillis() < end) { if (test()) return; SystemClock.sleep(500) }
        error("Timed out waiting for TV UI")
    }
    private fun click(label: String) {
        var node = all().firstOrNull { it.contentDescription?.toString() == label } ?: all().firstOrNull { !it.isEditable && it.text?.toString() == label } ?: error("Missing $label")
        while (!node.isClickable) node = node.parent ?: error("Not clickable: $label")
        check(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        SystemClock.sleep(500)
    }
    private fun setQuery(query: String) {
        waitUntil { all().any { it.isEditable } }
        sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
        val field = all().first { it.isEditable }
        check(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query) }))
        SystemClock.sleep(1500)
    }
    private fun scrollFind(label: String) {
        repeat(24) {
            if (all().any { !it.isEditable && (it.text?.toString() == label || it.contentDescription?.toString() == label) }) return
            all().firstOrNull { node -> val rect = android.graphics.Rect(); node.getBoundsInScreen(rect); node.isScrollable && rect.width() > 500 && rect.height() > 250 }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            SystemClock.sleep(400)
        }
        error("Could not scroll to $label")
    }
    private fun scrollTop() { repeat(16) { all().firstOrNull { node -> val rect = android.graphics.Rect(); node.getBoundsInScreen(rect); node.isScrollable && rect.width() > 500 && rect.height() > 250 }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD); SystemClock.sleep(150) } }
}
