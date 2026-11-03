package com.cinestream.tv

/** UI_MODE_TYPE_TELEVISION, deliberately independent of screen size or input hardware. */
internal fun usesTelevisionInterface(uiMode: Int): Boolean = uiMode and 0x0f == 0x04
internal fun mobilePosterColumns(widthDp: Int): Int = (widthDp / 144).coerceIn(2, 6)
internal fun episodePageCount(count: Int, pageSize: Int = 50): Int = (count.coerceAtLeast(0) + pageSize - 1) / pageSize

internal data class MobileCategory(val id: String, val label: String)
internal val mobileDestinations = listOf("home", "browse", "search", "history", "settings")
internal fun mobileCategories(catalog: DesignCatalog, mode: CatalogueMode): List<MobileCategory> =
    (listOf(MobileCategory("home", "All")) + if (mode == CatalogueMode.ENGLISH) {
        listOf(MobileCategory("movies", "Movies"), MobileCategory("series", "TV Series")) +
            catalog.rows.filter { it.id.startsWith("bingebang-") || it.id == "new-releases" }.map { MobileCategory(it.id, it.title) }
    } else catalog.rows.map { MobileCategory(it.id, it.title) }).distinctBy { it.id }
