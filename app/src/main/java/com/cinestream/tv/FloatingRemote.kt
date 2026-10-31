package com.cinestream.tv

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import android.view.SoundEffectConstants
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Simulator's original floating remote, backed by real Android key dispatch. Touch never steals D-pad focus. */
@Composable
fun FloatingRemote(mediaEnabled: Boolean, onHome: () -> Unit, onMenu: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        var minimized by rememberSaveable { mutableStateOf(maxWidth < 600.dp) }
        var sound by rememberSaveable { mutableStateOf(true) }
        fun emit(code: Int) {
            if (sound) view.playSoundEffect(SoundEffectConstants.CLICK)
            context.activity()?.let { activity -> activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code)); activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code)) }
        }
        if (minimized) {
            RemoteTouch("Android TV D-Pad", Icons.Outlined.Gamepad, Modifier.align(Alignment.BottomEnd).padding(20.dp).size(48.dp).clip(CircleShape).background(Color(0xEE111111)).border(1.dp, Accent.copy(alpha = .5f), CircleShape), color = Accent) { minimized = false }
        } else {
            Surface(Modifier.align(Alignment.BottomEnd).padding(24.dp).width(288.dp).heightIn(max = maxHeight - 32.dp), shape = RoundedCornerShape(24.dp), color = Color(0xF9090B0B), border = BorderStroke(1.dp, Color(0xFF262626))) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("ANDROID TV D-PAD", Modifier.weight(1f), fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        RemoteTouch(if (sound) "Mute navigation sounds" else "Enable navigation sounds", if (sound) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff, Modifier.size(28.dp)) { sound = !sound }
                        RemoteTouch("Minimize Remote", Icons.Outlined.CloseFullscreen, Modifier.size(28.dp)) { minimized = true }
                    }
                    HorizontalDivider(color = Color(0xFF262626))
                    Box(Modifier.align(Alignment.CenterHorizontally).size(192.dp).clip(CircleShape).background(Color(0xFF171717)).border(2.dp, Color(0xFF292929), CircleShape)) {
                        RemoteTouch("D-Pad Up", Icons.Outlined.KeyboardArrowUp, Modifier.align(Alignment.TopCenter).padding(top = 4.dp).size(48.dp)) { emit(KeyEvent.KEYCODE_DPAD_UP) }
                        RemoteTouch("D-Pad Down", Icons.Outlined.KeyboardArrowDown, Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(48.dp)) { emit(KeyEvent.KEYCODE_DPAD_DOWN) }
                        RemoteTouch("D-Pad Left", Icons.Outlined.KeyboardArrowLeft, Modifier.align(Alignment.CenterStart).padding(start = 4.dp).size(48.dp)) { emit(KeyEvent.KEYCODE_DPAD_LEFT) }
                        RemoteTouch("D-Pad Right", Icons.Outlined.KeyboardArrowRight, Modifier.align(Alignment.CenterEnd).padding(end = 4.dp).size(48.dp)) { emit(KeyEvent.KEYCODE_DPAD_RIGHT) }
                        Column(Modifier.align(Alignment.Center).size(80.dp).clip(CircleShape).background(Color(0xFF262626)).border(1.dp, Color(0xFF404040), CircleShape).remoteClick("Select / OK") { emit(KeyEvent.KEYCODE_DPAD_CENTER) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("OK", fontSize = 13.sp, fontWeight = FontWeight.Black); Text("SELECT", color = Accent, fontSize = 9.sp) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RemoteTile("Back", Icons.Outlined.Undo, Modifier.weight(1f)) { emit(KeyEvent.KEYCODE_BACK) }
                        RemoteTile("Home", Icons.Outlined.Home, Modifier.weight(1f), onClick = onHome)
                        RemoteTile("Menu", Icons.Outlined.Menu, Modifier.weight(1f), onClick = onMenu)
                    }
                    HorizontalDivider(color = Color(0xFF262626))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RemoteTile("-10s", Icons.Outlined.FastRewind, Modifier.weight(1f)) { if (mediaEnabled) emit(KeyEvent.KEYCODE_MEDIA_REWIND) }
                        RemoteTile("Play/Pause", Icons.Outlined.PlayArrow, Modifier.weight(1f), color = Accent) { if (mediaEnabled) emit(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }
                        RemoteTile("+10s", Icons.Outlined.FastForward, Modifier.weight(1f)) { if (mediaEnabled) emit(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) }
                    }
                    Row(Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Outlined.Keyboard, null, Modifier.size(13.dp), tint = Muted); Text("Keys: Arrows + Enter", fontSize = 10.sp, color = Muted) }
                }
            }
        }
    }
}

private fun Modifier.remoteClick(label: String, action: () -> Unit): Modifier = semantics { contentDescription = label; onClick { action(); true } }.pointerInput(label, action) { detectTapGestures { action() } }
@Composable
private fun RemoteTouch(label: String, icon: ImageVector, modifier: Modifier, color: Color = Muted, action: () -> Unit) { Box(modifier.remoteClick(label, action), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(22.dp), tint = color) } }
@Composable
private fun RemoteTile(label: String, icon: ImageVector, modifier: Modifier, color: Color = Color.LightGray, onClick: () -> Unit) { Column(modifier.height(52.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF171717)).border(1.dp, Color(0xFF292929), RoundedCornerShape(12.dp)).remoteClick(label, onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(icon, null, Modifier.size(18.dp), tint = color); Text(label, fontSize = 9.sp, color = color) } }
private tailrec fun Context.activity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.activity(); else -> null }
