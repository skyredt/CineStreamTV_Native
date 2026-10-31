package com.cinestream.tv

enum class MediaTypeFilter(val label: String) { ALL("All"), MOVIES("Movies"), DRAMAS("Dramas"), SHORTS("Shorts") }
enum class SortOption(val label: String) { DEFAULT("Provider order"), RATING("Rating"), YEAR("Newest release"), TITLE("Title") }
data class SearchFilters(val mediaType: MediaTypeFilter = MediaTypeFilter.ALL, val genre: String = "All", val year: String = "All", val country: String = "All", val sort: SortOption = SortOption.DEFAULT) {
    val isActive get() = this != SearchFilters()
}
object FilterOptions {
    val YEARS = listOf("All") + (java.time.Year.now().value downTo 1980).map(Int::toString)
    val COUNTRIES = listOf("All", "大陆", "香港", "台湾", "韩国", "日本", "泰国", "新加坡", "United States", "United Kingdom")
    val GENRES = listOf("All", "古装", "武侠", "科幻", "悬疑", "都市", "爱情", "战争", "青春", "偶像", "喜剧", "家庭", "奇幻", "剧情", "乡村", "年代", "警匪", "谍战", "历险", "罪案", "宫廷", "经典", "动作", "惊悚", "历史", "穿越", "同性")
}
data class ProviderSearchStatus(val provider: String, val count: Int, val error: String? = null, val isUnsupported: Boolean = false, val message: String? = null)
data class SearchResults(val titles: List<DesignTitle>, val statuses: List<ProviderSearchStatus>, val localFilterNotice: String? = null, val nextPages: Map<String, String> = emptyMap())
internal fun filterTitles(titles: List<DesignTitle>, filters: SearchFilters): List<DesignTitle> = titles.filter { t ->
    (when (filters.mediaType) { MediaTypeFilter.ALL -> true; MediaTypeFilter.MOVIES -> !t.isTv; MediaTypeFilter.DRAMAS -> t.isTv && !t.isShort; MediaTypeFilter.SHORTS -> t.isShort }) &&
        (filters.year == "All" || t.year.toString() == filters.year) &&
        (t.provider == "cineby" || filters.genre == "All" || t.genres.any { it.equals(filters.genre, true) }) &&
        (t.provider == "cineby" || filters.country == "All" || t.countries.any { it.equals(filters.country, true) })
}
internal fun sortTitles(titles: List<DesignTitle>, sort: SortOption): List<DesignTitle> = when (sort) {
    SortOption.DEFAULT -> titles
    SortOption.RATING -> titles.sortedByDescending { it.rating.toDoubleOrNull() ?: -1.0 }
    SortOption.YEAR -> titles.sortedByDescending { it.year }
    SortOption.TITLE -> titles.sortedBy { it.title.lowercase() }
}
