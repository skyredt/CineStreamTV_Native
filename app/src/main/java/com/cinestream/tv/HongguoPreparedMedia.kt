package com.cinestream.tv

import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.URI

/** Contract boundary only; no protected preparation implementation is installed. */
internal interface HongguoMediaPreparation {
    suspend fun prepare(seriesId: String, episodeId: String, progress: (Int) -> Unit): Prepared
    suspend fun release()
    class Prepared(val seriesId: String, val episodeId: String, val address: String)
}

internal class HongguoPreparedMedia(
    private val session: HongguoCoreRuntime.Session,
    private val preparation: HongguoMediaPreparation,
) {
    /** The caller must release on stop, navigation, cancellation or error before stopping the runtime. */
    suspend fun select(seriesId: String, episodeId: String, progress: (Int) -> Unit): HongguoMediaPreparation.Prepared {
        require(seriesId.matches(Regex("[0-9]{1,24}")) && episodeId.matches(Regex("[0-9]{1,24}")))
        session.requireAlive()
        try {
            val result = preparation.prepare(seriesId, episodeId) { progress(it.coerceIn(0, 100)) }
            session.requireAlive()
            val uri = URI(result.address)
            if (result.seriesId != seriesId || result.episodeId != episodeId || uri.scheme != "http" ||
                uri.host != "127.0.0.1" || uri.port != session.endpoint.port || uri.rawUserInfo != null ||
                uri.rawFragment != null || uri.path.isNullOrBlank()) throw IOException("Invalid prepared media identity or origin")
            return result
        } catch (error: Exception) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { preparation.release() }
            if (error is CancellationException) throw error
            throw IOException("Hongguo media preparation failed")
        }
    }
    fun playbackHeaders(): Map<String, String> = session.headers()
    suspend fun release() = preparation.release()
}
