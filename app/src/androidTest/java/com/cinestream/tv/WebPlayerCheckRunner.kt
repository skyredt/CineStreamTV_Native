package com.cinestream.tv

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme

/** Loads a user-selected real provider URL into the production web-player component for device inspection. */
internal fun runWebPlayerCheck(runner: Instrumentation, mediaUrl: String) = with(runner) {
    require(android.net.Uri.parse(mediaUrl).let { it.scheme == "https" && it.host == "play.xpass.top" })
        val activity = startActivitySync(Intent().setClassName(targetContext.packageName, "${targetContext.packageName}.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        runOnMainSync {
            activity.setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    EmbeddedPlayer(ProviderStream("Published provider source", mediaUrl, true, ProvidersRepository.CINEBY + "/"), "Player verification", onBack = {})
                }
            }
        }
        sendStatus(0, Bundle().apply { putString("stream", "Production web-player component loaded for inspection.\n") })
        SystemClock.sleep(240000)
        finish(Activity.RESULT_OK, Bundle())
}
