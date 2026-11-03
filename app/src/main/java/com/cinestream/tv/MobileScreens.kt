package com.cinestream.tv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun MobileCatalogueChooser(onSelect: (CatalogueMode) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp).testTag("mobile-chooser"), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Text("CineDBKTV", style = MaterialTheme.typography.headlineLarge, color = Accent) }
        item { Text("Choose your catalogue / 选择片库", style = MaterialTheme.typography.titleLarge) }
        item { Button({ onSelect(CatalogueMode.ENGLISH) }, Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("English · Cineby + BingeBang") } }
        item { Button({ onSelect(CatalogueMode.CHINESE) }, Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("中文 · 独播库 DBKU") } }
        item { Text("You can change this in Settings. This choice does not change your phone’s language.", color = Muted) }
    }
}

@Composable
fun MobileShell(state: StreamingState, content: @Composable () -> Unit) {
    val icons = listOf(Icons.Outlined.Home, Icons.Outlined.Movie, Icons.Outlined.Search, Icons.Outlined.History, Icons.Outlined.Settings)
    val labels = if (state.catalogueMode == CatalogueMode.CHINESE) listOf("首页", "分类", "搜索", "记录", "设置") else listOf("Home", "Browse", "Search", "History", "Settings")
    Scaffold(Modifier.fillMaxSize().safeDrawingPadding().imePadding().testTag("mobile-shell"), topBar = {
        Surface(shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.selected != null) IconButton(state::back) { Icon(Icons.Outlined.ArrowBack, "Back") }
                Text(state.selected?.title ?: "CineDBKTV", Modifier.weight(1f).padding(8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                TextButton({ state.navigate("catalogue") }) { Text(state.catalogueMode?.label.orEmpty()) }
            }
        }
    }, bottomBar = {
        NavigationBar {
            mobileDestinations.forEachIndexed { index, id ->
                NavigationBarItem(selected = state.tab == id, onClick = { state.navigate(id) }, icon = { Icon(icons[index], labels[index]) }, label = { Text(labels[index], maxLines = 1) })
            }
        }
    }) { padding -> Box(Modifier.fillMaxSize().padding(padding)) { content() } }
}

@Composable
private fun MobilePoster(title: DesignTitle, onSelect: (DesignTitle) -> Unit) {
    Card(onClick = { onSelect(title) }, modifier = Modifier.fillMaxWidth().testTag("mobile-poster:${title.id}"), shape = RoundedCornerShape(12.dp)) {
        OriginalArtwork(title.poster, title.title, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
            Text(title.provider.uppercase(), color = Accent, style = MaterialTheme.typography.labelSmall)
            val metadata = listOfNotNull(title.year.takeIf { it > 0 }?.toString(), title.rating.takeIf { it.isNotBlank() }?.let { "★ $it" })
            if (metadata.isNotEmpty()) Text(metadata.joinToString(" · "), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun MobileCatalogue(state: StreamingState) {
    if (state.tab == "home") DiscoveryHome(state, false) else CatalogueBrowse(state, false)
}

@Composable
private fun MobileFilter(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton({ expanded = true }, Modifier.heightIn(min = 48.dp)) { Text("$label: $value") }
        DropdownMenu(expanded, { expanded = false }, Modifier.heightIn(max = 320.dp)) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { expanded = false; onSelect(option) }) }
        }
    }
}

@Composable
fun MobileSearch(state: StreamingState) {
    val search = state.searchState
    var input by rememberSaveable { mutableStateOf(search.query) }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(input) { if (input != search.query) { delay(450); state.search(input) } }
    val results = if (search.query.isBlank() && !search.filters.isActive && search.results.isEmpty()) state.catalog?.titles.orEmpty() else search.results
    LazyVerticalGrid(columns = GridCells.Fixed(mobilePosterColumns(LocalConfiguration.current.screenWidthDp)), modifier = Modifier.fillMaxSize().testTag("mobile-search-grid"),
        contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth().testTag("mobile-search-input"), label = { Text("Search / 搜索片名") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); state.search(input) }))
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { MobileFilter("Type", search.filters.mediaType.label, MediaTypeFilter.entries.filter { it != MediaTypeFilter.SHORTS }.map { it.label }) { value -> state.search(input, search.filters.copy(mediaType = MediaTypeFilter.entries.first { it.label == value })) } }
                    item { MobileFilter("Year", search.filters.year, FilterOptions.YEARS) { state.search(input, search.filters.copy(year = it)) } }
                    item { MobileFilter("Sort", search.filters.sort.label, SortOption.entries.map { it.label }) { value -> state.search(input, search.filters.copy(sort = SortOption.entries.first { it.label == value })) } }
                    if (state.catalogueMode == CatalogueMode.CHINESE) {
                        item { MobileFilter("地区", search.filters.country, FilterOptions.COUNTRIES) { state.search(input, search.filters.copy(country = it)) } }
                        item { MobileFilter("类型", search.filters.genre, FilterOptions.GENRES) { state.search(input, search.filters.copy(genre = it)) } }
                    }
                }
                if (search.filters.isActive) TextButton({ state.search(input, SearchFilters()) }) { Text("Clear filters") }
                Text("Type, year and sorting apply to loaded results. DBKU region/genre use filtered catalogue pages.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (search.searching) item(span = { GridItemSpan(maxLineSpan) }) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        search.providerStatuses.forEach { status -> item(span = { GridItemSpan(maxLineSpan) }) {
            Text("${status.provider.uppercase()}: ${status.error ?: status.message ?: "${status.count} results"}", color = if (status.error != null) Gold else Muted)
        } }
        search.searchError?.let { message -> item(span = { GridItemSpan(maxLineSpan) }) {
            Column { Text(message, color = Gold); TextButton({ state.search(input) }) { Text("Retry") } }
        } }
        if (results.isEmpty() && !search.searching) item(span = { GridItemSpan(maxLineSpan) }) { Text("No matching titles. Check provider status above.") }
        items(results, key = { it.id }) { title -> MobilePoster(title) { keyboard?.hide(); state.open(it) } }
        if (search.hasMore) item(span = { GridItemSpan(maxLineSpan) }) { Button(state::loadMoreSearch, enabled = !search.searching) { Text("Load more") } }
    }
}

