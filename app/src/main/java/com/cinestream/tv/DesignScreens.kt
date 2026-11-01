package com.cinestream.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay

val Accent = Color(0xFF00E5FF)
val Background = Color(0xFF0F0F0F)
val Muted = Color(0xFFAAAAAA)
val Gold = Color(0xFFFFB300)

@Composable
fun Action(label: String, modifier: Modifier = Modifier, primary: Boolean = false, icon: ImageVector? = null, dense: Boolean = false, requester: FocusRequester? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "button focus")
    val shape = RoundedCornerShape(12.dp)
    Button(onClick, modifier
        .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
        .onFocusChanged { focused = it.isFocused }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .shadow(if (focused) 20.dp else 0.dp, shape, ambientColor = Accent, spotColor = Accent)
        .defaultMinSize(minWidth = 0.dp, minHeight = 44.dp), shape = shape,
        border = BorderStroke(if (focused) 3.dp else 1.dp, if (focused) Accent else Color.White.copy(alpha = .18f)),
        colors = ButtonDefaults.buttonColors(containerColor = if (focused) Color.White else if (primary) Accent else Color(0xD91A1A1A), contentColor = if (focused || primary) Color.Black else Color.White),
        contentPadding = PaddingValues(horizontal = if (dense) 10.dp else 22.dp, vertical = 12.dp)) {
        icon?.let { Icon(it, null, Modifier.size(if (dense) 18.dp else 20.dp)); if (label.isNotEmpty()) Spacer(Modifier.width(9.dp)) }
        if (label.isNotEmpty()) Text(label, fontSize = if (dense) 12.sp else 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun OriginalArtwork(url: String, description: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Crop) {
    SubcomposeAsyncImage(model = url, contentDescription = description, modifier = modifier.background(Color(0xFF161616)), contentScale = scale,
        loading = { Box(Modifier.fillMaxSize().background(Color(0xFF161616))) },
        error = { Box(Modifier.fillMaxSize().background(Color(0xFF161616)), contentAlignment = Alignment.Center) { Text("Artwork unavailable", color = Muted, fontSize = 10.sp) } })
}

private data class Tab(val id: String, val title: String, val icon: ImageVector)

@Composable
fun DesignShell(tab: String, menuSignal: Int, onTab: (String) -> Unit, onTester: () -> Unit, content: @Composable (Boolean) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().background(Background)) {
        val compact = maxWidth < 600.dp
        var expanded by remember { mutableStateOf(false) }
        val navFocus = remember { FocusRequester() }
        LaunchedEffect(menuSignal) { if (menuSignal > 0) { expanded = true; withFrameNanos { }; navFocus.requestFocus() } }
        val railWidth = if (compact) 56.dp else 80.dp
        Box(Modifier.fillMaxSize().padding(start = if (tab == "search" && expanded) 256.dp else railWidth)) { content(compact) }
        Column(Modifier.fillMaxHeight().width(if (expanded) 256.dp else railWidth)
            .background(Color(0xF90A0A0A)).border(BorderStroke(1.dp, Color(0xFF242424)))
            .onFocusChanged { expanded = it.hasFocus }.focusGroup()) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = if (compact && !expanded) 4.dp else 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF67E8F9)))), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AutoAwesome, "CineDBKTV", tint = Color.Black, modifier = Modifier.size(22.dp)) }
                        if (expanded) Column(Modifier.padding(start = 12.dp)) { Text("CineDBKTV", fontWeight = FontWeight.Black); Text("ANDROID TV", color = Accent, fontSize = 10.sp) }
                    }
                }
                val tabs = listOf(Tab("home", "首页 Home", Icons.Outlined.Home), Tab("mainland", "陆剧 Chinese Dramas", Icons.Outlined.Tv), Tab("taiwan_thailand", "台泰剧 Taiwan / Thailand", Icons.Outlined.LocalMovies), Tab("japan_korea", "日韩剧 Japan & Korea", Icons.Outlined.Public), Tab("anime", "动漫 Anime / Donghua", Icons.Outlined.AutoAwesome), Tab("variety", "综艺 Variety Shows", Icons.Outlined.Mic), Tab("search", "搜索 Search", Icons.Outlined.Search))
                items(tabs, key = { it.id }) { item ->
                    var focused by remember { mutableStateOf(false) }
                    val selected = tab == item.id
                    Row(Modifier.fillMaxWidth().height(48.dp).then(if (item.id == "home") Modifier.focusRequester(navFocus) else Modifier).onFocusChanged { focused = it.isFocused }
                        .shadow(if (focused) 16.dp else 0.dp, RoundedCornerShape(12.dp), ambientColor = Accent, spotColor = Accent)
                        .clip(RoundedCornerShape(12.dp)).background(if (focused) Color.White else if (selected) Color(0xFF262626) else Color.Transparent)
                        .border(if (focused) 3.dp else 1.dp, if (focused) Accent else if (selected) Accent.copy(alpha = .3f) else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { onTab(item.id) }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(item.icon, item.title, tint = if (focused) Color.Black else if (selected) Accent else Muted, modifier = Modifier.size(24.dp))
                        if (expanded) Text(item.title, Modifier.padding(start = 16.dp), color = if (focused) Color.Black else if (selected) Accent else Muted, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
            HorizontalDivider(color = Color(0xFF222222))
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF10B981)))
                if (expanded) Text("Cineby · DBKU", Modifier.padding(start = 12.dp), fontSize = 11.sp, color = Muted)
            }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(16.dp)) { Action("Stream Extractor API", icon = Icons.Outlined.WifiTethering, dense = true, onClick = onTester) }
    }
}

