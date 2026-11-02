package com.cinestream.tv

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Test-only app_process child, not the community engine. Unprotected PCM; no Hongguo access. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun runHongguoRuntimeCheck(runner: Instrumentation) = with(runner) {
    val report = Bundle()
    var resultCode = Activity.RESULT_OK
    val passed = mutableListOf<String>()
    var player: ExoPlayer? = null
    var child: Process? = null
    val root = File(targetContext.cacheDir, "hongguo-fixture-check")
    val runtime = HongguoCoreRuntime(root, "fixture-v1", { instance, secret, work ->
        ProcessBuilder("/system/bin/app_process", "/system/bin", "com.cinestream.tv.FixtureCore", instance, "ok")
            .directory(work).redirectErrorStream(true).apply {
                environment()["CLASSPATH"] = context.applicationInfo.sourceDir + File.pathSeparator + targetContext.applicationInfo.sourceDir
                environment()["CINEDBKTV_CORE_TOKEN"] = secret
            }.start().also { child = it }
    }, 15000)
    try {
        runBlocking {
            val session = runtime.start()
            check(child!!.isAlive)
            check(HongguoCoreHttp.get(session.endpoint.baseUrl + "api/v1/status", emptyMap()).status == 403)
            passed += "Android-owned test child: authenticated readiness/version; anonymous request denied"
            val adapter = HongguoPreparedMedia(session, object : HongguoMediaPreparation {
                override suspend fun prepare(seriesId: String, episodeId: String, progress: (Int) -> Unit): HongguoMediaPreparation.Prepared {
                    progress(100)
                    return HongguoMediaPreparation.Prepared(seriesId, episodeId, session.endpoint.baseUrl + "fixture.wav")
                }
                override suspend fun release() { }
            })
            val media = adapter.select("123", "51") { }
            val ready = CountDownLatch(1)
            val ended = CountDownLatch(1)
            var failure: String? = null
            runOnMainSync {
                val data = androidx.media3.datasource.DefaultHttpDataSource.Factory().setDefaultRequestProperties(adapter.playbackHeaders())
                player = ExoPlayer.Builder(targetContext).setMediaSourceFactory(
                    androidx.media3.exoplayer.source.DefaultMediaSourceFactory(targetContext).setDataSourceFactory(data)
                ).build().apply {
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) ready.countDown()
                            if (state == Player.STATE_ENDED) ended.countDown()
                        }
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            failure = error.errorCodeName; ready.countDown(); ended.countDown()
                        }
                    })
                    setMediaItem(MediaItem.fromUri(media.address)); prepare()
                }
            }
            check(ready.await(15, TimeUnit.SECONDS)) { "Fixture Media3 readiness timed out" }
            check(failure == null) { "Fixture Media3 error: $failure" }
            runOnMainSync {
                check(player!!.duration in 900..1100)
                player!!.play(); player!!.pause(); check(!player!!.playWhenReady)
                player!!.seekTo(500); player!!.play()
            }
            check(ended.await(10, TimeUnit.SECONDS)) { "Fixture Media3 completion timed out" }
            check(failure == null) { "Fixture Media3 error: $failure" }
            passed += "Media3 unprotected PCM fixture: READY, pause/resume, seek, ENDED (no video/audio-observation claim)"
            runOnMainSync { player?.release(); player = null }
            adapter.release(); runtime.stop()
            check(!child!!.isAlive && root.listFiles().orEmpty().isEmpty())
            passed += "Android child stopped and private runtime files removed"
        }
        report.putString("stream", passed.joinToString("\nPASS: ", "PASS: ") + "\n")
    } catch (error: Throwable) {
        report.putString("stream", passed.joinToString("\nPASS: ", "PASS: ") + "\nFAIL: ${error.stackTraceToString()}\n")
        resultCode = Activity.RESULT_CANCELED
    } finally {
        runOnMainSync { player?.release() }
        runBlocking { runtime.stop() }
        root.delete()
    }
    finish(resultCode, report)
}
