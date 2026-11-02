package com.cinestream.tv

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

val LocalTvInterface = staticCompositionLocalOf { true }

@Composable
fun AdaptiveApplication(state: StreamingState) {
    val configuration = LocalConfiguration.current
    val tv = usesTelevisionInterface(configuration.uiMode)
    val activity = LocalContext.current as? Activity
    val playing = state.streams.isNotEmpty()
    LaunchedEffect(tv, playing) {
        activity?.requestedOrientation = when {
            tv -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            playing -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
    }
    DisposableEffect(activity, tv, playing) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (tv || playing) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else controller?.show(WindowInsetsCompat.Type.systemBars())
        onDispose { }
    }
    CompositionLocalProvider(LocalTvInterface provides tv) { TvAppNavigation(state) }
}
