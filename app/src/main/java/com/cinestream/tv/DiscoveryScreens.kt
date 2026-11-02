package com.cinestream.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
private fun catalogueReady(state: StreamingState): Boolean {
    if (state.catalog != null) return true
    Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("CineDBKTV", style = MaterialTheme.typography.headlineMedium, color = Accent)
        if (state.catalogError == null) { CircularProgressIndicator(); Text("Loading selected catalogue…") }
        else { Text(state.catalogError.orEmpty()); Action("Retry", onClick = state::reload) }
    }
    return false
}

@Composable
fun DiscoveryHome(state: StreamingState, tv: Boolean) {
    if (!catalogueReady(state)) return
    val catalog = state.catalog ?: return
    val mode = state.catalogueMode ?: return
    val continuing = continuingEntries(state.history, mode)
    val featured = catalog.titles.firstOrNull { it.featured } ?: catalog.titles.firstOrNull()
    val rows = discoveryRows(catalog, continuing.map { it.title.id }.toSet() + listOfNotNull(featured?.id))
    val first = remember { FocusRequester() }
    val scroll = rememberLazyListState()
    val restoreRow = discoveryFocusRow(state.homeFocusId, rows, continuing)
    val restoreId = state.homeFocusId.takeIf { restoreRow != null }
    LaunchedEffect(Unit) {
        if (tv) {
            val row = if (restoreRow == "continue") 1 else rows.indexOfFirst { it.id == restoreRow }.takeIf { it >= 0 }?.let { it + 1 + if (continuing.isNotEmpty()) 1 else 0 }
            if (restoreId != null && row != null) scroll.scrollToItem(row)
            withFrameNanos { }
            runCatching { first.requestFocus() }
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("discovery-home"), state = scroll, verticalArrangement = Arrangement.spacedBy(24.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            if (featured != null) Box(Modifier.fillMaxWidth().height(if (tv) 350.dp else 340.dp)) {
                OriginalArtwork(featured.backdrop, featured.title, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .15f), Background.copy(alpha = .8f), Background))))
                Column(Modifier.align(Alignment.BottomStart).padding(if (tv) 32.dp else 20.dp).widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("FEATURED · ${featured.provider.uppercase()}", color = Accent, style = MaterialTheme.typography.labelMedium)
                    Text(featured.title, style = if (tv) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val metadata = listOfNotNull(featured.year.takeIf { it > 0 }?.toString(), featured.rating.takeIf { it.isNotBlank() }?.let { "★ $it" }, featured.genres.take(2).joinToString(" · ").takeIf { it.isNotBlank() })
                    if (metadata.isNotEmpty()) Text(metadata.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall)
                    if (featured.overview.isNotBlank()) Text(featured.overview, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val saved = continuing.any { it.title.id == featured.id }
                        Action(if (saved) "Continue" else "Play", primary = true, requester = if (tv && restoreId == null) first else null) { state.homeFocusId = null; state.discoverPlay(featured, saved) }
                        Action("More details", dense = !tv) { state.homeFocusId = null; state.open(featured) }
                    }
                }
            }
        }
        if (continuing.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Continue Watching / 继续观看", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge)
                val initial = continuing.indexOfFirst { it.title.id == restoreId }.coerceAtLeast(0)
                LazyRow(state = rememberLazyListState(initial), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(continuing, key = { it.title.id }) { entry ->
                        Card(Modifier.width(if (tv) 270.dp else 240.dp)) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OriginalArtwork(entry.title.poster, entry.title.title, Modifier.width(64.dp).height(96.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(entry.title.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    if (entry.episodeId != null) Text(entry.episodeLabel.ifBlank { "Last selected episode" }, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    Text(if (entry.position > 0) "Played ${mediaClock(entry.position)}" else "Return to episode", color = Accent, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Action(if (entry.position > 0) "Continue" else "Return", Modifier.onFocusChanged { if (it.isFocused) { state.homeFocusId = entry.title.id; state.homeFocusRow = "continue" } }, dense = true,
                                    requester = if (tv && restoreRow == "continue" && entry.title.id == restoreId) first else null) { state.homeFocusId = entry.title.id; state.homeFocusRow = "continue"; state.discoverPlay(entry.title, true) }
                                TextButton({ state.homeFocusId = entry.title.id; state.homeFocusRow = "continue"; state.open(entry.title) }) { Text("Details") }
                            }
                        }
                    }
                }
            }
        }
        items(rows, key = { it.id }) { row ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(row.title, Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge)
                val initial = if (restoreRow == row.id) row.titles.indexOfFirst { it.id == restoreId }.coerceAtLeast(0) else 0
                LazyRow(state = rememberLazyListState(initial), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(row.titles, key = { it.id }) { title ->
                        DesignPoster(title, { state.homeFocusId = title.id; state.homeFocusRow = row.id; state.open(title) },
                            (if (tv && restoreRow == row.id && title.id == restoreId) Modifier.focusRequester(first) else Modifier)
                                .onFocusChanged { if (it.isFocused) { state.homeFocusId = title.id; state.homeFocusRow = row.id } })
                    }
                }
            }
        }
        if (state.warnings.isNotEmpty()) item { Column(Modifier.padding(20.dp)) { state.warnings.forEach { Text(it, color = Gold) }; Action("Retry catalogue", onClick = state::reload) } }
        if (featured == null && rows.isEmpty()) item { Text("No titles available. Try reloading the catalogue.", Modifier.padding(20.dp)) }
    }
}

