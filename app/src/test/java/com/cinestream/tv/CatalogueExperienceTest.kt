package com.cinestream.tv

import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test
import java.util.Collections

class CatalogueExperienceTest {
    @Test fun catalogueRequestsAreIsolatedAndAbandonedProviderIsNeverRequested() = runBlocking {
        val requests = Collections.synchronizedList(mutableListOf<String>())
        val repository = ProvidersRepository { url, _ ->
            requests += url
            when {
                url.contains("bingebang") -> """{"results":[]}"""
                url.contains("cineby") -> """<article class="content-card"><a href="/movie/test"><h3 class="card-title">Film</h3></a></article>"""
                else -> """<div class="myui-panel"><div class="myui-panel__head"><h3 class="title">陆剧</h3></div><a class="myui-vodlist__thumb" href="/voddetail/1.html" title="剧"></a></div>"""
            }
        }
        assertTrue(repository.home(CatalogueMode.ENGLISH).catalog.titles.all { CatalogueMode.ENGLISH.accepts(it.provider) })
        assertTrue(requests.none { it.contains("dbku") || it.contains("hongguo") })
        requests.clear()
        assertEquals("dbku", repository.home(CatalogueMode.CHINESE).catalog.titles.single().provider)
        assertEquals(listOf("https://www.dbku.tv/"), requests.toList())
    }
    @Test fun englishSearchKeepsHealthyProviderWhenOtherProviderFails() = runBlocking {
        val requests = Collections.synchronizedList(mutableListOf<String>())
        val repository = ProvidersRepository { url, _ ->
            requests += url
            if (url.contains("cineby")) error("Unavailable")
            """{"results":[{"id":12,"media_type":"movie","title":"Film","url":"/movie/watch/12-film"}]}"""
        }
        val result = repository.searchWithFilters("Film", mode = CatalogueMode.ENGLISH)
        assertEquals("bingebang", result.titles.single().provider)
        assertEquals(2, result.statuses.size)
        assertEquals(1, result.statuses.count { it.error != null })
        assertTrue(requests.none { it.contains("dbku") || it.contains("hongguo") })
    }
    @Test fun chineseSearchNeverContactsEnglishProvidersEvenForPagination() = runBlocking {
        val requests = mutableListOf<String>()
        val repository = ProvidersRepository { url, _ -> requests += url; "<ul id='searchList'></ul>" }
        repository.searchWithFilters("剧", mode = CatalogueMode.CHINESE)
        assertEquals(1, requests.size)
        assertTrue(requests.single().contains("dbku.tv"))
        requests.clear()
        repository.searchWithFilters("剧", page = 2, nextPages = mapOf("cineby" to "https://cineby.ws/search", "hongguo" to "https://hongguoduanju.com/"), mode = CatalogueMode.CHINESE)
        assertTrue(requests.isEmpty())
    }
    @Test fun liveReferenceMetadataAndEpisodesAreParsedRatherThanHardcoded() {
        val html = javaClass.classLoader!!.getResource("dbku-reference-detail.html")!!.readText()
        val title = liveTitle("dbku", "https://www.dbku.tv/voddetail/1951.html", "", "")
        val detail = parseDbkuDetails(title, Jsoup.parse(html, title.pageUrl))
        assertEquals("斗罗大陆", detail.title)
        assertEquals(2021, detail.year)
        assertEquals("8.9", detail.rating)
        assertEquals(listOf("大陆"), detail.countries)
        assertEquals(listOf("杨振宇"), detail.directors)
        assertEquals("2021-02-21 09:40:35", detail.updatedAt)
        assertTrue(detail.cast.any { it.name == "肖战" })
        assertEquals(40, detail.seasons.first().episodes.size)
        val changed = parseDbkuDetails(title, Jsoup.parse(html.replace("杨振宇", "Other Director"), title.pageUrl))
        assertEquals(listOf("Other Director"), changed.directors)
    }
    @Test fun movieAndSeriesCategoriesDoNotLeakOtherTypesOrMergeTitles() {
        val movie = liveTitle("cineby", "https://cineby.ws/movie/1", "Same title", "", tv = false)
        val series = liveTitle("bingebang", "https://bingebang.st/tv/watch/1", "Same title", "", tv = true)
        val catalog = DesignCatalog(listOf(movie, series), listOf(DesignRow("row", "Catalogue", "", listOf(movie, series))))
        assertEquals(listOf(movie), catalogueRows(catalog, "movies").single().titles)
        assertEquals(listOf(series), catalogueRows(catalog, "series").single().titles)
        assertEquals(2, catalogueRows(catalog, "home").single().titles.size)
    }
}
