package com.cinestream.tv

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class DesignCatalogTest {
    private val json = File("src/main/assets/catalog.json").readText()
    private val catalog = DesignCatalog.parse(json)

    @Test fun suppliedHeroAndCatalogArePreserved() {
        assertEquals(17, catalog.titles.size)
        val hero = catalog.titles.first { it.featured }
        assertEquals("dune-2", hero.id)
        assertEquals("Dune: Part Two", hero.title)
        assertEquals("Long live the fighters.", hero.tagline)
        assertEquals("4K HDR", hero.quality)
        assertEquals("8.6", hero.rating)
        assertTrue(hero.backdrop.startsWith("file:///android_asset/artwork/"))
        assertEquals(6, catalog.rows.size)
        assertEquals(hero.id, catalog.rows.first().titles.first().id)
    }

    @Test fun episodeMappingKeepsSelectionAndRemovesUnrelatedStreams() {
        assertFalse(json.contains("streamSource"))
        assertFalse(json.contains("mux.dev"))
        val episodes = catalog.titles.flatMap { title -> title.seasons.flatMap { it.episodes } }
        assertTrue(episodes.isNotEmpty())
        assertEquals(episodes.size, episodes.map { it.id }.toSet().size)
        catalog.titles.forEach { title -> title.seasons.forEach { season -> season.episodes.forEach { assertEquals(season.number, it.season) } } }
    }
}
