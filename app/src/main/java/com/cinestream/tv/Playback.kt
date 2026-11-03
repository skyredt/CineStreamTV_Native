package com.cinestream.tv

import android.view.KeyEvent
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(url: String, title: String, headers: Map<String, String> = emptyMap(), initialPosition: Long = 0, onPosition: (Long) -> Unit = {}, onFailure: (String) -> Boolean = { false }, onBack: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var error by remember(url) { mutableStateOf<String?>(null) }
    var buffering by remember(url) { mutableStateOf(true) }
    var tracks by remember(url) { mutableStateOf(Tracks.EMPTY) }
    var picker by remember { mutableStateOf<Int?>(null) }
    var diagnostics by remember { mutableStateOf(false) }
    var savedPosition by rememberSaveable(url) { mutableLongStateOf(initialPosition) }
    var position by remember(url) { mutableLongStateOf(savedPosition) }
    var duration by remember(url) { mutableLongStateOf(0L) }
    var buffered by remember(url) { mutableIntStateOf(0) }
    var playing by remember(url) { mutableStateOf(true) }
    var muted by remember(url) { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    var interaction by remember { mutableLongStateOf(0L) }
    val scrub = remember(url) { MediaScrub() }
    val timelineFocus = remember { FocusRequester() }
    val scrubbing = scrub.target != null
    val rootFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val player = remember(url) {
        val data = androidx.media3.datasource.DefaultHttpDataSource.Factory().setUserAgent(ProvidersRepository.AGENT).setDefaultRequestProperties(headers).setConnectTimeoutMs(15000).setReadTimeoutMs(15000)
        val media = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context).setDataSourceFactory(data)
        ExoPlayer.Builder(context).setMediaSourceFactory(media).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            setHandleAudioBecomingNoisy(true)
            setMediaItem(MediaItem.fromUri(url)); seekTo(savedPosition); prepare(); playWhenReady = true
        }
    }
    LaunchedEffect(error) { error?.let { if (onFailure(it)) error = null } }
    DisposableEffect(player, owner) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) { buffering = state == Player.STATE_BUFFERING }
            override fun onTracksChanged(value: Tracks) { tracks = value }
            override fun onIsPlayingChanged(value: Boolean) { playing = value }
            override fun onPlayerError(value: PlaybackException) { buffering = false; error = "Playback failed: ${value.errorCodeName}. Check the URL, network and supported media format."; controls = true }
        }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) { savedPosition = player.currentPosition; player.pause() } }
        player.addListener(listener); owner.lifecycle.addObserver(observer)
        onDispose { savedPosition = player.currentPosition; owner.lifecycle.removeObserver(observer); player.removeListener(listener); player.release() }
    }
    LaunchedEffect(player) {
        while (true) {
            if (!scrubbing) position = player.currentPosition.coerceAtLeast(0L)
            savedPosition = player.currentPosition
            onPosition(savedPosition)
            duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
            buffered = player.bufferedPercentage
            playing = player.isPlaying
            delay(500)
        }
    }
    LaunchedEffect(controls, scrubbing) { withFrameNanos { }; if (scrubbing && controls && duration > 0) timelineFocus.requestFocus() else if (controls) playFocus.requestFocus() else rootFocus.requestFocus() }
    LaunchedEffect(playing, controls, interaction, picker, error, scrubbing) {
        if (playing && controls && picker == null && error == null && !scrubbing) { delay(5000); controls = false }
    }
    val interact = { interaction++; controls = true }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().focusRequester(rootFocus).onPreviewKeyEvent { event ->
        if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) false else {
            when (event.nativeKeyEvent.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { if (player.isPlaying) player.pause() else player.play(); interact(); true }
                KeyEvent.KEYCODE_MEDIA_PLAY -> { player.play(); interact(); true }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> { player.pause(); interact(); true }
                KeyEvent.KEYCODE_MEDIA_REWIND -> { scrub.move(position, duration, -1, event.nativeKeyEvent.repeatCount); interact(); true }
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> { scrub.move(position, duration, 1, event.nativeKeyEvent.repeatCount); interact(); true }
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { val hidden = !controls
                    if (hidden && event.nativeKeyEvent.keyCode in listOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT)) scrub.move(position, duration, if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1, event.nativeKeyEvent.repeatCount)
                    interact(); hidden }
                else -> false
            }
        }
    }.focusable()) {
        val compact = maxWidth < 600.dp
        AndroidView(factory = { ctx -> PlayerView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(-1, -1); this.player = player; useController = false; isFocusable = false; keepScreenOn = true
        } }, update = { it.player = player }, onRelease = { it.player = null }, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().clickable(onClickLabel = "Show playback controls") { interact() })
        if (buffering) CircularProgressIndicator(Modifier.align(Alignment.Center).size(40.dp), color = Accent)
        if (controls || !playing) {
            Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .8f), Color.Transparent))).padding(if (compact) 16.dp else 32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RoundControl("Back", Icons.Outlined.ArrowBack, onClick = onBack)
                Column(Modifier.weight(1f)) { Text(title, fontSize = if (compact) 16.sp else 20.sp, fontWeight = FontWeight.Black, maxLines = 2); Text(if (title.contains("sample", true)) "Explicit sample video" else if (headers.isNotEmpty()) "DBKU" else "User-provided media", color = Muted, fontSize = 11.sp) }
                if (!compact) Action(android.net.Uri.parse(url).host.orEmpty(), icon = Icons.Outlined.Dns, dense = true) { diagnostics = !diagnostics; interact() }
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .7f), Color.Black.copy(alpha = .95f)))).padding(if (compact) 16.dp else 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (duration > 0) {
                    MediaTimeline(position, duration, scrub, timelineFocus) { player.seekTo(it); interact() }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundControl("Seek backward", Icons.Outlined.FastRewind) { scrub.move(position, duration, -1, 0); interact(); timelineFocus.requestFocus() }
                    RoundControl(if (playing) "Pause" else "Play", if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, primary = true, requester = playFocus) { if (player.isPlaying) player.pause() else player.play(); interact() }
                    RoundControl("Seek forward", Icons.Outlined.FastForward) { scrub.move(position, duration, 1, 0); interact(); timelineFocus.requestFocus() }
                    if (!compact) Spacer(Modifier.width(40.dp))
                    listOf(C.TRACK_TYPE_TEXT to "Subtitles", C.TRACK_TYPE_VIDEO to "Quality", C.TRACK_TYPE_AUDIO to "Audio").forEach { (type, label) ->
                        val count = tracks.groups.filter { it.type == type }.sumOf { group -> (0 until group.length).count { group.isTrackSupported(it) } }
                        if ((type == C.TRACK_TYPE_TEXT && count > 0) || count > 1) Action(label, dense = true, icon = if (type == C.TRACK_TYPE_TEXT) Icons.Outlined.Subtitles else if (type == C.TRACK_TYPE_VIDEO) Icons.Outlined.Tune else Icons.Outlined.Audiotrack) { picker = type; interact() }
                    }
                    RoundControl(if (muted) "Unmute" else "Mute", if (muted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp) { muted = !muted; player.volume = if (muted) 0f else 1f; interact() }
                }
            }
        }
        error?.let { message ->
            Surface(Modifier.align(Alignment.Center).padding(32.dp).widthIn(max = 520.dp), color = Color(0xF2111111), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Accent.copy(alpha = .4f))) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { Text(message, color = Color.White); Action("Retry selected URL", primary = true) { error = null; buffering = true; player.prepare(); player.play() }; Action("Back", onClick = onBack) }
            }
        }
        if (diagnostics) Surface(Modifier.align(Alignment.TopEnd).padding(top = 100.dp, end = 32.dp).width(280.dp), shape = RoundedCornerShape(16.dp), color = Color(0xF20A0A0A), border = BorderStroke(1.dp, Accent.copy(alpha = .4f))) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("STREAM DIAGNOSTICS", color = Accent, fontWeight = FontWeight.Bold); Text("Host: ${android.net.Uri.parse(url).host}", fontSize = 12.sp); Text("Buffered: $buffered%", fontSize = 12.sp); Text("Position: ${clock(position)}", fontSize = 12.sp); Text(if (buffering) "Buffering" else if (playing) "Playing" else "Paused", fontSize = 12.sp); Action("Close", dense = true) { diagnostics = false } }
        }
        picker?.let { type ->
            AlertDialog(onDismissRequest = { picker = null; interact() }, title = { Text(if (type == C.TRACK_TYPE_VIDEO) "Video Quality / Resolution" else if (type == C.TRACK_TYPE_AUDIO) "Audio Tracks" else "Subtitles", color = Accent) }, containerColor = Color(0xFF0B0B0B), confirmButton = { Action("Close", dense = true) { picker = null; interact() } }, text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Action(if (type == C.TRACK_TYPE_TEXT) "Off" else "Auto / default", dense = true) { player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().clearOverridesOfType(type).setTrackTypeDisabled(type, type == C.TRACK_TYPE_TEXT).build(); picker = null; interact() } }
                    tracks.groups.filter { it.type == type }.forEach { group ->
                        (0 until group.length).filter { group.isTrackSupported(it) }.forEach { index ->
                            val format = group.getTrackFormat(index)
                            val label = if (type == C.TRACK_TYPE_VIDEO) "${if (format.height > 0) "${format.height}p" else "Video ${index + 1}"}${format.bitrate.takeIf { it > 0 }?.let { " · ${it / 1000} kbps" }.orEmpty()}" else "${format.label ?: format.language ?: "Track ${index + 1}"}"
                            item { Action((if (group.isTrackSelected(index)) "✓ " else "") + label, dense = true) { player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(type, false).setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, index)).build(); picker = null; interact() } }
                        }
                    }
                }
            })
        }
    }
}

@Composable
private fun RoundControl(label: String, icon: ImageVector, primary: Boolean = false, requester: FocusRequester? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Button(onClick, Modifier.size(if (primary) 56.dp else 44.dp).then(if (requester != null) Modifier.focusRequester(requester) else Modifier).onFocusChanged { focused = it.isFocused }, shape = CircleShape, contentPadding = PaddingValues(10.dp), border = BorderStroke(if (focused) 3.dp else 0.dp, if (focused) Accent else Color.Transparent), colors = ButtonDefaults.buttonColors(containerColor = if (focused) Color.White else if (primary) Accent else Color.Black.copy(alpha = .3f), contentColor = if (focused || primary) Color.Black else Color.White)) { Icon(icon, label, Modifier.size(if (primary) 28.dp else 24.dp)) }
}
private fun clock(ms: Long): String { val seconds = ms.coerceAtLeast(0) / 1000; return if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60) else "%02d:%02d".format(seconds / 60, seconds % 60) }

