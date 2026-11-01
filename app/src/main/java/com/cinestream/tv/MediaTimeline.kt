package com.cinestream.tv

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.sp

internal fun scrubPosition(position: Long, duration: Long, direction: Int, repeats: Int): Long {
    if (duration <= 0) return 0
    val step = when { repeats >= 15 -> 60000L; repeats >= 5 -> 15000L; else -> 5000L }
    return (position.coerceIn(0, duration) + step * direction.coerceIn(-1, 1)).coerceIn(0, duration)
}
class MediaScrub {
    var target by mutableStateOf<Long?>(null)
    fun move(position: Long, duration: Long, direction: Int, repeats: Int) { if (duration > 0) target = scrubPosition(target ?: position, duration, direction, repeats) }
}
@Composable
fun MediaTimeline(position: Long, duration: Long, scrub: MediaScrub, requester: FocusRequester, onSeek: (Long) -> Unit) {
    if (duration <= 0) return
    val commit = { scrub.target?.let { onSeek(it.coerceIn(0, duration)) }; scrub.target = null }
    BackHandler(scrub.target != null) { scrub.target = null }
    Column {
        Slider(value = (scrub.target ?: position).coerceIn(0, duration).toFloat(),
            onValueChange = { scrub.target = it.toLong() }, valueRange = 0f..duration.toFloat(), onValueChangeFinished = commit,
            modifier = Modifier.fillMaxWidth().focusRequester(requester).onPreviewKeyEvent { e ->
                val code = e.nativeKeyEvent.keyCode
                if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (e.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) scrub.move(position, duration, if (code == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1, e.nativeKeyEvent.repeatCount)
                    true
                } else if ((code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) && scrub.target != null) {
                    if (e.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) commit()
                    true
                } else false
            }, colors = SliderDefaults.colors(thumbColor = androidx.compose.ui.graphics.Color.White, activeTrackColor = Accent))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(mediaClock(scrub.target ?: position), fontSize = 12.sp)
            if (scrub.target != null) Text("OK: seek · Back: cancel", color = Accent, fontSize = 12.sp)
            Text(mediaClock(duration), fontSize = 12.sp)
        }
    }
}
internal fun mediaClock(ms: Long): String {
    val s = ms.coerceAtLeast(0) / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%02d:%02d".format(s / 60, s % 60)
}
