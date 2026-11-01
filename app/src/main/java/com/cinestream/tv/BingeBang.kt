package com.cinestream.tv

import org.json.JSONObject
import java.net.URI

internal fun bingePage(path: String, playback: Boolean = false): String {
    val url = URI(ProvidersRepository.BINGEBANG).resolve(path).toString()
    val uri = URI(url)
    val kind = if (playback) "play" else "watch"
    require(uri.scheme == "https" && uri.host == "bingebang.st" && uri.userInfo == null &&
        Regex("/(movie|tv)/$kind/[^/]+(?:/\\d+/\\d+)?").matches(uri.path) && uri.query == null && uri.fragment == null) {
        "BingeBang returned an invalid $kind page."
    }
    return url
}

private fun bingeImage(path: String, size: String = "w500") =
    if (path.startsWith("/") && !path.startsWith("//")) "https://image.tmdb.org/t/p/$size$path" else ""

internal fun parseBingeList(json: String): List<DesignTitle> {
    val results = JSONObject(json).getJSONArray("results")
    return (0 until results.length()).map { results.getJSONObject(it) }
        .filter { it.optString("media_type") in listOf("movie", "tv") }
        .map { data ->
            val type = data.getString("media_type")
            val name = data.optString(if (type == "tv") "name" else "title")
            require(name.isNotBlank() && data.optLong("id") > 0) { "BingeBang title identity was missing." }
            val url = bingePage(data.getString("url"))
            val date = data.optString(if (type == "tv") "first_air_date" else "release_date")
            liveTitle("bingebang", url, name, bingeImage(data.optString("poster_path")),
                year = data.optInt("year", date.take(4).toIntOrNull() ?: 0), score = data.optString("vote_average"),
                tv = type == "tv", backdrop = bingeImage(data.optString("backdrop_path"), "w1280"), overview = data.optString("overview"))
                .copy(id = "bingebang:$type:${data.getLong("id")}")
        }.distinctBy { it.id }
}

internal fun parseBingeDetails(title: DesignTitle, data: JSONObject): DesignTitle {
    val parsed = parseBingeList(JSONObject().put("results", org.json.JSONArray().put(data)).toString()).single()
    require(parsed.id == title.id && parsed.pageUrl == title.pageUrl) { "BingeBang returned details for a different title." }
    val genres = data.optJSONArray("genres")
    val countries = data.optJSONArray("production_countries")
    val cast = data.optJSONObject("credits")?.optJSONArray("cast")
    return parsed.copy(tagline = data.optString("tagline"), minutes = data.optInt("runtime"),
        playbackPage = bingePage(data.getString("play_url"), playback = true),
        genres = if (genres == null) emptyList() else (0 until genres.length()).map { genres.getJSONObject(it).getString("name") },
        countries = if (countries == null) emptyList() else (0 until countries.length()).map { countries.getJSONObject(it).getString("name") },
        cast = if (cast == null) emptyList() else (0 until cast.length()).map { index -> cast.getJSONObject(index).let {
            DesignCast(it.getString("name"), it.optString("character"), bingeImage(it.optString("profile_path"), "w185"))
        } })
}

internal fun parseBingeEpisodes(json: String, season: Int, playbackPage: String): List<DesignEpisode> {
    val episodes = JSONObject(json).getJSONArray("episodes")
    return (0 until episodes.length()).map { index ->
        val data = episodes.getJSONObject(index)
        val number = data.getInt("episode_number")
        require(number > 0 && data.optLong("id") > 0) { "BingeBang episode identity was missing." }
        val page = bingePage(data.getString("play_url"), playback = true)
        require(page == "$playbackPage/$season/$number") { "BingeBang returned an episode from a different selection." }
        DesignEpisode("bingebang:episode:${data.getLong("id")}", number, season, data.optString("name").ifBlank { "Episode $number" },
            data.optString("overview"), bingeImage(data.optString("still_path"), "w300"), data.optInt("runtime"), data.optString("air_date"), page)
    }.distinctBy { it.id }
}

internal fun bingeStream(title: DesignTitle, episode: DesignEpisode?): ProviderStream {
    val page = if (title.isTv) {
        val episodes = title.seasons.flatMap { it.episodes }
        val selected = episode ?: episodes.firstOrNull() ?: error("BingeBang did not list any available episodes.")
        require(episodes.any { it.id == selected.id && it.pageUrl == selected.pageUrl }) { "This BingeBang episode is not listed for the title." }
        selected.pageUrl
    } else title.playbackPage
    return ProviderStream("BingeBang · servers in player", bingePage(page, playback = true), true, ProvidersRepository.BINGEBANG + "/")
}
