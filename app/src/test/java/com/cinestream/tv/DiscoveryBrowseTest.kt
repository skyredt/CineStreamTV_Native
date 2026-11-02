package com.cinestream.tv

import org.junit.Assert.*
import org.junit.Test

class DiscoveryBrowseTest {
    private val film = liveTitle("cineby", "https://cineby.ws/movie/1", "Film", "", year = 2025, tv = false, genres = listOf("Drama")).copy(countries = listOf("US"))
    private val show = liveTitle("bingebang", "https://bingebang.st/tv/watch/2", "Show", "", year = 2024, tv = true, genres = listOf("Comedy"))
    private val drama = liveTitle("dbku", "https://www.dbku.tv/voddetail/3.html", "剧", "", tv = true, genres = listOf("陆剧"))
    private val catalog = DesignCatalog(listOf(film, show), listOf(DesignRow("new-releases", "New releases", "", listOf(film)), DesignRow("cineby-movies", "Movies", "", listOf(film)), DesignRow("bingebang-tv", "Trending TV", "", listOf(show))))
    @Test fun discoveryDoesNotRepeatTheSameProviderQualifiedTitleAcrossRows() {
        val rows = discoveryRows(catalog)
        assertEquals(listOf("new-releases", "bingebang-tv"), rows.map { it.id })
        assertEquals(2, rows.flatMap { it.titles }.size)
        assertEquals(listOf(show), discoveryRows(catalog, setOf(film.id)).flatMap { it.titles })
    }
    @Test fun similarNamesFromDifferentProvidersRemainSeparate() {
        val other = film.copy(id = "bingebang:movie:1", provider = "bingebang")
        val rows = discoveryRows(DesignCatalog(listOf(film, other), listOf(DesignRow("one", "Movies", "", listOf(film, other)))))
        assertEquals(2, rows.single().titles.size)
    }
    @Test fun continueIsCatalogueScopedAndDoesNotInventAnExactPosition() {
        val entries = listOf(ViewingEntry(film, null, 0, 1), ViewingEntry(show, "ep2", 0, 2, "Episode 2"), ViewingEntry(drama, "ep1", 12000, 3, "第1集"))
        val english = continuingEntries(entries, CatalogueMode.ENGLISH)
        assertEquals(listOf(show), english.map { it.title })
        assertEquals(0L, english.single().position)
        assertEquals("Episode 2", english.single().episodeLabel)
        assertEquals(listOf(drama), continuingEntries(entries, CatalogueMode.CHINESE).map { it.title })
    }
    @Test fun browseCombinesRealFiltersAndExcludesUnknownValues() {
        assertEquals(listOf(film), browseTitles(catalog, BrowseFilters(provider = "cineby", type = MediaTypeFilter.MOVIES, genre = "Drama", year = "2025", region = "US")))
        assertEquals(listOf(show), browseTitles(catalog, BrowseFilters(type = MediaTypeFilter.DRAMAS)))
        assertTrue(browseTitles(catalog, BrowseFilters(year = "1999")).isEmpty())
        assertTrue(browseTitles(catalog, BrowseFilters(region = "Unknown")).isEmpty())
        assertEquals(listOf(film, show), browseTitles(catalog, BrowseFilters(sort = SortOption.TITLE)))
    }
    @Test fun focusFollowsAPlayedTitleIntoContinueWatching() {
        val saved = listOf(ViewingEntry(show, "ep1", 10000, 1))
        val rows = discoveryRows(catalog, setOf(show.id))
        assertEquals("continue", discoveryFocusRow(show.id, rows, saved))
        assertEquals("new-releases", discoveryFocusRow(film.id, rows, saved))
        assertNull(discoveryFocusRow("missing", rows, saved))
    }
    @Test fun homeBrowseAndCatalogueSwitchingUseDistinctPolicies() {
        assertTrue(mobileDestinations.containsAll(listOf("home", "browse")))
        assertTrue(mobileCategories(catalog, CatalogueMode.ENGLISH).any { it.id == "movies" })
        val chinese = DesignCatalog(listOf(drama), listOf(DesignRow("mainland", "陆剧", "DBKU", listOf(drama))))
        assertEquals(listOf("home", "mainland"), mobileCategories(chinese, CatalogueMode.CHINESE).map { it.id })
        assertEquals(listOf(drama), browseTitles(chinese, BrowseFilters(category = "mainland")))
    }
}
