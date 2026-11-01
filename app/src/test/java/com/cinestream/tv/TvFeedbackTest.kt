package com.cinestream.tv

import org.junit.Assert.*
import org.junit.Test

class TvFeedbackTest {
    @Test fun hundredsOfEpisodesJumpToCorrectRange() {
        assertEquals(0, episodePage(0)); assertEquals(0, episodePage(49))
        assertEquals(1, episodePage(50)); assertEquals(9, episodePage(499))
    }
    @Test fun dbkuFiltersNeverHideCinebyMatches() {
        val cineby = liveTitle("cineby", "https://cineby.ws/tv/example", "Example", "", year = 2026)
        val dbku = liveTitle("dbku", "https://www.dbku.tv/voddetail/1.html", "剧集", "", year = 2026, genres = listOf("古装")).copy(countries = listOf("大陆"))
        val filters = SearchFilters(country = "大陆", genre = "古装", year = "2026")
        assertEquals(2, filterTitles(listOf(cineby, dbku), filters).size)
        assertEquals(listOf(cineby), filterTitles(listOf(cineby, dbku), filters.copy(country = "日本")))
        assertTrue(filterTitles(listOf(cineby), filters.copy(year = "2025")).isEmpty())
        assertEquals("https://www.dbku.tv/vodshow/2-%E5%A4%A7%E9%99%86--%E5%8F%A4%E8%A3%85--------.html", dbkuFilterUrl(2, filters))
    }
    @Test fun failedSourcesAreTriedOnceThenStop() {
        assertEquals(1, nextSourceIndex(0, setOf(0), 3))
        assertEquals(2, nextSourceIndex(1, setOf(0, 1), 3))
        assertNull(nextSourceIndex(2, setOf(0, 1, 2), 3))
        assertNull(nextSourceIndex(0, setOf(0), 1))
    }
    @Test fun timelineScrubbingClampsAndAccelerates() {
        assertEquals(0L, scrubPosition(1000, 100000, -1, 0))
        assertEquals(100000L, scrubPosition(99000, 100000, 1, 0))
        assertTrue(scrubPosition(20000, 100000, 1, 16) > scrubPosition(20000, 100000, 1, 0))
        assertEquals("1:02:03", mediaClock(3723000))
    }
}