@Composable
private fun Metadata(title: DesignTitle, compact: Boolean, details: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (title.rating.isNotBlank()) Text("★ ${title.rating}${if (!details && title.provider.isBlank()) " TMDB" else ""}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Gold.copy(alpha = .1f)).border(1.dp, Gold.copy(alpha = .2f), RoundedCornerShape(5.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
        if (title.year > 0) Text("${title.year}", color = Color.LightGray, fontSize = 12.sp)
        Text(title.quality, color = Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.background(Color(0x99083642), RoundedCornerShape(4.dp)).border(1.dp, Accent.copy(alpha = .3f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
        if (!compact) Text(if (details && title.minutes > 0) "${title.minutes} min  ·  ${title.genres.joinToString(" • ")}" else title.genres.take(2).joinToString(" • "), color = Muted, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun CinematicBackdrop(title: DesignTitle, modifier: Modifier) {
    Box(modifier) {
        OriginalArtwork(title.backdrop, "${title.title} original backdrop", Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .1f)))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Background, .45f to Background.copy(alpha = .85f), 1f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Background.copy(alpha = .35f), .45f to Color.Transparent, 1f to Background)))
    }
}

@Composable
fun DesignHome(catalog: DesignCatalog, tab: String, compact: Boolean, onSelect: (DesignTitle) -> Unit, onPlay: (DesignTitle) -> Unit) {
    val featured = catalog.titles.filter { it.featured }
    var slide by rememberSaveable { mutableIntStateOf(0) }
    val title = featured.getOrElse(slide) { featured.first() }
    val first = remember { FocusRequester() }
    LaunchedEffect(tab) { withFrameNanos { }; first.requestFocus() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 64.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(if (compact) 490.dp else 460.dp)) {
                CinematicBackdrop(title, Modifier.fillMaxSize())
                Column(Modifier.align(Alignment.CenterStart).padding(horizontal = if (compact) 24.dp else 64.dp).widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Metadata(title, compact)
                    Text(title.title, fontSize = if (compact) 32.sp else 48.sp, fontWeight = FontWeight.Black, lineHeight = if (compact) 36.sp else 52.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (title.tagline.isNotEmpty()) Text("\"${title.tagline}\"", color = Color(0xCC67E8F9), fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(title.overview, color = Color(0xFFD4D4D4), fontSize = 14.sp, lineHeight = 22.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (compact) Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 12.dp)) {
                        Action("Watch Now", primary = true, icon = Icons.Outlined.PlayArrow, requester = first) { onPlay(title) }
                        Action("Details & Seasons", icon = Icons.Outlined.Info) { onSelect(title) }
                    } else Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(top = 12.dp)) {
                        Action("Watch Now", primary = true, icon = Icons.Outlined.PlayArrow, requester = first) { onPlay(title) }
                        Action("Details & Seasons", icon = Icons.Outlined.Info) { onSelect(title) }
                    }
                }
                Text(if (title.provider.isBlank()) "DEMO CATALOG" else title.provider.uppercase(), Modifier.align(Alignment.TopStart).padding(start = if (compact) 24.dp else 64.dp, top = 20.dp), color = Muted, fontSize = 9.sp, letterSpacing = 2.sp)
                Row(Modifier.align(Alignment.BottomEnd).padding(end = 48.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    featured.forEachIndexed { index, item ->
                        Box(Modifier.size(width = if (slide == index) 32.dp else 10.dp, height = 10.dp).clip(CircleShape).background(if (slide == index) Accent else Color.White.copy(alpha = .3f)).clickable(onClickLabel = "Feature ${item.title}") { slide = index })
                    }
                }
            }
        }
        items(catalog.rows.filter { tab == "home" || it.id == tab }, key = { it.id }) { row ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.padding(horizontal = if (compact) 24.dp else 64.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(row.title, fontSize = if (compact) 16.sp else 20.sp, fontWeight = FontWeight.Black)
                    if (!compact) Text("• ${row.tag}", color = Accent, fontSize = 11.sp)
                }
                LazyRow(contentPadding = PaddingValues(horizontal = if (compact) 24.dp else 64.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(row.titles, key = { it.id }) { media -> DesignPoster(media, onSelect) }
                }
            }
        }
        item { Text("Catalog and playback sources: cineby.ws · dbku.tv", Modifier.padding(horizontal = if (compact) 24.dp else 64.dp), color = Muted, fontSize = 11.sp) }
    }
}