@Composable
private fun BrowseMenu(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Action("$label: $value", Modifier.heightIn(min = 48.dp), dense = true) { open = true }
        DropdownMenu(open, { open = false }, Modifier.heightIn(max = 300.dp)) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { open = false; onSelect(option) }) }
        }
    }
}

@Composable
fun CatalogueBrowse(state: StreamingState, tv: Boolean) {
    if (!catalogueReady(state)) return
    val catalog = state.catalog ?: return
    val mode = state.catalogueMode ?: return
    var category by rememberSaveable(state.tab, mode) { mutableStateOf(if (state.tab == "browse") "home" else state.tab) }
    var provider by rememberSaveable(mode) { mutableStateOf("All") }
    var type by rememberSaveable(mode) { mutableStateOf(MediaTypeFilter.ALL.name) }
    var genre by rememberSaveable(mode) { mutableStateOf("All") }
    var year by rememberSaveable(mode) { mutableStateOf("All") }
    var region by rememberSaveable(mode) { mutableStateOf("All") }
    var sort by rememberSaveable(mode) { mutableStateOf(SortOption.DEFAULT.name) }
    var expanded by rememberSaveable { mutableStateOf(tv) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    val grid = rememberLazyGridState()
    val filters = BrowseFilters(category, provider, MediaTypeFilter.valueOf(type), genre, year, region, SortOption.valueOf(sort))
    val results = browseTitles(catalog, filters)
    val years = listOf("All") + catalog.titles.map { it.year }.filter { it > 0 }.distinct().sortedDescending().map { it.toString() }
    val genres = listOf("All") + catalog.titles.flatMap { it.genres }.filter { it.isNotBlank() }.distinct().sorted()
    val regions = listOf("All") + catalog.titles.flatMap { it.countries }.filter { it.isNotBlank() }.distinct().sorted()
    LaunchedEffect(Unit) {
        if (tv && selectedId != null) {
            val index = results.indexOfFirst { it.id == selectedId }
            if (index >= 0) { grid.scrollToItem(index + 1); withFrameNanos { }; runCatching { focus.requestFocus() } }
        }
    }
    val columns = if (tv) ((LocalConfiguration.current.screenWidthDp - 80) / 180).coerceIn(3, 7) else mobilePosterColumns(LocalConfiguration.current.screenWidthDp)
    LazyVerticalGrid(columns = GridCells.Fixed(columns), state = grid, modifier = Modifier.fillMaxSize().testTag("structured-browse"), contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (mode == CatalogueMode.CHINESE) "分类浏览" else "Browse catalogue", style = MaterialTheme.typography.headlineMedium)
                Text("Filter the loaded catalogue batch. Use Search for a wider provider search.", style = MaterialTheme.typography.bodySmall, color = Muted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(mobileCategories(catalog, mode)) { choice -> FilterPill(choice.label, category == choice.id) { category = choice.id } }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { BrowseMenu("Type", MediaTypeFilter.valueOf(type).label, MediaTypeFilter.entries.filter { it != MediaTypeFilter.SHORTS }.map { it.label }) { label -> type = MediaTypeFilter.entries.first { it.label == label }.name } }
                    if (mode == CatalogueMode.ENGLISH) item { BrowseMenu("Provider", provider, listOf("All", "cineby", "bingebang")) { provider = it } }
                    item { BrowseMenu("Sort loaded", SortOption.valueOf(sort).label, SortOption.entries.map { it.label }) { label -> sort = SortOption.entries.first { it.label == label }.name } }
                    item { Action(if (expanded) "Fewer filters" else "More filters", dense = true) { expanded = !expanded } }
                }
                if (expanded) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (genres.size > 1) item { BrowseMenu("Genre/category", genre, genres) { genre = it } }
                    if (years.size > 1) item { BrowseMenu("Year", year, years) { year = it } }
                    if (regions.size > 1) item { BrowseMenu("Region", region, regions) { region = it } }
                    if (years.size == 1 || regions.size == 1) item { Text("Year/region filters appear when supplied in this batch.", Modifier.padding(12.dp), color = Muted, style = MaterialTheme.typography.bodySmall) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Action("Reset", dense = true) { category = "home"; provider = "All"; type = MediaTypeFilter.ALL.name; genre = "All"; year = "All"; region = "All"; sort = SortOption.DEFAULT.name }
                    Action("Search providers", dense = true) { state.navigate("search") }
                }
                Text("${results.size} loaded results", color = Accent)
                if (state.warnings.isNotEmpty()) { state.warnings.forEach { Text(it, color = Gold) }; Action("Retry catalogue", dense = true, onClick = state::reload) }
            }
        }
        if (results.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Text("No loaded titles match these filters. Reset filters or search providers.") }
        items(results, key = { it.id }) { title ->
            if (tv) DesignPoster(title, { selectedId = title.id; state.open(title) }, (if (selectedId == title.id) Modifier.focusRequester(focus) else Modifier).onFocusChanged { if (it.isFocused) selectedId = title.id })
            else Card(onClick = { selectedId = title.id; state.open(title) }) {
                OriginalArtwork(title.poster, title.title, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                Column(Modifier.padding(10.dp)) { Text(title.title, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(title.provider.uppercase(), color = Accent, style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}
