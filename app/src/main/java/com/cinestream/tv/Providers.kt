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

/** Public page adapters for the user-selected providers. No guessed stream API or sample fallback. */
class ProvidersRepository(private val fetchPage: ((String, Boolean) -> String)? = null) {
    companion object {
        const val CINEBY = "https://cineby.ws"
        const val DBKU = "https://www.dbku.tv"
        const val BINGEBANG = "https://bingebang.st"
        const val HONGGUO = "https://hongguoduanju.com"
        const val AGENT = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36 CineStream/1.1"
    }
    private val detailsCache = mutableMapOf<String, DesignTitle>()
    private fun request(url: String, ajax: Boolean = false): String {
        require(URI(url).scheme == "https") { "Provider returned a non-HTTPS URL" }
        fetchPage?.let { return it(url, ajax) }
        val connection = Jsoup.connect(url).userAgent(AGENT).timeout(20000).maxBodySize(3 * 1024 * 1024).ignoreContentType(true)
        if (ajax) connection.header("X-Requested-With", "XMLHttpRequest").header("Accept", "application/json")
        return connection.execute().body()
    }
    suspend fun home(mode: CatalogueMode? = null): LiveCatalog = withContext(Dispatchers.IO) {
        val warnings = mutableListOf<String>()
        val cine = async { runCatching { if (mode == CatalogueMode.CHINESE) emptyList() else parseCinebyHome(request("$CINEBY/home")) } }
        val dbku = async { runCatching { if (mode == CatalogueMode.ENGLISH) emptyList() else parseDbkuHome(request("$DBKU/")) } }
        val binge = async { runCatching {
            if (mode == CatalogueMode.CHINESE) emptyList() else listOf("movie" to "BingeBang Trending Movies", "tv" to "BingeBang Trending TV").map { (type, name) ->
                DesignRow("bingebang-$type", name, "BingeBang", parseBingeList(request("$BINGEBANG/api/list/trending_${type}_week?limit=24")))
            }.filter { it.titles.isNotEmpty() }
        } }
        val newReleases = async { runCatching {
            if (mode == CatalogueMode.CHINESE) emptyList() else listOf(DesignRow("new-releases", "BingeBang New Releases", "BingeBang", parseBingeList(request("$BINGEBANG/api/list/movie_now_playing?limit=24"))))
        } }
        val rows = newReleases.await().getOrElse { if (it is CancellationException) throw it; warnings += "BingeBang new releases: ${it.message}"; emptyList() } + cine.await().getOrElse { if (it is CancellationException) throw it; warnings += "Cineby: ${it.message}"; emptyList() } + dbku.await().getOrElse { if (it is CancellationException) throw it; warnings += "DBKU: ${it.message}"; emptyList() } + binge.await().getOrElse { if (it is CancellationException) throw it; warnings += "BingeBang: ${it.message}"; emptyList() }
        val availableRows = rows.filter { it.titles.isNotEmpty() }
        if (availableRows.isEmpty()) error(warnings.joinToString("\n").ifBlank { "The providers returned no catalog entries." })
        val titles = availableRows.flatMap { it.titles }.distinctBy { it.id }.toMutableList()
        if (titles.none { it.featured }) titles[0] = titles[0].copy(featured = true)
        LiveCatalog(DesignCatalog(titles, availableRows), warnings)
    }
    suspend fun details(title: DesignTitle): DesignTitle = withContext(Dispatchers.IO) {
        synchronized(detailsCache) { detailsCache[title.id] } ?: run {
            val doc = Jsoup.parse(request(title.pageUrl), title.pageUrl)
            val value = when (title.provider) {
                "hongguo" -> parseHongguoDetails(title, doc.outerHtml())
                "bingebang" -> {
                    val data = JSONObject(doc.selectFirst("script[data-detail-payload]")?.data() ?: error("BingeBang detail payload was not found."))
                    val detail = parseBingeDetails(title, data)
                    val available = data.optJSONArray("seasons")
                    val seasons = if (!detail.isTv || available == null) emptyList() else coroutineScope {
                        val permits = kotlinx.coroutines.sync.Semaphore(4)
                        (0 until available.length()).map { available.getJSONObject(it) }
                            .filter { it.optInt("available_episodes") > 0 }.map { season -> async {
                                permits.acquire()
                                try {
                                    val number = season.getInt("season_number")
                                    val episodes = parseBingeEpisodes(request("$BINGEBANG/api/tv/${data.getLong("id")}/season/$number"), number, detail.playbackPage)
                                    DesignSeason(number, season.optString("name", "Season $number"), episodes)
                                } finally { permits.release() }
                            } }.awaitAll()
                    }
                    detail.copy(seasons = seasons)
                }
                "dbku" -> parseDbkuDetails(title, doc)
                else -> parseCinebyDetails(title, doc)
            }
            synchronized(detailsCache) { detailsCache[title.id] = value }; value
        }
    }
    suspend fun searchWithFilters(query: String, filters: SearchFilters = SearchFilters(), page: Int = 1, nextPages: Map<String, String> = emptyMap(), mode: CatalogueMode? = null): SearchResults = withContext(Dispatchers.IO) {
        val value = URLEncoder.encode(query.trim(), "UTF-8")
        val dbkuBrowse = filters.genre != "All" || filters.country != "All"
        val initial = mutableMapOf("cineby" to "$CINEBY/search?q=$value&type=all", "bingebang" to "$BINGEBANG/api/search/multi?query=$value")
        if (dbkuBrowse) {
            val types = when (filters.mediaType) { MediaTypeFilter.MOVIES -> listOf(1); MediaTypeFilter.DRAMAS -> listOf(2, 4); MediaTypeFilter.SHORTS -> listOf(21); else -> listOf(1, 2, 4, 21) }
            types.forEach { type -> initial["dbku:$type"] = dbkuFilterUrl(type, filters) }
        } else initial["dbku"] = "$DBKU/vodsearch/-------------.html?wd=$value"
        val requests = (if (page == 1) initial else nextPages).filterKeys { key -> key != "hongguo" && (mode == null || mode.accepts(key.substringBefore(':'))) }
        val metadataNeeded = filters.genre != "All" || filters.country != "All" || filters.mediaType != MediaTypeFilter.ALL
        val permits = kotlinx.coroutines.sync.Semaphore(4)
        val responses = requests.map { (provider, url) -> async {
            try {
                requireProviderPage(provider, url)
                val html = request(url)
                val doc = Jsoup.parse(html, url)
                val hongguoResult = if (provider == "hongguo") parseHongguoSearch(html, url) else null
                var titles = if (hongguoResult != null) hongguoResult.first else if (provider == "bingebang") parseBingeList(html) else if (provider == "cineby") parseCinebySearch(html) else if (provider.startsWith("dbku:")) parseDbkuBrowse(doc, provider.substringAfter(':').toInt(), filters).filter { query.isBlank() || it.title.contains(query.trim(), true) } else parseDbkuSearch(html)
                var metadataFailures = 0
                if (provider.startsWith("dbku") && ((metadataNeeded && !dbkuBrowse) || filters.year != "All" || filters.sort == SortOption.YEAR)) titles = coroutineScope { titles.map { title -> async {
                    permits.acquire()
                    try { details(title) } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { synchronized(permits) { metadataFailures++ }; title }
                    finally { permits.release() }
                } }.awaitAll() }
                Triple(titles, ProviderSearchStatus(provider, titles.size, message = if (provider == "hongguo") "红果：暂不支持播放；新剧排序与逐集锁定状态未提供。分类仅用于空关键词浏览。" else if (metadataFailures > 0) "$metadataFailures title details unavailable" else null), if (provider == "hongguo") hongguoResult?.second else if (provider == "bingebang") null else nextSearchPage(doc, provider, url))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Triple(emptyList(), ProviderSearchStatus(provider, 0, error = e.message ?: "Request failed"), url.takeIf { page > 1 }) }
        } }.awaitAll()
        val statuses = responses.map { it.second }
        val titles = responses.flatMap { it.first }.distinctBy { it.id }
        val next = responses.mapNotNull { it.third?.let { url -> it.second.provider to url } }.toMap()
        SearchResults(sortTitles(filterTitles(titles, filters), filters.sort), statuses,
            if (filters.isActive) "Country and Chinese genre: DBKU only. In this mode DBKU searches title text within filtered catalog pages; load more for older titles. Cineby and BingeBang ignore these two filters. BingeBang returns one search batch; remote pagination is not provided. Other filters/sorting apply to loaded results." else null, next)
    }