@Composable
fun DesignPoster(title: DesignTitle, onSelect: (DesignTitle) -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "poster focus")
    Box(modifier.width(160.dp).height(240.dp).onFocusChanged { focused = it.isFocused }.graphicsLayer { scaleX = scale; scaleY = scale; translationY = if (focused) -4.dp.toPx() else 0f }
        .shadow(if (focused) 24.dp else 0.dp, RoundedCornerShape(12.dp), ambientColor = Accent, spotColor = Accent)
        .clip(RoundedCornerShape(12.dp)).border(if (focused) 4.dp else 1.dp, if (focused) Accent else Color.White.copy(alpha = .1f), RoundedCornerShape(12.dp)).clickable { onSelect(title) }) {
        OriginalArtwork(title.poster, title.title, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, .5f to Color.Black.copy(alpha = .15f), 1f to Color(0xFF050505))))
        Text(title.quality, Modifier.align(Alignment.TopEnd).padding(10.dp).background(Color(0xBF000000), RoundedCornerShape(4.dp)).border(1.dp, Accent.copy(alpha = .3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp), color = Color(0xFF67E8F9), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("★ ${title.rating}", color = Gold, fontSize = 11.sp); Text("${title.year}", color = Muted, fontSize = 11.sp) }
        }
        if (focused) Box(Modifier.fillMaxSize().background(Color(0x33083642)), contentAlignment = Alignment.Center) { Box(Modifier.size(48.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PlayArrow, "Open details", tint = Color.Black, modifier = Modifier.size(28.dp)) } }
    }
}

