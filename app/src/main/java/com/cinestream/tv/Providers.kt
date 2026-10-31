package com.cinestream.tv

import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64

data class ProviderStream(val name: String, val url: String, val embedded: Boolean, val referer: String)
data class LiveCatalog(val catalog: DesignCatalog, val warnings: List<String>)

/** Public page adapters for the two user-selected providers. No guessed stream API or sample fallback. */
class ProvidersRepository {
    companion object {
        const val CINEBY = "https://cineby.ws"
        const val DBKU = "https://www.dbku.tv"
        const val AGENT = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36 CineStream/1.1"
    }
    private val detailsCache = mutableMapOf<String, DesignTitle>()
    private fun request(url: String, ajax: Boolean = false): String {
        require(URI(url).scheme == "https") { "Provider returned a non-HTTPS URL" }
        val connection = Jsoup.connect(url).userAgent(AGENT).timeout(20000).maxBodySize(3 * 1024 * 1024).ignoreContentType(true)
        if (ajax) connection.header("X-Requested-With", "XMLHttpRequest").header("Accept", "application/json")
        return connection.execute().body()
    }
    suspend fun home(): LiveCatalog = withContext(Dispatchers.IO) {
        val warnings = mutableListOf<String>()
        val cine = async { runCatching { parseCinebyHome(request("$CINEBY/home")) } }
        val dbku = async { runCatching { parseDbkuHome(request("$DBKU/")) } }
        val rows = cine.await().getOrElse { if (it is CancellationException) throw it; warnings += "Cineby: ${it.message}"; emptyList() } + dbku.await().getOrElse { if (it is CancellationException) throw it; warnings += "DBKU: ${it.message}"; emptyList() }
        if (rows.isEmpty()) error(warnings.joinToString("\n").ifBlank { "The providers returned no catalog entries." })
        val titles = rows.flatMap { it.titles }.distinctBy { it.id }.toMutableList()
        if (titles.none { it.featured }) titles[0] = titles[0].copy(featured = true)
        LiveCatalog(DesignCatalog(titles, rows), warnings)
    }
    suspend fun details(title: DesignTitle): DesignTitle = withContext(Dispatchers.IO) {
        synchronized(detailsCache) { detailsCache[title.id] } ?: run {
            val doc = Jsoup.parse(request(title.pageUrl), title.pageUrl)
            val value = if (title.provider == "dbku") parseDbkuDetails(title, doc) else parseCinebyDetails(title, doc)
            synchronized(detailsCache) { detailsCache[title.id] = value }; value
        }
    }
    suspend fun search(query: String): List<DesignTitle> = withContext(Dispatchers.IO) {
        val value = URLEncoder.encode(query, "UTF-8")
        val cine = async { runCatching { parseCinebyHome(request("$CINEBY/search?q=$value"), true).flatMap { it.titles }.distinctBy { it.id } } }
        val dbku = async { runCatching {
            val doc = Jsoup.parse(request("$DBKU/vodsearch/-------------.html?wd=$value"), DBKU)
            doc.select("a.myui-vodlist__thumb[href*=/voddetail/]").map { a -> liveTitle("dbku", a.absUrl("href"), a.attr("title"), a.absUrl("data-original")) }.distinctBy { it.id }
        } }
        val c = cine.await(); val d = dbku.await()
        c.exceptionOrNull()?.let { if (it is CancellationException) throw it }; d.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        if (c.isFailure && d.isFailure) error("Both provider searches failed: ${c.exceptionOrNull()?.message}; ${d.exceptionOrNull()?.message}")
        c.getOrDefault(emptyList()) + d.getOrDefault(emptyList())
    }
    suspend fun sources(title: DesignTitle, episode: DesignEpisode?): List<ProviderStream> = withContext(Dispatchers.IO) {
        val detail = details(title)
        val sources = if (title.provider == "dbku") {
            val page = episode?.pageUrl?.takeIf { it.isNotBlank() } ?: detail.playbackPage
            require(page.isNotBlank()) { "DBKU did not provide a playback link for this title." }
            val html = request(page)
            val raw = Regex("var\\s+player_data\\s*=\\s*(\\{.*?\\})\\s*</script>", RegexOption.DOT_MATCHES_ALL).find(html)?.groupValues?.get(1) ?: error("DBKU player data was not found. The provider page may have changed.")
            val data = JSONObject(raw)
            val url = decodeDbkuUrl(data.getString("url"), data.optInt("encrypt"))
            listOf(ProviderStream(data.optString("from", "DBKU"), url, !isDirectMedia(url), "$DBKU/"))
        } else if (detail.isTv) {
            val target = episode ?: detail.seasons.firstOrNull()?.episodes?.firstOrNull() ?: error("Cineby did not list any episodes.")
            val response = JSONObject(request(target.pageUrl, true))
            require(response.optBoolean("success")) { response.optString("error", "Cineby could not load this episode.") }
            val list = response.optJSONArray("playerSources") ?: error("Cineby returned no player sources.")
            (0 until list.length()).map { list.getJSONObject(it) }.map { ProviderStream(it.getString("name"), it.getString("url"), !isDirectMedia(it.getString("url")), "$CINEBY/") }
        } else {
            val doc = Jsoup.parse(request(detail.pageUrl), detail.pageUrl)
            doc.select(".server-tab[data-url]").map { ProviderStream(it.text(), it.absUrl("data-url").ifBlank { URI(detail.pageUrl).resolve(it.attr("data-url")).toString() }, !isDirectMedia(it.attr("data-url")), "$CINEBY/") }
        }
        require(sources.isNotEmpty()) { "The provider returned no playback sources for this selection." }
        val resolved = sources.map { source ->
            val uri = URI(source.url)
            if (uri.host == "cineby.ws" && uri.path.startsWith("/embed/source")) {
                val target = request(source.url).trim()
                source.copy(url = target, embedded = !isDirectMedia(target))
            } else source
        }
        resolved.forEach { require(URI(it.url).scheme == "https" && !URI(it.url).host.isNullOrBlank()) { "The provider returned an invalid or non-HTTPS playback source." } }
        resolved
    }
}

