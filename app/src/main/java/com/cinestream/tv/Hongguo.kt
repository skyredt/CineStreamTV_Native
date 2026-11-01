package com.cinestream.tv

import org.json.JSONObject
import org.json.JSONTokener
import org.jsoup.Jsoup
import java.net.URI
import java.net.URLEncoder

internal const val HONGGUO_NOTICE = "红果播放尚未支持。本阶段仅提供目录、详情和选集；逐集锁定状态未知。"
internal val HONGGUO_CATEGORIES = linkedMapOf("real-drama" to "真人剧", "comic-drama" to "漫剧", "ai-drama" to "AI剧")

internal fun hongguoSearchUrl(query: String, category: String = "real-drama"): String {
    require(category in HONGGUO_CATEGORIES)
    return if (query.isBlank()) "${ProvidersRepository.HONGGUO}/category/$category"
    else "${ProvidersRepository.HONGGUO}/api/search/result?aid=8662&query=${URLEncoder.encode(query.trim(), "UTF-8") }"
}

internal fun hongguoLoader(html: String, route: String): JSONObject {
    val marker = "_ROUTER_DATA = "
    require(html.contains(marker)) { "Hongguo did not return catalog data." }
    val root = JSONTokener(html.substringAfter(marker)).nextValue() as? JSONObject ?: error("Invalid Hongguo data.")
    val page = root.getJSONObject("loaderData").getJSONObject(route)
    require(page.optBoolean("isSuccess")) { "Hongguo catalog request failed." }
    return page
}

private fun hongguoTitle(data: JSONObject): DesignTitle {
    val id = data.getString("series_id")
    require(id.isNotBlank())
    val name = data.optString("series_name").ifBlank { data.optString("series_title") }
    require(name.isNotBlank()) { "Hongguo title was missing." }
    val tags = data.optJSONArray("tags")
    return liveTitle("hongguo", "${ProvidersRepository.HONGGUO}/detail?series_id=$id", name,
        data.optString("series_cover"), overview = data.optString("series_intro"),
        genres = if (tags == null) emptyList() else (0 until tags.length()).map { tags.getString(it) })
        .copy(id = "hongguo:$id", isShort = true, playbackNotice = HONGGUO_NOTICE)
}

internal fun parseHongguoHome(html: String): List<DesignRow> {
    val sections = hongguoLoader(html, "page").getJSONArray("homeSections")
    return (0 until sections.length()).map { index ->
        val section = sections.getJSONObject(index)
        val items = section.getJSONArray("video_list")
        DesignRow("hongguo:${section.getString("tab_type")}", "红果 · ${section.getString("tab_name")}", "红果短剧",
            (0 until items.length()).map { hongguoTitle(items.getJSONObject(it)) }.distinctBy { it.id })
    }.filter { it.titles.isNotEmpty() }
}

internal fun parseHongguoSearch(body: String, url: String): Pair<List<DesignTitle>, String?> {
    if (URI(url).path == "/api/search/result") {
        val data = JSONObject(body)
        val results = data.getJSONArray("searchList")
        val titles = (0 until results.length()).map { results.getJSONObject(it) }
            .mapNotNull { it.optJSONObject("video_data")?.takeIf { row -> row.has("series_id") } }
            .map(::hongguoTitle).distinctBy { it.id }
        val page = data.getInt("pageNum")
        val next = if (data.optBoolean("hasMore") && page < data.getInt("totalPages"))
            url.replace(Regex("&page=\\d+"), "") + "&page=${page + 1}" else null
        return titles to next?.takeIf { it != url }
    }
    val data = hongguoLoader(body, "category_$")
    val results = data.getJSONArray("recommendList")
    val pagination = data.getJSONObject("pagination")
    val wanted = "${URI(url).path}?page=${pagination.getInt("pageNum") + 1}"
    val next = Jsoup.parse(body, ProvidersRepository.HONGGUO).select("a[href]").firstOrNull { it.attr("href") == wanted }
        ?.let { URI(ProvidersRepository.HONGGUO).resolve(it.attr("href")).toString() }
        ?.takeIf { pagination.getInt("pageNum") < pagination.getInt("totalPages") && it != url }
    return (0 until results.length()).map { hongguoTitle(results.getJSONObject(it)) }.distinctBy { it.id } to next
}

internal fun parseHongguoDetails(title: DesignTitle, html: String): DesignTitle {
    val data = hongguoLoader(html, "detail_page").getJSONObject("seriesDetail")
    val parsed = hongguoTitle(data)
    require(parsed.id == title.id) { "Hongguo returned a different drama." }
    val ids = data.getJSONArray("vid_list")
    val episodes = (0 until ids.length()).map { index ->
        val vid = ids.getString(index)
        require(vid.isNotBlank())
        DesignEpisode("${parsed.id}:$vid", index + 1, 1, "第${index + 1}集", "逐集锁定状态未知；本阶段不支持播放。", "", 0, "", availability = "状态未知 · 暂不支持播放")
    }
    require(episodes.map { it.id }.distinct().size == episodes.size) { "Hongguo returned duplicate episode IDs." }
    val count = data.getInt("episode_cnt")
    val accessible = data.optInt("accessible_episode_cnt", -1)
    val notice = HONGGUO_NOTICE + " 网站列出 ${episodes.size}/$count 集。" +
        if (accessible >= 0) "网页标注可访问 $accessible 集，但未提供逐集对应关系。" else ""
    return parsed.copy(seasons = listOf(DesignSeason(1, "剧集", episodes)), playbackNotice = notice)
}