    suspend fun search(query: String): List<DesignTitle> = searchWithFilters(query).titles

    suspend fun sources(title: DesignTitle, episode: DesignEpisode?): List<ProviderStream> = withContext(Dispatchers.IO) {
        require(title.provider != "hongguo") { HONGGUO_NOTICE }
        val detail = details(title)
        if (title.provider == "bingebang") return@withContext listOf(bingeStream(detail, episode))
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
internal fun liveTitle(provider: String, url: String, name: String, image: String, year: Int = 0, score: String = "", tv: Boolean = true, featured: Boolean = false, backdrop: String = image, overview: String = "", genres: List<String> = emptyList()) = DesignTitle("$provider:${URI(url).path}", name, "", overview, image, backdrop, year, score, "SOURCE", genres, tv, featured, 0, emptyList(), emptyList(), provider, url)

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
    return listOf(DesignRow("trending", "Cineby Featured & Catalogue", "Cineby", (featured + cards).distinctBy { it.id }), DesignRow("cineby-movies", "Cineby Movies", "Cineby", cards.filter { !it.isTv }), DesignRow("cineby-tv", "Cineby TV Series", "Cineby", cards.filter { it.isTv })).filter { it.titles.isNotEmpty() }
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
        val id = when { heading.contains("陆剧") -> "mainland"; heading.contains("日韩") -> "japan_korea"; heading.contains("台泰") -> "taiwan_thailand"; heading.contains("港台") -> "hk_taiwan"; heading.contains("动漫") -> "anime"; heading.contains("综艺") -> "variety"; heading.contains("电影") -> "dbku-movies"; heading.contains("短剧") -> "dbku-shorts"; else -> "dbku-hot" }
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
        countries = json?.opt("countryOfOrigin")?.let { value -> when (value) {
            is JSONObject -> listOf(value.optString("name")); is org.json.JSONArray -> (0 until value.length()).map { index -> val item = value.get(index); if (item is JSONObject) item.optString("name") else item.toString() }; else -> listOf(value.toString())
        } }?.filter { it.isNotBlank() } ?: title.countries,
        originalTitle = json?.optString("alternateName").orEmpty(),
        directors = json?.optJSONArray("director")?.let { names -> (0 until names.length()).mapNotNull { names.optJSONObject(it)?.optString("name") } }.orEmpty(),
        ratingSource = "Cineby",
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
    val publishedGenres = doc.select("meta[name=keywords]").attr("content").split(',', '，', '、').map { it.trim() }.filter { it in FilterOptions.GENRES && it != "All" }
    return title.copy(title = detail?.select("h1")?.text()?.takeIf { it.isNotBlank() } ?: title.title,
        isTv = if (category.isNotBlank()) category != "电影" else title.isTv,
        overview = doc.select("#desc").text().ifBlank { detail?.select("p.data")?.firstOrNull { it.text().contains("简介：") }?.text().orEmpty() },
        rating = detail?.select(".branch")?.text()?.takeIf { it.isNotBlank() } ?: title.rating,
        year = year(firstData?.text().orEmpty()), poster = doc.selectFirst(".myui-content__thumb img")?.absUrl("data-original")?.takeIf { it.isNotBlank() } ?: title.poster,
        genres = (listOfNotNull(category.takeIf { it.isNotBlank() }) + doc.select(".myui-content__detail a[href*=/vodshow/]").map { it.text() } + publishedGenres + title.genres).distinct(), countries = firstData?.select("a")?.getOrNull(1)?.text()?.let { listOf(it) } ?: title.countries, isShort = category.contains("短剧"), cast = cast, seasons = groups,
        directors = detail?.select("p.data")?.firstOrNull { it.text().contains("导演：") }?.select("a")?.eachText().orEmpty(),
        updatedAt = detail?.select("p.data")?.firstOrNull { it.text().contains("更新：") }?.text()?.substringAfter("更新：")?.trim().orEmpty(),
        ratingSource = "DBKU",
        playbackPage = groups.firstOrNull()?.episodes?.firstOrNull()?.pageUrl.orEmpty())
}


internal fun requireProviderPage(provider: String, url: String) {
    val uri = URI(url)
    val host = when (provider) { "cineby" -> "cineby.ws"; "bingebang" -> "bingebang.st"; "hongguo" -> "hongguoduanju.com"; else -> "www.dbku.tv" }
    require(uri.scheme == "https" && uri.host == host && uri.userInfo == null) { "Invalid provider page URL" }
}
internal fun nextSearchPage(doc: Document, provider: String, current: String): String? {
    val next = doc.select("a[rel=next], .pagination a, .myui-page a").firstOrNull {
        it.attr("rel") == "next" || it.text().trim() in listOf("Next", "Next Page", "下一页", "»", "›")
    } ?: return null
    if (next.hasClass("disabled") || next.parent()?.hasClass("disabled") == true) return null
    val url = next.absUrl("href")
    if (url.isBlank() || url == current || next.attr("href").startsWith("#")) return null
    return url.takeIf { runCatching { requireProviderPage(provider, it) }.isSuccess }
}
internal fun parseCinebySearch(html: String): List<DesignTitle> {
    val doc = Jsoup.parse(html, ProvidersRepository.CINEBY)
    val cards = doc.select("article.content-card")
    require(cards.isNotEmpty() || doc.selectFirst(".search-results-count") != null) { "Cineby search markup was not recognized; this is not a confirmed empty result." }
    return cards.mapNotNull { card ->
        val a = card.selectFirst("a[href*=/movie/], a[href*=/tv/]") ?: return@mapNotNull null
        val name = card.select(".card-title").text().ifBlank { a.attr("title") }
        if (name.isBlank()) return@mapNotNull null
        liveTitle("cineby", a.absUrl("href"), name, card.image(), year(card.select("time").text()), rating(card.select(".rating-badge").text()), URI(a.absUrl("href")).path.startsWith("/tv/"))
    }.distinctBy { it.id }
}
internal fun parseDbkuSearch(html: String): List<DesignTitle> {
    val doc = Jsoup.parse(html, ProvidersRepository.DBKU)
    val list = doc.selectFirst("#searchList")
    require(list != null || doc.text().contains("没有找到") || doc.text().contains("暂无")) { "DBKU search markup was not recognized; this is not a confirmed empty result." }
    return list?.select("a.myui-vodlist__thumb[href*=/voddetail/]")?.map { a ->
        val li = a.closest("li") ?: a
        val meta = li.select("p").firstOrNull { it.text().contains("分类：") }?.text().orEmpty()
        val category = Regex("分类：\\s*(.*?)\\s*地区：").find(meta)?.groupValues?.get(1).orEmpty()
        val country = Regex("地区：\\s*(.*?)\\s*年份：").find(meta)?.groupValues?.get(1).orEmpty()
        liveTitle("dbku", a.absUrl("href"), a.attr("title"), a.absUrl("data-original"), year(meta), rating(li.select(".pic-tag-top").text()), !category.contains("电影"), genres = listOfNotNull(category.takeIf { it.isNotBlank() }))
            .copy(countries = listOfNotNull(country.takeIf { it.isNotBlank() }), isShort = category.contains("短剧"))
    }?.distinctBy { it.id }.orEmpty()
}
