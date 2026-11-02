package com.cinestream.tv

import kotlinx.coroutines.runBlocking
import org.jsoup.HttpStatusException
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException

class CinebySearchReliabilityTest {
    private fun fixture(name: String) = javaClass.classLoader!!.getResource("cineby-16-$name.html")!!.readText()
    @Test fun capturedSmallvilleResponseRetainsProviderIdentity() {
        val title = parseCinebySearch(fixture("smallville")).single()
        assertEquals("Smallville", title.title)
        assertEquals("cineby", title.provider)
        assertTrue(title.isTv)
        assertTrue(title.pageUrl.startsWith("https://cineby.ws/tv/"))
    }
    @Test fun capturedZeroResultResponseIsVerifiedEmpty() { assertTrue(parseCinebySearch(fixture("empty")).isEmpty()) }
    @Test fun capturedBlankQueryIsAnInputPromptNotAnUnknownPage() {
        val error = assertThrows(SearchResponseException::class.java) { parseCinebySearch(fixture("blank")) }
        assertEquals(SearchFailureKind.INPUT_REQUIRED, error.kind)
    }
    @Test fun blankQueriesDoNotRequestCinebyOrRaisePartialFailure() = runBlocking {
        val requested = java.util.Collections.synchronizedList(mutableListOf<String>())
        val repo = ProvidersRepository { url, _ -> requested += url; """{"results":[]}""" }
        val result = repo.searchWithFilters("  ", mode = CatalogueMode.ENGLISH)
        assertTrue(requested.none { it.startsWith(ProvidersRepository.CINEBY) })
        assertTrue(result.statuses.all { it.error == null })
        assertTrue(result.statuses.single { it.provider == "cineby" }.message!!.contains("Enter a title"))
    }
    @Test fun malformedAndContradictoryPagesAreNeverReportedAsEmpty() {
        listOf("", "<html>Maintenance</html>", "<p class='search-results-count'>2 results</p>", "<article class='content-card'>broken card</article>", "<p class='search-results-count'>2 results for 0 results</p>").forEach { html ->
            val error = assertThrows(SearchResponseException::class.java) { parseCinebySearch(html) }
            assertEquals(SearchFailureKind.UNRECOGNIZED, error.kind)
        }
    }
    @Test fun aSearchTitleIsNotMistakenForAnAccessRestriction() {
        val html = fixture("smallville").replace(Regex("<title>.*?</title>"), "<title>Search Results for Access denied - Cineby</title>")
        assertEquals(1, parseCinebySearch(html).size)
    }
    @Test fun accessPageWinsOverAnEmptyLookingCountMarker() {
        val error = assertThrows(SearchResponseException::class.java) { parseCinebySearch("<title>Just a moment</title><form id='challenge-form'></form><p class='search-results-count'>0 results</p>") }
        assertEquals(SearchFailureKind.ACCESS, error.kind)
    }
    @Test fun networkHttpAndParsingFailuresHaveSeparateSafeMessages() {
        assertEquals(SearchFailureKind.TEMPORARY, searchFailureKind(SocketTimeoutException("do-not-display")))
        assertEquals(SearchFailureKind.ACCESS, searchFailureKind(HttpStatusException("do-not-display", 403, "https://cineby.ws/search")))
        assertEquals(SearchFailureKind.ACCESS, searchFailureKind(HttpStatusException("limited", 429, "https://cineby.ws/search")))
        assertEquals(SearchFailureKind.TEMPORARY, searchFailureKind(HttpStatusException("unavailable", 503, "https://cineby.ws/search")))
        assertTrue(SearchFailureKind.entries.all { !searchFailureMessage(it).contains("do-not-display") })
    }
    @Test fun accessAndUnexpectedPagesAlsoKeepHealthyBingeResults() = runBlocking {
        val cases = listOf("<title>Access denied</title>" to SearchFailureKind.ACCESS, "<html>unexpected</html>" to SearchFailureKind.UNRECOGNIZED)
        cases.forEach { (html, kind) ->
            val repo = ProvidersRepository { url, _ -> if (url.startsWith(ProvidersRepository.CINEBY)) html else """{"results":[{"id":1,"media_type":"tv","name":"Smallville","url":"/tv/watch/1-smallville"}]}""" }
            val result = repo.searchWithFilters("Smallville", mode = CatalogueMode.ENGLISH)
            assertEquals(listOf("bingebang"), result.titles.map { it.provider })
            assertEquals(kind, result.statuses.single { it.provider == "cineby" }.failureKind)
        }
    }
    @Test fun timeoutKeepsBingeResultsAndRetryRecoversWithoutNewRepository() = runBlocking {
        var failure = true
        val repo = ProvidersRepository { url, _ ->
            if (url.startsWith(ProvidersRepository.CINEBY)) { if (failure) throw SocketTimeoutException("do-not-display"); fixture("smallville") }
            else """{"results":[{"id":1,"media_type":"tv","name":"Smallville","url":"/tv/watch/1-smallville"}]}"""
        }
        val initial = repo.searchWithFilters("Smallville", mode = CatalogueMode.ENGLISH)
        assertEquals(listOf("bingebang"), initial.titles.map { it.provider })
        assertEquals(SearchFailureKind.TEMPORARY, initial.statuses.single { it.provider == "cineby" }.failureKind)
        assertFalse(initial.statuses.single { it.provider == "cineby" }.error!!.contains("do-not-display"))
        failure = false
        val retry = repo.searchWithFilters("Smallville", mode = CatalogueMode.ENGLISH)
        assertEquals(setOf("cineby", "bingebang"), retry.titles.map { it.provider }.toSet())
        assertTrue(retry.statuses.all { it.error == null })
    }
}