internal fun isDirectMedia(url: String): Boolean = runCatching { URI(url).path.lowercase().let { path -> listOf(".m3u8", ".mpd", ".mp4", ".webm").any { path.endsWith(it) } } }.getOrDefault(false)
internal fun decodeDbkuUrl(value: String, encryption: Int): String = when (encryption) {
    0 -> value
    1 -> URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")
    2 -> URLDecoder.decode(String(Base64.getDecoder().decode(value)).replace("+", "%2B"), "UTF-8")
    else -> error("Unsupported DBKU URL encoding: $encryption")
}
private fun Element.image(): String = selectFirst("img")?.let { it.absUrl("data-original").ifBlank { it.absUrl("src") } }.orEmpty()
private fun Element.link(): String = if (tagName() == "a") absUrl("href") else selectFirst("a[href]")?.absUrl("href").orEmpty()
private fun year(text: String): Int = Regex("(?:19|20)\\d{2}").find(text)?.value?.toIntOrNull() ?: 0
private fun rating(text: String): String = Regex("\\d+(?:\\.\\d+)?").find(text)?.value.orEmpty()
private fun liveTitle(provider: String, url: String, name: String, image: String, year: Int = 0, score: String = "", tv: Boolean = true, featured: Boolean = false, backdrop: String = image, overview: String = "", genres: List<String> = emptyList()) = DesignTitle("$provider:${URI(url).path}", name, "", overview, image, backdrop, year, score, "SOURCE", genres, tv, featured, 0, emptyList(), emptyList(), provider, url)

internal fun parseCinebyHome(html: String, allowEmpty: Boolean = false): List<DesignRow> {
    val doc = Jsoup.parse(html, ProvidersRepository.CINEBY)
    val featured = doc.select(".h-hero-slide").mapNotNull { slide ->
        val url = slide.selectFirst("a[href*=/tv/], a[href*=/movie/]")?.absUrl("href") ?: return@mapNotNull null
        val name = slide.selectFirst(".h-hero-title")?.text().orEmpty()
        if (name.isBlank()) return@mapNotNull null
        liveTitle("cineby", url, name, slide.image(), year(slide.select("time").text()), rating(slide.select(".h-hero-rating").text()), url.contains("/tv/"), true, slide.image(), slide.select(".h-hero-description").text(), slide.select(".h-hero-meta span:not(.meta-sep):not(.h-hero-rating)").eachText())
    }
    val cards = doc.select("article.content-card").mapNotNull { card ->
        val url = card.link(); if (!url.contains("/movie/") && !url.contains("/tv/")) return@mapNotNull null
        liveTitle("cineby", url, card.select(".card-title").text(), card.image(), year(card.select("time").text()), rating(card.select(".rating-badge").text()), url.contains("/tv/"))
    }.distinctBy { it.id }
    require(allowEmpty || featured.isNotEmpty() || cards.isNotEmpty()) { "Cineby catalog markup was not recognized." }
    return listOf(DesignRow("trending", "热门推荐 (Trending Hits)", "Cineby", (featured + cards).distinctBy { it.id }), DesignRow("cineby-movies", "Popular Movies", "Cineby", cards.filter { !it.isTv }), DesignRow("cineby-tv", "Top TV Series", "Cineby", cards.filter { it.isTv })).filter { it.titles.isNotEmpty() }
}