@Composable
fun DesignDetails(title: DesignTitle, compact: Boolean, onBack: () -> Unit, onPlay: (DesignEpisode?) -> Unit) {
    var seasonIndex by rememberSaveable(title.id) { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    LaunchedEffect(title.id) { withFrameNanos { }; first.requestFocus() }
    val padding = if (compact) 24.dp else 56.dp
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 64.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(if (compact) 540.dp else 480.dp)) {
                CinematicBackdrop(title, Modifier.fillMaxSize())
                Box(Modifier.align(Alignment.TopStart).padding(start = padding, top = 24.dp)) { Action("Back to Catalog", icon = Icons.Outlined.ArrowBack, dense = true, onClick = onBack) }
                Column(Modifier.align(Alignment.BottomStart).padding(start = padding, end = padding, bottom = 32.dp).widthIn(max = 740.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Metadata(title, compact, details = true)
                    Text(title.title, fontSize = if (compact) 32.sp else 48.sp, fontWeight = FontWeight.Black, lineHeight = if (compact) 36.sp else 52.sp)
                    Text(title.overview, color = Color.LightGray, fontSize = 14.sp, lineHeight = 22.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Action(if (!title.isTv) "Play Movie" else title.seasons.firstOrNull()?.episodes?.firstOrNull()?.let { if (title.provider == "dbku") "Play ${it.title}" else "Play S${it.season}:E${it.number}" } ?: "No episodes available", primary = true, icon = Icons.Outlined.PlayArrow, requester = first) { onPlay(title.seasons.firstOrNull()?.episodes?.firstOrNull()) }
                        if (!compact) Text("Source: ${title.provider.uppercase()}", color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
        if (title.provider == "dbku" && title.seasons.isNotEmpty()) {
            item { DbkuEpisodes(title, onPlay) }
        } else if (title.seasons.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = padding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Seasons & Episodes", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Episodes from ${title.provider.uppercase()}", fontSize = 11.sp, color = Muted)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(title.seasons.size) { index -> val season = title.seasons[index]; Action("${season.name} (${season.episodes.size} eps)", primary = seasonIndex == index, dense = true) { seasonIndex = index } }
                    }
                    HorizontalDivider(color = Color(0xFF262626))
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = padding, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(title.seasons.getOrElse(seasonIndex) { title.seasons.first() }.episodes, key = { it.id }) { episode ->
                        var focused by remember { mutableStateOf(false) }
                        Column(Modifier.width(256.dp).onFocusChanged { focused = it.isFocused }.clip(RoundedCornerShape(12.dp)).background(Color(0xFF171717)).border(if (focused) 4.dp else 1.dp, if (focused) Accent else Color(0xFF262626), RoundedCornerShape(12.dp)).clickable { onPlay(episode) }) {
                            Box(Modifier.fillMaxWidth().height(144.dp)) {
                                OriginalArtwork(episode.image, episode.title, Modifier.fillMaxSize())
                                if (episode.minutes > 0) Text("${episode.minutes}m", Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Color.Black.copy(alpha = .8f)).padding(4.dp), fontSize = 10.sp, color = Color.LightGray)
                                if (focused) Icon(Icons.Outlined.PlayArrow, null, Modifier.align(Alignment.Center).size(32.dp), tint = Accent)
                            }
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("EP ${episode.number}", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(episode.date, color = Muted, fontSize = 10.sp) }
                                Text(episode.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(episode.overview, color = Muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        if (title.isTv && title.seasons.isEmpty()) item { Text("The provider has not listed any available episodes for this title.", Modifier.padding(horizontal = padding), color = Muted) }
        item { Text("Cast & Crew", Modifier.padding(horizontal = padding), fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = padding), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                items(title.cast) { person -> Column(Modifier.width(108.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OriginalArtwork(person.image, person.name, Modifier.size(80.dp).clip(CircleShape))
                    Text(person.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Text(person.role, color = Muted, fontSize = 10.sp, maxLines = 2)
                } }
            }
        }
    }
}

@Composable
fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color.White else if (selected) Accent else Color(0xFF292929),
                shape = shape
            ),
        shape = shape,
        color = if (focused) Color.White else if (selected) Accent else Color(0xFF171717),
        contentColor = if (focused || selected) Color.Black else Color.White
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            fontSize = 11.sp,
            fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignSearch(titles: List<DesignTitle>, compact: Boolean, state: StreamingState, onSelect: (DesignTitle) -> Unit) {
    val searchState = state.searchState
    var inputQuery by rememberSaveable { mutableStateOf(searchState.query) }
    val queryFocus = remember { FocusRequester() }
    val filterFocus = remember { FocusRequester() }
    val resultFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    val posterState = rememberLazyListState()
    val resultsState = rememberLazyListState(searchState.scrollIndex, searchState.scrollOffset)
    DisposableEffect(resultsState) {
        onDispose {
            searchState.scrollIndex = resultsState.firstVisibleItemIndex
            searchState.scrollOffset = resultsState.firstVisibleItemScrollOffset
        }
    }
    val results = if (searchState.query.isBlank() && !searchState.filters.isActive && searchState.results.isEmpty()) titles.take(8) else searchState.results
    val focusResultId = searchState.focusedResultId?.takeIf { id -> results.any { it.id == id } } ?: results.firstOrNull()?.id
    LaunchedEffect(inputQuery) {
        if (inputQuery != searchState.query) {
            delay(500)
            state.search(inputQuery, searchState.filters, 1)
        }
    }
    LaunchedEffect(Unit) {
        if (searchState.restoreResultFocus && focusResultId != null) {
            posterState.scrollToItem(results.indexOfFirst { it.id == focusResultId }.coerceAtLeast(0))
            withFrameNanos { }
            resultFocus.requestFocus()
            searchState.restoreResultFocus = false
        } else filterFocus.requestFocus()
    }
    BackHandler(imeVisible) {
        keyboard?.hide()
        filterFocus.requestFocus()
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val narrow = compact || maxWidth < 600.dp || maxHeight < 480.dp
        val padding = if (narrow) 16.dp else 24.dp
        Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp).testTag("search-input").focusRequester(queryFocus).focusProperties { down = filterFocus },
                placeholder = { Text("Search titles / 搜索中文片名") },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Accent) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { state.search(inputQuery, searchState.filters); keyboard?.hide(); filterFocus.requestFocus() }),
                shape = RoundedCornerShape(16.dp)
            )
            val toggle: @Composable () -> Unit = {
                Action(if (searchState.filtersExpanded) "Hide filters" else "Filters",
                    Modifier.testTag("search-filter-toggle").focusProperties {
                        up = queryFocus
                        if (results.isNotEmpty() && (!narrow || !searchState.filtersExpanded)) right = resultFocus
                    }, dense = true, requester = filterFocus) {
                    filterFocus.requestFocus()
                    searchState.filtersExpanded = !searchState.filtersExpanded
                }
            }
            if (narrow) {
                toggle()
                LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("search-compact"), state = resultsState, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (searchState.filtersExpanded) searchFilterItems(inputQuery, searchState, state) { inputQuery = it }
                    searchResultItems(results, searchState, state, onSelect, posterState, focusResultId, resultFocus, queryFocus, filterFocus)
                }
            } else {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.width(if (searchState.filtersExpanded) 300.dp else 80.dp).fillMaxHeight().testTag("search-sidebar").focusGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        toggle()
                        if (searchState.filtersExpanded) {
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                searchFilterItems(inputQuery, searchState, state) { inputQuery = it }
                            }
                        }
                    }
                    LazyColumn(Modifier.weight(1f).fillMaxHeight().testTag("search-results").focusGroup(), state = resultsState, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        searchResultItems(results, searchState, state, onSelect, posterState, focusResultId, resultFocus, queryFocus, filterFocus)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.searchFilterItems(
    inputQuery: String, searchState: SearchState, state: StreamingState, onQuery: (String) -> Unit
) {
        // Search Filters Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SEARCH FILTERS", fontSize = 12.sp, color = Accent, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                // Active Filters bar & Clear button
                if (searchState.filters.isActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).background(Color(0xFF141414), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("ACTIVE:", fontSize = 10.sp, color = Gold, fontWeight = FontWeight.Bold)
                        if (searchState.filters.mediaType != MediaTypeFilter.ALL) {
                            FilterPill("Type: ${searchState.filters.mediaType.label} ✕", selected = true) {
                                state.search(inputQuery, searchState.filters.copy(mediaType = MediaTypeFilter.ALL), 1)
                            }
                        }
                        if (searchState.filters.sort != SortOption.DEFAULT) {
                            FilterPill("Sort: ${searchState.filters.sort.label} ✕", selected = true) {
                                state.search(inputQuery, searchState.filters.copy(sort = SortOption.DEFAULT), 1)
                            }
                        }
                        if (searchState.filters.year != "All") {
                            FilterPill("Year: ${searchState.filters.year} ✕", selected = true) {
                                state.search(inputQuery, searchState.filters.copy(year = "All"), 1)
                            }
                        }
                        if (searchState.filters.country != "All") {
                            FilterPill("Country: ${searchState.filters.country.substringBefore(" ")} ✕", selected = true) {
                                state.search(inputQuery, searchState.filters.copy(country = "All"), 1)
                            }
                        }
                        if (searchState.filters.genre != "All") {
                            FilterPill("Genre: ${searchState.filters.genre.substringBefore(" ")} ✕", selected = true) {
                                state.search(inputQuery, searchState.filters.copy(genre = "All"), 1)
                            }
                        }
                        Action("Clear All Filters", icon = Icons.Outlined.Clear, dense = true) {
                            state.search(inputQuery, SearchFilters())
                        }
                    }
                }

                // Media Type Filter Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("MEDIA TYPE", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MediaTypeFilter.values().forEach { type ->
                            FilterPill(type.label, selected = searchState.filters.mediaType == type) {
                                state.search(inputQuery, searchState.filters.copy(mediaType = type), 1)
                            }
                        }
                    }
                }

                // Sort Options Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("SORT LOADED RESULTS (POPULARITY NOT PROVIDED)", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SortOption.values().forEach { sort ->
                            FilterPill(sort.label, selected = searchState.filters.sort == sort) {
                                state.search(inputQuery, searchState.filters.copy(sort = sort), 1)
                            }
                        }
                    }
                }

                // Release Year Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("RELEASE YEAR", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterOptions.YEARS.forEach { y ->
                            FilterPill(y, selected = searchState.filters.year == y) {
                                state.search(inputQuery, searchState.filters.copy(year = y), 1)
                            }
                        }
                    }
                }

                // Country / Origin Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("COUNTRY / REGION — DBKU ONLY", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterOptions.COUNTRIES.forEach { c ->
                            FilterPill(c, selected = searchState.filters.country == c) {
                                state.search(inputQuery, searchState.filters.copy(country = c), 1)
                            }
                        }
                    }
                }

                // Genre Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("CHINESE GENRE — DBKU ONLY", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterOptions.GENRES.forEach { g ->
                            FilterPill(g, selected = searchState.filters.genre == g) {
                                state.search(inputQuery, searchState.filters.copy(genre = g), 1)
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.widthIn(max = 420.dp).background(Color(0xFF101010), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Chinese input: select the search box and use your TV’s Chinese keyboard.", color = Muted, fontSize = 11.sp)
                Text("ANDROID TV ON-SCREEN KEYBOARD", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Bold)
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toList().chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { letter -> Action(letter.toString(), Modifier.weight(1f), dense = true) { onQuery(inputQuery + letter) } }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Action("SPACE", Modifier.weight(1f), dense = true) { onQuery(inputQuery + " ") }
                    Action("DEL", Modifier.weight(1f), dense = true) { onQuery(inputQuery.dropLast(1)) }
                    Action("CLEAR", Modifier.weight(1f), dense = true) { onQuery(""); state.search("", SearchFilters()) }
                }
            }
        }


}

