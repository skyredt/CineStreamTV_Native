package com.cinestream.tv

import java.util.Base64
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class ProviderMappingTest {
    @Test fun cinebyCatalogAndEpisodeLinksRetainProviderIdentity() {
        val home = """<div class="h-hero-slide"><img src="/backdrop.webp"><p class="h-hero-title">Example Show</p><time>2026</time><span class="h-hero-rating">8.1</span><a href="/tv/abc-example">Details</a></div><article class="content-card"><a href="/movie/def-film"><img src="/poster.webp"><h3 class="card-title">Example Film</h3><time>2025</time><span class="rating-badge">★ 7.1</span></a></article>"""
        val rows = parseCinebyHome(home)
        val title = rows.first().titles.first()
        assertEquals("https://cineby.ws/tv/abc-example", title.pageUrl)
        assertEquals("cineby", title.provider)
        assertTrue(title.featured)
        val detail = Jsoup.parse("""<script type="application/ld+json">{"@type":"TVSeries","name":"Example Show","description":"Synopsis","numberOfSeasons":2}</script><a class="episode-item" data-season="2" data-episode="3" data-episode-url="/tv/abc/episode/2/3"><span class="episode-title">Third episode</span></a>""", title.pageUrl)
        val episode = parseCinebyDetails(title, detail).seasons.single().episodes.single()
        assertEquals(2, episode.season); assertEquals(3, episode.number)
        assertEquals("https://cineby.ws/tv/abc/episode/2/3", episode.pageUrl)
        assertEquals("Third episode", episode.title)
    }
    @Test fun dbkuCatalogAndEpisodeLinksAreMappedWithoutInventingStreams() {
        val html = """<div class="myui-panel"><div class="myui-panel__head"><h3 class="title">陆剧</h3></div><ul><li><a class="myui-vodlist__thumb" href="/voddetail/123.html" title="Example" data-original="https://images.example/poster.jpg"></a></li></ul></div>"""
        val row = parseDbkuHome(html).single()
        assertEquals("mainland", row.id)
        val title = row.titles.single()
        val doc = Jsoup.parse("""<div class="myui-content__detail"><h1>Example</h1><p class="data">分类：陆剧 年份：2025</p><span class="branch">8.0</span></div><ul class="myui-content__list"><li><a href="/vodplay/123-1-2.html">第2集</a></li></ul>""", title.pageUrl)
        val detail = parseDbkuDetails(title, doc)
        assertEquals("https://www.dbku.tv/vodplay/123-1-2.html", detail.playbackPage)
        assertEquals("第2集", detail.seasons.single().episodes.single().title)
        assertEquals("dbku", detail.provider)
    }
    @Test fun publishedDbkuEncodingIsDecodedAndUnsupportedEncodingFails() {
        val encoded = Base64.getEncoder().encodeToString("https%3A//media.example/episode.m3u8?token=a+b".toByteArray())
        assertEquals("https://media.example/episode.m3u8?token=a+b", decodeDbkuUrl(encoded, 2))
        assertThrows(IllegalStateException::class.java) { decodeDbkuUrl("bad", 8) }
        assertTrue(isDirectMedia("https://media.example/a.m3u8?token=x"))
        assertFalse(isDirectMedia("https://player.example/embed?url=a.mp4"))
    }
    @Test fun changedCatalogMarkupIsAnErrorRatherThanSampleFallback() {
        assertThrows(IllegalArgumentException::class.java) { parseCinebyHome("<html>changed</html>") }
        assertThrows(IllegalArgumentException::class.java) { parseDbkuHome("<html>changed</html>") }
        assertTrue(parseCinebyHome("<html>no search results</html>", true).isEmpty())
    }
    @Test fun cinebySearchParsesSearchResultsWithBothMovieAndTvBadges() {
        val searchHtml = """
            <div class="search-results">
              <div class="card">
                <span class="rating-badge">★ 5.0</span>
                <a href="/movie/valley-of-lanterns-2018" title="Valley of Lanterns">
                  <img src="https://cineby.ws/images/valley.jpg" alt="Valley of Lanterns">
                </a>
              </div>
              <div class="card">
                <span class="badge">TV</span>
                <span class="rating-badge">★ 8.2</span>
                <a href="/tv/lanterns-2026" title="LANTERNS">
                  <img src="https://cineby.ws/images/lanterns.jpg" alt="LANTERNS">
                </a>
              </div>
            </div>
        """.trimIndent()
        val results = parseCinebySearch(searchHtml)
        assertEquals(2, results.size)
        val movie = results.find { it.title.contains("Valley of Lanterns") }
        assertNotNull(movie)
        assertFalse(movie!!.isTv)
        assertEquals("5.0", movie.rating)

        val series = results.find { it.title.contains("LANTERNS") }
        assertNotNull(series)
        assertTrue(series!!.isTv)
        assertEquals("8.2", series.rating)
        assertEquals("https://cineby.ws/tv/lanterns-2026", series.pageUrl)
    }
}
