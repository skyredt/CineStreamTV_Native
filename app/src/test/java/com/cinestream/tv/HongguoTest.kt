package com.cinestream.tv

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.URLDecoder

class HongguoTest {
    private fun fixture(name: String) = javaClass.classLoader!!.getResource(name)!!.readText()
    private fun html(name: String) = "<script>_ROUTER_DATA = ${fixture(name)};</script>"
    private fun title() = parseHongguoHome(html("hongguo-home.json")).flatMap { it.titles }
        .first { it.id == "hongguo:7695727473111092286" }

    @Test fun catalogPreservesChineseAndOpaqueIdsWithoutClaimingLatestOrder() {
        val rows = parseHongguoHome(html("hongguo-home.json"))
        assertEquals(4, rows.size)
        assertTrue(rows.first().title.contains("热播"))
        assertEquals("一个人的长征", title().title)
        assertEquals("hongguo", title().provider)
        assertTrue(title().isShort)
        assertTrue(rows.none { it.title.contains("最新") })
        assertFalse(HONGGUO_CATEGORIES.containsKey("latest"))
        assertEquals(0, title().year)
    }

    @Test fun chineseSearchAndActualPageContractArePreserved() {
        val query = "和离 红果"
        assertTrue(URLDecoder.decode(hongguoSearchUrl(query), "UTF-8").endsWith(query))
        val url = hongguoSearchUrl("和离")
        val result = parseHongguoSearch(fixture("hongguo-search.json"), url)
        assertTrue(result.first.first().title.contains("和离"))
        assertEquals("hongguo:7698294694886788121", result.first.first().id)
        assertEquals("$url&page=2", result.second)
        assertNull(parseHongguoSearch(fixture("hongguo-search.json"), "$url&page=2").second)
        assertThrows(IllegalArgumentException::class.java) { hongguoSearchUrl("", "unknown") }
    }

    @Test fun categoriesFollowPublishedNextLinkOnly() {
        val url = hongguoSearchUrl("")
        val body = html("hongguo-category.json")
        assertNull(parseHongguoSearch(body, url).second)
        assertEquals("$url?page=2", parseHongguoSearch(body + "<a href='/category/real-drama?page=2'>2</a>", url).second)
        assertTrue(parseHongguoSearch(body, url).first.isNotEmpty())
    }

    @Test fun detailsRetainNinetyExactEpisodeIdsAndUnknownAvailability() {
        val detail = parseHongguoDetails(title(), html("hongguo-detail.json"))
        val episodes = detail.seasons.single().episodes
        assertEquals(90, episodes.size)
        assertEquals("hongguo:7695727473111092286:7695729320861387838", episodes.first().id)
        assertEquals((1..90).toList(), episodes.map { it.number })
        assertTrue(episodes.all { it.pageUrl.isBlank() && it.availability.contains("未知") })
        assertTrue(detail.playbackPage.isBlank())
        assertTrue(detail.playbackNotice.contains("可访问 3 集"))
        assertTrue(detail.playbackNotice.contains("未提供逐集对应关系"))
        assertEquals(1, episodePage(episodes.lastIndex))
    }

    @Test fun malformedResponsesAndWrongIdentityFailExplicitly() {
        assertThrows(Exception::class.java) { parseHongguoHome("<html>challenge</html>") }
        assertThrows(Exception::class.java) { parseHongguoSearch("{}", hongguoSearchUrl("中文")) }
        assertThrows(IllegalArgumentException::class.java) { parseHongguoDetails(title().copy(id = "wrong"), html("hongguo-detail.json")) }
        assertThrows(IllegalArgumentException::class.java) { requireProviderPage("hongguo", "https://evil.example/") }
    }

    @Test fun sourcesAreBlockedBeforeAnyRequest() {
        var requests = 0
        val repository = ProvidersRepository { _, _ -> requests++; error("Playback must not request upstream") }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.sources(title(), null) } }
        assertEquals(0, requests)
    }

    @Test fun timeoutLeavesAllThreeExistingSearchProvidersUnchanged() = runBlocking<Unit> {
        val repository = ProvidersRepository { url, _ -> when {
            url.startsWith(ProvidersRepository.HONGGUO) -> throw SocketTimeoutException("catalog timeout")
            url.startsWith(ProvidersRepository.CINEBY) -> fixture("cineby-lanterns.html")
            url.startsWith(ProvidersRepository.BINGEBANG) -> fixture("bingebang-search.json")
            else -> fixture("dbku-chinese.html")
        } }
        val result = repository.searchWithFilters("和离")
        val expected = parseCinebySearch(fixture("cineby-lanterns.html")) + parseBingeList(fixture("bingebang-search.json")) + parseDbkuSearch(fixture("dbku-chinese.html"))
        assertEquals(expected, result.titles)
        assertNotNull(result.statuses.single { it.provider == "hongguo" }.error)
        assertEquals(result.titles.size, result.titles.distinctBy { it.id }.size)
    }

    @Test fun dbkuFiltersDoNotHideHongguoButShortsFilterStillApplies() {
        val item = title()
        assertEquals(listOf(item), filterTitles(listOf(item), SearchFilters(country = "日本", genre = "古装")))
        assertTrue(filterTitles(listOf(item), SearchFilters(mediaType = MediaTypeFilter.MOVIES)).isEmpty())
    }

    @Test fun cancellationPropagates() {
        val repository = ProvidersRepository { _, _ -> throw CancellationException("cancelled") }
        assertThrows(CancellationException::class.java) { runBlocking { repository.searchWithFilters("中文") } }
    }
}
