package com.cinestream.tv

import android.graphics.Bitmap
import android.view.View
import android.view.KeyEvent
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Cineby publishes iframe players, not direct Media3 URLs. Use its supplied player unchanged. */
@Composable
fun EmbeddedPlayer(source: ProviderStream, title: String, onBack: () -> Unit) {
    var browser by remember { mutableStateOf<WebView?>(null) }
    var error by remember(source.url) { mutableStateOf<String?>(null) }
    var fullScreen by remember { mutableStateOf<View?>(null) }
    var fullScreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    fun mediaKey(code: Int): Boolean {
        if (android.net.Uri.parse(source.url).host != "play.xpass.top") return false
        val action = when (code) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "toggle"
            KeyEvent.KEYCODE_MEDIA_PLAY -> "play"
            KeyEvent.KEYCODE_MEDIA_PAUSE -> "pause"
            KeyEvent.KEYCODE_MEDIA_REWIND -> "rewind"
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> "forward"
            else -> return false
        }
        browser?.evaluateJavascript("window.cinestreamControl&&window.cinestreamControl('$action')", null)
        return true
    }
    val owner = LocalLifecycleOwner.current
    BackHandler(fullScreen != null) { fullScreenCallback?.onCustomViewHidden(); fullScreen = null; fullScreenCallback = null }
    DisposableEffect(owner, browser) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) { browser?.evaluateJavascript("window.cinestreamControl&&window.cinestreamControl('pause')", null); browser?.onPause() }
            else if (event == Lifecycle.Event.ON_RESUME) browser?.onResume()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Box(Modifier.fillMaxSize().background(Color.Black).onPreviewKeyEvent { it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && mediaKey(it.nativeKeyEvent.keyCode) }) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Action("Back", dense = true, onClick = onBack)
                Column(Modifier.weight(1f).padding(end = 200.dp)) { Text(title, maxLines = 1, fontSize = 18.sp); Text("Cineby · ${source.name}", color = Accent, fontSize = 11.sp) }
            }
            AndroidView(factory = { context ->
                WebView(context).apply {
                    browser = this
                    setBackgroundColor(android.graphics.Color.BLACK)
                    keepScreenOn = true
                    setOnKeyListener { _, code, event -> event.action == KeyEvent.ACTION_DOWN && mediaKey(code) }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.setSupportMultipleWindows(false)
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View, callback: CustomViewCallback) { fullScreen = view; fullScreenCallback = callback }
                        override fun onHideCustomView() { fullScreenCallback?.onCustomViewHidden(); fullScreen = null; fullScreenCallback = null }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val scheme = request.url.scheme
                            if (scheme != "https") return true
                            // Keep popup/advertisement top-level redirects from replacing the chosen player.
                            return request.isForMainFrame && request.url.host != android.net.Uri.parse(source.url).host
                        }
                        override fun onReceivedError(view: WebView, request: WebResourceRequest, value: WebResourceError) {
                            if (request.isForMainFrame || request.url.toString() == source.url) error = "Embedded player failed to load: ${value.description}. Use another source or go Back."
                        }
                        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                            if (request.isForMainFrame || request.url.toString() == source.url) error = "Embedded player returned HTTP ${response.statusCode}. Use another source or go Back."
                        }
                    }
                    val safe = source.url.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;")
                    val html = """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>html,body{position:fixed;inset:0;margin:0;padding:0;width:100%;border:0;background:#000;overflow:hidden}iframe{position:absolute;inset:0;display:block;margin:0;padding:0;width:100%;border:0;background:#000}</style></head><body><iframe id="selected-player" src="$safe" allow="autoplay; fullscreen; encrypted-media; picture-in-picture" referrerpolicy="same-origin" allowfullscreen></iframe><script>
                        var frame=document.getElementById('selected-player'), target=new URL(frame.src), state={playing:false,position:0};
                        window.cinestreamFit=function(){var h=Math.max(window.innerHeight||document.documentElement.clientHeight,1);[document.documentElement,document.body,frame].forEach(function(e){e.style.setProperty('height',h+'px','important')})};
                        function command(action,position){if(target.hostname!=='play.xpass.top')return;var message={type:'player.action',action:action};if(position!==undefined)message.position=position;frame.contentWindow.postMessage(message,target.origin)}
                        window.cinestreamControl=function(action){if(action==='toggle')command(state.playing?'pause':'play');else if(action==='rewind'||action==='forward')command('playAt',Math.max(0,state.position+(action==='forward'?10:-10)));else command(action)};
                        window.addEventListener('message',function(e){if(e.source!==frame.contentWindow||e.origin!==target.origin||target.hostname!=='play.xpass.top')return;var data=e.data;try{if(typeof data==='string')data=JSON.parse(data)}catch(_){return}if(!data||data.type!=='player.event'||!data.event)return;var event=data.event;if(event.name==='ready')command('play');if(event.name==='play')state.playing=true;if(event.name==='pause'||event.name==='end')state.playing=false;if(event.name==='position'&&Number.isFinite(event.position))state.position=event.position});
                        window.addEventListener('resize',window.cinestreamFit);window.addEventListener('load',window.cinestreamFit);window.cinestreamFit();requestAnimationFrame(window.cinestreamFit);
                    </script></body></html>"""
                    addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> post { evaluateJavascript("window.cinestreamFit&&window.cinestreamFit()", null) } }
                    loadDataWithBaseURL(source.referer, html, "text/html", "UTF-8", null)
                }
            }, onRelease = { view -> fullScreenCallback?.onCustomViewHidden(); view.stopLoading(); view.loadUrl("about:blank"); view.onPause(); view.removeAllViews(); view.destroy(); browser = null }, modifier = Modifier.weight(1f).fillMaxWidth())
        }
        fullScreen?.let { custom -> AndroidView(factory = { custom }, modifier = Modifier.fillMaxSize()) }
        error?.let { message ->
            Surface(Modifier.align(Alignment.Center).padding(32.dp).widthIn(max = 520.dp), color = Color(0xF2111111), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { Text(message); Action("Retry this source") { error = null; browser?.reload() }; Action("Back", onClick = onBack) }
            }
        }
    }
}
