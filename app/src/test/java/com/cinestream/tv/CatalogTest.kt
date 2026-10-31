package com.cinestream.tv
import org.junit.Assert.*
import org.junit.Test
class CatalogTest {
    @Test fun urlsAreValidatedWithoutFallback() {
        assertNull(MediaUrl.validate("https://example.com/video.m3u8?token=x"))
        listOf("", "http://example.com/a.mp4", "file:///a", "https://user:secret@example.com/a", "https:///a").forEach { assertNotNull(MediaUrl.validate(it)) }
    }
    @Test fun catalogAndEpisodeIdsAreUniqueAndExplicitlySamples() {
        val titles = CatalogRepository().catalog()
        assertEquals(titles.size, titles.map { it.id }.toSet().size)
        val episodes = titles.flatMap { it.episodes }
        assertEquals(episodes.size, episodes.map { it.id }.toSet().size)
        titles.forEach { assertTrue(it.description.startsWith("Demo catalog")) }
        episodes.forEach { assertNull(MediaUrl.validate(it.url)); assertTrue(it.title.contains("sample")) }
    }
}