@Composable
fun MobileDetails(title: DesignTitle, state: StreamingState) {
    var seasonIndex by rememberSaveable(title.id) { mutableIntStateOf(0) }
    var page by rememberSaveable(title.id, seasonIndex) { mutableIntStateOf(0) }
    val history = state.history.firstOrNull { it.title.id == title.id }
    val season = title.seasons.getOrNull(seasonIndex) ?: title.seasons.firstOrNull()
    val episodes = season?.episodes.orEmpty()
    val pages = episodePageCount(episodes.size)
    val columns = mobilePosterColumns(LocalConfiguration.current.screenWidthDp)
    LazyColumn(Modifier.fillMaxSize().testTag("mobile-details"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { OriginalArtwork(title.backdrop, title.title, Modifier.fillMaxWidth().aspectRatio(16f / 9f)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OriginalArtwork(title.poster, title.title, Modifier.width(96.dp).height(144.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title.title, style = MaterialTheme.typography.headlineSmall)
                    Text(title.provider.uppercase(), color = Accent)
                    if (title.year > 0) Text(title.year.toString())
                    if (title.rating.isNotBlank()) Text("★ ${title.rating} · ${title.ratingSource.ifBlank { title.provider.uppercase() }}")
                    if (title.genres.isNotEmpty()) Text(title.genres.joinToString(" · "))
                    if (title.countries.isNotEmpty()) Text(title.countries.joinToString())
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!title.isTv || episodes.isNotEmpty()) Button({ state.play(title, if (title.isTv) episodes.firstOrNull() else null) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (title.isTv) "Play ${episodes.firstOrNull()?.title.orEmpty()}" else "Play movie") }
                else Text("No episodes listed by this provider.")
                if (history != null) OutlinedButton({ state.continueWatching(title) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (history.position > 0) "Continue · ${history.position / 60000} min" else "Return to last episode") }
            }
        }
        item { Text(title.overview.ifBlank { "Synopsis unavailable." }) }
        if (title.directors.isNotEmpty()) item { Text("Director: ${title.directors.joinToString()}") }
        if (title.cast.isNotEmpty()) item { Text("Cast: ${title.cast.joinToString { it.name }}") }
        if (title.updatedAt.isNotBlank()) item { Text("Updated: ${title.updatedAt}") }
        if (title.isTv && title.seasons.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(title.seasons.size) { index ->
                    FilterChip(seasonIndex == index, { seasonIndex = index; page = 0 }, label = { Text(title.seasons[index].name) })
                } }
            }
            if (pages > 1) item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(pages) { index ->
                    FilterChip(page == index, { page = index }, label = { Text("${index * 50 + 1}–${minOf((index + 1) * 50, episodes.size)}") })
                } }
            }
            items(episodes.drop(page.coerceAtMost((pages - 1).coerceAtLeast(0)) * 50).take(50).chunked(columns)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { episode -> OutlinedButton({ state.play(title, episode) }, Modifier.weight(1f).heightIn(min = 48.dp)) { Text((if (history?.episodeId == episode.id) "▶ " else "") + episode.title, maxLines = 2, overflow = TextOverflow.Ellipsis) } }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun MobileHistory(state: StreamingState) {
    val entries = state.history.filter { state.catalogueMode?.accepts(it.title.provider) == true }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Watch history / 观看记录", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Exact resume depends on the provider player reporting progress.", color = Muted) }
        if (entries.isNotEmpty()) item { OutlinedButton(state::clearHistory) { Text("Clear this catalogue’s history") } }
        if (entries.isEmpty()) item { Text("No viewing history yet.") }
        items(entries, key = { it.title.id }) { entry ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    TextButton({ state.open(entry.title) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("${entry.title.title} · ${entry.title.provider.uppercase()}") }
                    if (entry.position > 0) Text("${entry.position / 60000} min", color = Muted)
                    TextButton({ state.removeHistory(entry.title.id) }) { Text("Remove") }
                }
            }
        }
    }
}

@Composable
fun MobileSettings(state: StreamingState) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Settings / 设置", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Catalogue: ${state.catalogueMode?.label}") }
        item { Button({ state.navigate("catalogue") }) { Text("English / 中文") } }
        item { OutlinedButton(state::reload) { Text("Reload catalogue") } }
        item { Text("CineDBKTV 1.6.0 · Shared providers, history and playback. Subtitle and server options depend on each player.") }
    }
}