internal fun parseDbkuHome(html: String): List<DesignRow> {
    val doc = Jsoup.parse(html, ProvidersRepository.DBKU)
    val rows = doc.select(".myui-panel").mapNotNull { panel ->
        val heading = panel.selectFirst(".myui-panel__head .title")?.text().orEmpty()
        val cards = panel.select("a.myui-vodlist__thumb[href*=/voddetail/]").map { a ->
            val li = a.closest("li") ?: a
            liveTitle("dbku", a.absUrl("href"), a.attr("title"), a.absUrl("data-original"), score = rating(li.select(".pic-tag-top, .pic-tag").text()), tv = !heading.contains("电影"), genres = listOf(heading))
        }.distinctBy { it.id }
        if (cards.isEmpty() || heading.isBlank()) return@mapNotNull null
        val id = when { heading.contains("陆剧") -> "mainland"; heading.contains("日韩") -> "japan_korea"; heading.contains("台泰") || heading.contains("港台") -> "hk_taiwan"; heading.contains("动漫") -> "anime"; heading.contains("综艺") -> "variety"; heading.contains("电影") -> "dbku-movies"; heading.contains("短剧") -> "dbku-shorts"; else -> "dbku-hot" }
        DesignRow(id, heading, "DBKU", cards)
    }.distinctBy { it.id }
    require(rows.isNotEmpty()) { "DBKU catalog markup was not recognized." }
    return rows
}

internal fun parseCinebyDetails(title: DesignTitle, doc: Document): DesignTitle {
    val json = doc.select("script[type=application/ld+json]").mapNotNull { runCatching { JSONObject(it.data()) }.getOrNull() }.firstOrNull { it.optString("@type") in listOf("Movie", "TVSeries") }
    val episodes = doc.select(".episode-item[data-episode-url]").map { ep ->
        val season = ep.attr("data-season").toIntOrNull() ?: 1
        val number = ep.attr("data-episode").toIntOrNull() ?: 1
        val page = URI(title.pageUrl).resolve(ep.attr("data-episode-url")).toString()
        DesignEpisode(page, number, season, ep.select(".episode-title").text().ifBlank { "Episode $number" }, "", "", 0, "", page)
    }.distinctBy { it.id }
    val seasons = episodes.groupBy { it.season }.map { (number, items) -> DesignSeason(number, "Season $number", items) }
    val actors = json?.optJSONArray("actor")
    val image = json?.opt("image")
    val poster = if (image is JSONObject) image.optString("url") else image?.toString().orEmpty()
    val genres = json?.optJSONArray("genre")
    return title.copy(title = json?.optString("name")?.takeIf { it.isNotBlank() } ?: title.title, overview = json?.optString("description")?.takeIf { it.isNotBlank() } ?: title.overview,
        poster = poster.ifBlank { title.poster }, backdrop = doc.selectFirst("#playerBackdrop img, img[src*=backdrops]")?.absUrl("src")?.takeIf { it.isNotBlank() } ?: title.backdrop,
        year = json?.optString("datePublished")?.let { year(it) }?.takeIf { it > 0 } ?: title.year, rating = json?.optJSONObject("aggregateRating")?.optString("ratingValue")?.takeIf { it.isNotBlank() } ?: title.rating,
        genres = if (genres != null) (0 until genres.length()).map { genres.getString(it) } else title.genres,
        cast = if (actors != null) (0 until actors.length()).map { DesignCast(actors.getJSONObject(it).getString("name"), "", "") } else emptyList(), seasons = seasons)
}

internal fun parseDbkuDetails(title: DesignTitle, doc: Document): DesignTitle {
    val groups = doc.select("ul.myui-content__list").filter { it.select("a[href*=/vodplay/]").isNotEmpty() }.mapIndexed { index, list ->
        val episodes = list.select("a[href*=/vodplay/]").mapIndexed { epIndex, a ->
            val page = a.absUrl("href")
            DesignEpisode(page, epIndex + 1, index + 1, a.text(), "", "", 0, "", page)
        }.distinctBy { it.id }
        DesignSeason(index + 1, if (index == 0) "视频列表" else "视频列表 ${index + 1}", episodes)
    }
    val detail = doc.selectFirst(".myui-content__detail")
    val firstData = detail?.selectFirst("p.data")
    val cast = detail?.select("p.data")?.firstOrNull { it.text().contains("主演：") }?.select("a")?.map { DesignCast(it.text(), "", "") }.orEmpty()
    val category = firstData?.selectFirst("a")?.text().orEmpty()
    return title.copy(title = detail?.select("h1")?.text()?.takeIf { it.isNotBlank() } ?: title.title,
        isTv = if (category.isNotBlank()) category != "电影" else title.isTv,
        overview = doc.select("#desc").text().ifBlank { detail?.select("p.data")?.firstOrNull { it.text().contains("简介：") }?.text().orEmpty() },
        rating = detail?.select(".branch")?.text()?.takeIf { it.isNotBlank() } ?: title.rating,
        year = year(firstData?.text().orEmpty()), poster = doc.selectFirst(".myui-content__thumb img")?.absUrl("data-original")?.takeIf { it.isNotBlank() } ?: title.poster,
        genres = firstData?.select("a")?.take(1)?.map { it.text() }.orEmpty(), cast = cast, seasons = groups,
        playbackPage = groups.firstOrNull()?.episodes?.firstOrNull()?.pageUrl.orEmpty())
}
