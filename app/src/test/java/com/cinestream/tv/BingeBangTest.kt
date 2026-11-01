package com.cinestream.tv

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException

class BingeBangTest {
    private fun fixture(name: String) = javaClass.classLoader!!.getResource(name)!!.readText()
    private fun list(data: JSONObject) = parseBingeList(JSONObject().put("results", JSONArray().put(data)).toString()).single()

    @Test fun capturedSearchKeepsMovieAndTvIdentitiesAndDeduplicates() {
        val json = JSONObject(fixture("bingebang-search.json"))
        val results = json.getJSONArray("results")
        results.put(results.getJSONObject(0))
        results.put(JSONObject().put("media_type", "person"))
        val titles = parseBingeList(json.toString())
        assertEquals(4, titles.size)
        assertEquals("bingebang:tv:21696", titles.first().id)
        assertEquals("Lanterns", titles.first().title)
        assertEquals(2026, titles.first().year)
        assertTrue(titles.first().isTv)
        assertFalse(titles[1].isTv)
        assertTrue(parseBingeList("{\"results\":[]}").isEmpty())
        assertThrows(Exception::class.java) { parseBingeList("<html>challenge</html>") }
        assertThrows(Exception::class.java) { parseBingeList("{\"error\":\"unavailable\"}") }
    }

    @Test fun capturedMovieDetailsUsePublishedPlaybackPage() {
        val data = JSONObject(fixture("bingebang-movie.json"))
        val detail = parseBingeDetails(list(data), data)
        assertEquals("Obsession", detail.title)
        assertEquals(109, detail.minutes)
        assertTrue(detail.genres.isNotEmpty())
        assertTrue(detail.cast.isNotEmpty())
        val stream = bingeStream(detail, null)
        assertEquals("https://bingebang.st/movie/play/obsession-2026-fzqb1g", stream.url)
        assertEquals("https://bingebang.st/", stream.referer)
        assertTrue(stream.embedded)
        assertThrows(IllegalArgumentException::class.java) { parseBingeDetails(detail.copy(id = "wrong"), data) }
    }

    @Test fun repositoryDiscoversCompleteSeasonAndKeepsExactSelectedEpisode() = runBlocking<Unit> {
        val data = JSONObject(fixture("bingebang-tv.json"))
        val repository = ProvidersRepository { url, _ ->
            when {
                url.endsWith("/api/tv/21696/season/1") -> fixture("bingebang-season.json")
                url.endsWith("/tv/watch/lanterns-sne6ck") -> "<script type='application/json' data-detail-payload>$data</script>"
                else -> error("Unexpected request: $url")
            }
        }
        val detail = repository.details(list(data))
        val episodes = detail.seasons.single().episodes
        assertEquals(8, episodes.size)
        assertEquals("Pilot", episodes.first().title)
        assertEquals("Dirt and Stars", episodes.last().title)
        assertEquals("https://bingebang.st/tv/play/lanterns-sne6ck/1/8", repository.sources(detail, episodes.last()).single().url)
        assertThrows(IllegalArgumentException::class.java) { bingeStream(detail, episodes.last().copy(pageUrl = "https://bingebang.st/tv/play/other/1/8")) }
    }

    @Test fun wrongHostProtocolAndEpisodeIdentityAreRejected() {
        listOf("http://bingebang.st/tv/watch/example", "https://evil.example/tv/watch/example", "https://user@bingebang.st/tv/watch/example").forEach {
            assertThrows(IllegalArgumentException::class.java) { bingePage(it) }
        }
        assertThrows(IllegalArgumentException::class.java) { requireProviderPage("bingebang", "https://www.dbku.tv/") }
        assertThrows(IllegalArgumentException::class.java) { parseBingeEpisodes(fixture("bingebang-season.json"), 2, "https://bingebang.st/tv/play/lanterns-sne6ck") }
    }

    @Test fun dbkuOnlyFiltersDoNotHideBingeMatchesAndOtherFiltersStillApply() {
        val titles = parseBingeList(fixture("bingebang-search.json"))
        val filters = SearchFilters(country = "大陆", genre = "古装", year = "2026", mediaType = MediaTypeFilter.DRAMAS)
        assertEquals(listOf(titles.first()), filterTitles(titles, filters))
        assertTrue(filterTitles(titles, filters.copy(year = "1990")).isEmpty())
        assertTrue(filterTitles(titles, filters.copy(mediaType = MediaTypeFilter.SHORTS)).isEmpty())
    }

    @Test fun timeoutIsIsolatedAndExistingSearchResultsAreUnchanged() = runBlocking<Unit> {
        val repository = ProvidersRepository { url, _ -> when {
            url.startsWith(ProvidersRepository.BINGEBANG) -> throw SocketTimeoutException("BingeBang timeout")
            url.startsWith(ProvidersRepository.CINEBY) -> fixture("cineby-lanterns.html")
            else -> fixture("dbku-chinese.html")
        } }
        val result = repository.searchWithFilters("Lanterns")
        val expected = parseCinebySearch(fixture("cineby-lanterns.html")) + parseDbkuSearch(fixture("dbku-chinese.html"))
        assertEquals(expected, result.titles)
        assertNotNull(result.statuses.single { it.provider == "bingebang" }.error)
        assertEquals(result.titles.size, result.titles.distinctBy { it.id }.size)
        assertTrue(result.titles.none { it.provider == "bingebang" || it.provider.isBlank() })
    }

    @Test fun unifiedSearchUsesLiveBingeBatchWithoutInventingPagination() = runBlocking<Unit> {
        val repository = ProvidersRepository { url, _ -> when {
            url.startsWith(ProvidersRepository.BINGEBANG) -> fixture("bingebang-search.json")
            url.startsWith(ProvidersRepository.CINEBY) -> fixture("cineby-lanterns.html")
            else -> fixture("dbku-chinese.html")
        } }
        val result = repository.searchWithFilters("Lanterns")
        assertEquals(4, result.statuses.single { it.provider == "bingebang" }.count)
        assertEquals(4, result.titles.count { it.provider == "bingebang" })
        assertFalse(result.nextPages.containsKey("bingebang"))
    }

    @Test fun failedBingeCatalogDoesNotRemoveExistingProviderRows() = runBlocking<Unit> {
        val repository = ProvidersRepository { url, _ -> when {
            url.startsWith(ProvidersRepository.BINGEBANG) -> throw SocketTimeoutException("catalog timeout")
            url.startsWith(ProvidersRepository.CINEBY) -> "<article class='content-card'><a href='/movie/example'><h3 class='card-title'>Film</h3></a></article>"
            else -> "<div class='myui-panel'><div class='myui-panel__head'><h3 class='title'>陆剧</h3></div><a class='myui-vodlist__thumb' href='/voddetail/1.html' title='剧集'></a></div>"
        } }
        val result = repository.home()
        assertEquals(setOf("cineby", "dbku"), result.catalog.titles.map { it.provider }.toSet())
        assertEquals(1, result.warnings.size)
        assertTrue(result.warnings.single().startsWith("BingeBang:"))
    }

    @Test fun cancellationIsNotConvertedToAnEmptyProviderResult() {
        val repository = ProvidersRepository { _, _ -> throw CancellationException("cancelled") }
        assertThrows(CancellationException::class.java) { runBlocking { repository.searchWithFilters("Lanterns") } }
    }
}
