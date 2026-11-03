package com.cinestream.tv

data class BrowseFilters(val category: String = "home", val provider: String = "All", val type: MediaTypeFilter = MediaTypeFilter.ALL,
    val genre: String = "All", val year: String = "All", val region: String = "All", val sort: SortOption = SortOption.DEFAULT)

internal fun discoveryRows(catalog: DesignCatalog, excludedIds: Set<String> = emptySet()): List<DesignRow> {
    val used = excludedIds.toMutableSet()
    // Cineby's aggregate row repeats its movie/TV rows. Use the specific sections when supplied.
    val hasSpecific = catalog.rows.any { it.id == "cineby-movies" || it.id == "cineby-tv" }
    return catalog.rows.filterNot { hasSpecific && it.id == "trending" }.mapNotNull { row ->
        val titles = row.titles.distinctBy { it.id }.filter { used.add(it.id) }
        if (titles.isEmpty()) null else row.copy(titles = titles)
    }
}
internal fun continuingEntries(history: List<ViewingEntry>, mode: CatalogueMode): List<ViewingEntry> =
    history.filter { mode.accepts(it.title.provider) && (it.position > 0 || it.episodeId != null) }.distinctBy { it.title.id }

internal fun browseTitles(catalog: DesignCatalog, filters: BrowseFilters): List<DesignTitle> {
    val titles = catalogueRows(catalog, filters.category).flatMap { it.titles }.distinctBy { it.id }
    return sortTitles(titles.filter { title ->
        (filters.provider == "All" || title.provider == filters.provider) &&
        (filters.type == MediaTypeFilter.ALL || when (filters.type) { MediaTypeFilter.MOVIES -> !title.isTv; MediaTypeFilter.SHORTS -> title.isShort; else -> title.isTv && !title.isShort }) &&
        (filters.genre == "All" || filters.genre in title.genres) &&
        (filters.year == "All" || title.year.toString() == filters.year) &&
        (filters.region == "All" || filters.region in title.countries)
    }, filters.sort)
}

internal fun discoveryFocusRow(id: String?, rows: List<DesignRow>, continuing: List<ViewingEntry>): String? =
    if (id == null) null else if (continuing.any { it.title.id == id }) "continue" else rows.firstOrNull { row -> row.titles.any { it.id == id } }?.id