private fun androidx.compose.foundation.lazy.LazyListScope.searchResultItems(
    results: List<DesignTitle>, searchState: SearchState, state: StreamingState, onSelect: (DesignTitle) -> Unit,
    posterState: androidx.compose.foundation.lazy.LazyListState, focusResultId: String?, resultFocus: FocusRequester,
    queryFocus: FocusRequester, filterFocus: FocusRequester
) {
        // Provider Status badges & notices
        if (searchState.providerStatuses.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    searchState.providerStatuses.forEach { status ->
                        val isErr = status.error != null
                        val isUnsupp = status.isUnsupported
                        val badgeBg = if (isErr) Color(0x33FF5252) else if (isUnsupp) Color(0x33FFA000) else Color(0x3300E5FF)
                        val badgeBorder = if (isErr) Color(0xFFFF5252) else if (isUnsupp) Color(0xFFFFA000) else Accent.copy(alpha = 0.5f)
                        val badgeColor = if (isErr) Color(0xFFFF8A80) else if (isUnsupp) Color(0xFFFFD180) else Color(0xFF80DEEA)
                        val text = when {
                            isErr -> "${status.provider.uppercase()}: Request Failed (${status.error})"
                            isUnsupp -> "${status.provider.uppercase()}: Unsupported (${status.message})"
                            else -> "${status.provider.uppercase()}: ${status.count} results${status.message?.let { " · $it" }.orEmpty()}"
                        }
                        Text(
                            text = text,
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .background(badgeBg, RoundedCornerShape(6.dp))
                                .border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Local filter notice (if applicable)
        searchState.localFilterNotice?.let { notice ->
            item { Text("ℹ $notice", color = Gold, fontSize = 11.sp) }
        }

        // Results Header & Progress
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = if (searchState.query.isBlank() && !searchState.filters.isActive) "Recommended" else "Search results (${results.size})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (searchState.searching) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Accent, strokeWidth = 2.dp)
                    Text("Searching providers…", color = Accent, fontSize = 12.sp)
                }
            }
        }

        // Provider search error message
        searchState.searchError?.let { message ->
            item {
                Surface(color = Color(0x33FF5252), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFFF5252))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(message, color = Color(0xFFFF8A80), modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Action("Retry", dense = true) { state.search(searchState.query, searchState.filters, 1) }
                    }
                }
            }
        }

        // Empty Result handling
        if (results.isEmpty() && !searchState.searching) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (searchState.query.isNotBlank()) "No matching titles found for \"${searchState.query}\". Check provider status above."
                        else "No titles match the selected filter criteria",
                        color = Muted,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Posters Row / Grid
        if (results.isNotEmpty()) {
            item {
                LazyRow(state = posterState, modifier = Modifier.testTag("search-posters").focusGroup(), horizontalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                    items(results, key = { it.id }) { title ->
                        DesignPoster(title, { selected ->
                            searchState.focusedResultId = selected.id
                            searchState.restoreResultFocus = true
                            onSelect(selected)
                        }, Modifier
                            .then(if (title.id == focusResultId) Modifier.focusRequester(resultFocus) else Modifier)
                            .onFocusChanged { if (it.isFocused) searchState.focusedResultId = title.id }
                            .focusProperties {
                                up = queryFocus
                                if (title.id == results.firstOrNull()?.id) left = filterFocus
                            })
                    }
                }
            }
        }

        // Pagination / Load More button
        if (searchState.hasMore) {
            item {
                Action(
                    label = "Load More Results (Page ${searchState.page + 1})",
                    icon = Icons.Outlined.ExpandMore,
                    dense = true,
                    onClick = { state.loadMoreSearch() }
                )
            }
        }

}

