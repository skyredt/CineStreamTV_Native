package com.cinestream.tv

import java.net.URI

data class Episode(val id: String, val title: String, val url: String)
data class Title(val id: String, val name: String, val description: String, val episodes: List<Episode>)

/** Explicit demo fixtures; no mapping from commercial titles to unrelated streams. */
object MockData {
    val titles = listOf(
        Title("hls", "Apple HLS · Playback sample", "Demo catalog • Apple's public streaming test clip, also listed in the AndroidX Media3 demo catalog. Test patterns and sample footage, not a commercial film. Adaptive quality and tracks depend on the manifest.", listOf(
            Episode("apple-hls", "Play Apple HLS sample", "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8"))),
        Title("bunny", "Big Buck Bunny · Sample film", "Demo catalog • Blender Foundation animated short. This original project's sample host returned HTTP 403 during the desktop check; it may be unavailable on your network. Errors never substitute another video.", listOf(
            Episode("bunny-film", "Play Big Buck Bunny sample", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"))),
        Title("showcase", "Sample shorts · Demo series", "Demo catalog • A sample collection arranged as one season to test episode selection. Separate Blender shorts, not a commercial series. This original sample host returned HTTP 403 during the desktop check; availability may differ on your network.", listOf(
            Episode("s1e1", "Season 1 · Episode 1: Sintel (sample)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"),
            Episode("s1e2", "Season 1 · Episode 2: Tears of Steel (sample)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4")))
    )
}

class CatalogRepository { fun catalog(): List<Title> = MockData.titles }

object MediaUrl {
    fun validate(value: String): String? = try {
        val uri = URI(value.trim())
        when {
            uri.scheme != "https" -> "Enter an HTTPS direct media URL (MP4, HLS or DASH)."
            uri.host.isNullOrBlank() || uri.rawUserInfo != null -> "Enter a valid host without embedded credentials."
            else -> null
        }
    } catch (_: Exception) { "Enter a valid HTTPS direct media URL." }
}
