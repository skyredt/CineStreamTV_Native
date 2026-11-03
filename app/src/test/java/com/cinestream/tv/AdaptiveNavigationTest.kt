package com.cinestream.tv

import org.junit.Assert.*
import org.junit.Test

class AdaptiveNavigationTest {
    @Test fun largePhonesAndTabletsRemainTouchInterfacesAndSmallTelevisionsRemainTv() {
        assertFalse(usesTelevisionInterface(0x01))
        assertFalse(usesTelevisionInterface(0x21)) // phone, night mode
        assertFalse(usesTelevisionInterface(0x03)) // car is not a TV
        assertTrue(usesTelevisionInterface(0x04))
        assertTrue(usesTelevisionInterface(0x24)) // TV, night mode
    }
    @Test fun portraitAndLandscapeGridPoliciesKeepAtLeastTwoReadableColumns() {
        val dimensions = listOf(320 to 640, 360 to 800, 412 to 915, 640 to 320, 800 to 360, 915 to 412, 1280 to 720)
        dimensions.forEach { (width, _) ->
            val columns = mobilePosterColumns(width)
            assertTrue(columns in 2..6)
            assertTrue("Poster cells remain at least 128dp including narrow-phone spacing", (width - 32 - (columns - 1) * 12) / columns >= 128)
        }
        assertEquals(2, mobilePosterColumns(412))
        assertEquals(5, mobilePosterColumns(800))
    }
    @Test fun navigationExposesAllCoreDestinationsOnce() {
        assertEquals(listOf("home", "browse", "search", "history", "settings"), mobileDestinations)
        assertEquals(mobileDestinations.size, mobileDestinations.distinct().size)
    }
    @Test fun phoneCategoriesUseActualProviderFeedsWithoutInventedAvailability() {
        val empty = DesignCatalog(emptyList(), emptyList())
        assertEquals(listOf("home", "movies", "series"), mobileCategories(empty, CatalogueMode.ENGLISH).map { it.id })
        val catalog = DesignCatalog(emptyList(), listOf(DesignRow("mainland", "陆剧", "DBKU", emptyList()), DesignRow("new-releases", "New Releases", "BingeBang", emptyList())))
        assertTrue(mobileCategories(catalog, CatalogueMode.ENGLISH).any { it.id == "new-releases" })
        assertTrue(mobileCategories(catalog, CatalogueMode.ENGLISH).none { it.id == "mainland" })
        assertEquals(listOf("home", "mainland"), mobileCategories(DesignCatalog(emptyList(), catalog.rows.take(1)), CatalogueMode.CHINESE).map { it.id })
    }
    @Test fun episodePagingBoundsRenderedButtonsForLongShows() {
        assertEquals(0, episodePageCount(0))
        assertEquals(1, episodePageCount(1))
        assertEquals(1, episodePageCount(50))
        assertEquals(2, episodePageCount(51))
        assertEquals(40, episodePageCount(2000))
    }
}
