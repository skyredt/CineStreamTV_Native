package com.cinestream.tv

import android.app.Activity
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.json.JSONObject

/** The provider owns playback, seeking, subtitles and quality. Native UI does not duplicate them. */
@Composable
fun EmbeddedPlayer(source: ProviderStream, title: String, initialPosition: Long = 0, onPosition: (Long) -> Unit = {}, onFailure: (String) -> Boolean = { false }, onBack: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val supported = android.net.Uri.parse(source.url).host == "play.xpass.top"
    val script = remember { context.assets.open("tv-web-player.js").bufferedReader().use { it.readText() } }
    var browser by remember { mutableStateOf<WebView?>(null) }
    var error by remember(source.url) { mutableStateOf<String?>(null) }
    var fullScreen by remember { mutableStateOf<View?>(null) }
    var fullScreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(error) { error?.let { if (onFailure(it)) error = null } }
    LaunchedEffect(browser) {
        val view = browser ?: return@LaunchedEffect
        if (!supported) return@LaunchedEffect
        var loadingMillis = 0L
        while (isActive) {
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                view.evaluateJavascript("JSON.stringify(window.cinedbkPlayer?window.cinedbkPlayer():{})") { raw ->
                    runCatching {
                        val json = JSONObject(org.json.JSONTokener(raw).nextValue() as String)
                        val duration = json.optDouble("duration", 0.0)
                        ready = ready || duration > 0 || json.optBoolean("playing")
                        if (ready) onPosition((json.optDouble("position", 0.0) * 1000).toLong().coerceAtLeast(0))
                        if (json.optBoolean("failed")) error = "The selected source reported a playback error."
                    }
                }
                if (!ready) loadingMillis += 500
                if (!ready && loadingMillis > 45000 && error == null) error = "The selected source did not become ready within 45 seconds."
            }
            delay(500)
        }
    }
    fun leavePlayer() {
        if (fullScreen != null) { fullScreenCallback?.onCustomViewHidden(); fullScreen = null; fullScreenCallback = null }
        else browser?.evaluateJavascript("window.cinedbkTvBack?window.cinedbkTvBack():false") { handled -> if (handled != "true") onBack() } ?: onBack()
    }
    BackHandler { leavePlayer() }
    DisposableEffect(owner, browser) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) { browser?.evaluateJavascript("window.cinedbkPause&&window.cinedbkPause()", null); browser?.onPause() }
            else if (event == Lifecycle.Event.ON_RESUME) browser?.onResume()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(context) {
        val window = (context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.navigationBars()) }
    }
    fun navigateKey(code: Int, action: Int): Boolean {
        if (!supported || error != null) return false
        val directions = listOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT)
        if (code !in directions && code !in listOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER)) return false
        if (action == KeyEvent.ACTION_DOWN) {
            val command = if (code in directions) "window.cinedbkTvMove&&window.cinedbkTvMove($code)" else "window.cinedbkTvPress&&window.cinedbkTvPress()"
            browser?.evaluateJavascript(command, null)
        }
        return true
    }
    val currentNavigation by rememberUpdatedState<(KeyEvent) -> Boolean>({ navigateKey(it.keyCode, it.action) })
    DisposableEffect(context) {
        var activityContext = context
        while (activityContext is android.content.ContextWrapper && activityContext !is MainActivity) activityContext = activityContext.baseContext
        val activity = activityContext as? MainActivity
        val handler: (KeyEvent) -> Boolean = { currentNavigation(it) }
        activity?.webPlayerKeyHandler = handler
        onDispose { if (activity != null && activity.webPlayerKeyHandler === handler) activity.webPlayerKeyHandler = null }
    }
    Box(Modifier.fillMaxSize().background(Color.Black).onPreviewKeyEvent { navigateKey(it.nativeKeyEvent.keyCode, it.nativeKeyEvent.action) }) {
        AndroidView(factory = { ctx ->
            WebView(ctx).apply {
                browser = this
                setBackgroundColor(android.graphics.Color.BLACK)
                keepScreenOn = true
                isFocusable = true
                isFocusableInTouchMode = true
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.setSupportMultipleWindows(false)
                setOnKeyListener { _, code, event -> navigateKey(code, event.action) }
                webChromeClient = object : WebChromeClient() {
                    override fun onShowCustomView(view: View, callback: CustomViewCallback) { fullScreen = view; fullScreenCallback = callback }
                    override fun onHideCustomView() { fullScreen = null; fullScreenCallback = null }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                        request.url.scheme != "https" || (request.isForMainFrame && request.url.host != android.net.Uri.parse(source.url).host)
                    override fun onPageCommitVisible(view: WebView, url: String) {
                        if (supported && android.net.Uri.parse(url).host == android.net.Uri.parse(source.url).host) view.evaluateJavascript("window.cinedbkResume=${initialPosition / 1000.0};\n" + script, null)
                    }
                    override fun onPageFinished(view: WebView, url: String) {
                        if (supported && android.net.Uri.parse(url).host == android.net.Uri.parse(source.url).host) {
                            evaluateJavascript("window.cinedbkResume=${initialPosition / 1000.0};\n" + script, null)
                        }
                    }
                    override fun onReceivedError(view: WebView, request: WebResourceRequest, value: WebResourceError) {
                        if (request.isForMainFrame) error = "Web player failed to load: ${value.description}."
                    }
                    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                        if (request.isForMainFrame) error = "Web player returned HTTP ${response.statusCode}."
                    }
                }
                loadUrl(source.url, if (source.referer.isBlank()) emptyMap() else mapOf("Referer" to source.referer))
                post { requestFocus() }
            }
        }, onRelease = { view ->
            fullScreenCallback?.onCustomViewHidden(); view.stopLoading(); view.loadUrl("about:blank"); view.onPause(); view.removeAllViews(); view.destroy(); browser = null
        }, modifier = Modifier.fillMaxSize())
        fullScreen?.let { custom -> AndroidView(factory = { custom }, modifier = Modifier.fillMaxSize()) }
        error?.let { message ->
            Surface(Modifier.align(Alignment.Center).padding(32.dp).widthIn(max = 520.dp), color = Color(0xF2111111)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(message)
                    Action("Retry this source") { error = null; ready = false; browser?.reload() }
                    Action("Back", onClick = onBack)
                }
            }
        }
    }
}
