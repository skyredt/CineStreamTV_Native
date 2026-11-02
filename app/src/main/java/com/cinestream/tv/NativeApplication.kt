package com.cinestream.tv

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.*

class CineStreamApp : Application() { val providers = ProvidersRepository() }
class MainActivity : ComponentActivity() {
    var webPlayerKeyHandler: ((android.view.KeyEvent) -> Boolean)? = null
    // AndroidX core annotates its public Activity override as library-only; TV key routing requires this framework hook.
    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (webPlayerKeyHandler?.invoke(event) == true) return true
        return super.dispatchKeyEvent(event)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Background, surface = Color(0xFF161616), onSurface = Color.White)) {
                val app = application as CineStreamApp
                val state: StreamingState = viewModel(factory = viewModelFactory { initializer { StreamingState(app.providers, CataloguePreferences(app)) } })
                Surface(Modifier.fillMaxSize()) { TvAppNavigation(state) }
            }
        }
    }
}

class SearchState {
    var filtersExpanded by mutableStateOf(true)
    var scrollIndex = 0
    var scrollOffset = 0
    var focusedResultId by mutableStateOf<String?>(null)
    var restoreResultFocus by mutableStateOf(false)
    var query by mutableStateOf("")
    var filters by mutableStateOf(SearchFilters())
    var results by mutableStateOf<List<DesignTitle>>(emptyList())
    var searching by mutableStateOf(false)
    var providerStatuses by mutableStateOf<List<ProviderSearchStatus>>(emptyList())
    var localFilterNotice by mutableStateOf<String?>(null)
    var page by mutableIntStateOf(1)
    var hasMore by mutableStateOf(false)
    var nextPages: Map<String, String> = emptyMap()
    var searchError by mutableStateOf<String?>(null)
}

class StreamingState(val repository: ProvidersRepository, private val preferences: CataloguePreferences? = null) : ViewModel() {
    var catalogueMode by mutableStateOf(preferences?.mode); private set
    var history by mutableStateOf(preferences?.readHistory().orEmpty()); private set
    var catalogueSession by mutableIntStateOf(0); private set
    var homeFocusId: String? = null
    var homeFocusRow: String? = null
    private var playingSelection: DesignTitle? = null
    private var playingEpisode: DesignEpisode? = null
    fun chooseCatalogue(mode: CatalogueMode) {
        cancel(); searchJob?.cancel(); preferences?.mode = mode; catalogueMode = mode; catalogueSession++
        selected = null; streams = emptyList(); tab = "home"; catalog = null
        homeFocusId = null; homeFocusRow = null
        searchState.query = ""; searchState.filters = SearchFilters(); searchState.results = emptyList()
        searchState.providerStatuses = emptyList(); searchState.nextPages = emptyMap(); searchState.hasMore = false
        searchState.focusedResultId = null; searchState.restoreResultFocus = false
        searchState.searching = false; searchState.searchError = null; searchState.localFilterNotice = null
        searchState.scrollIndex = 0; searchState.scrollOffset = 0
        reload()
    }
    fun removeHistory(id: String) { history = history.filterNot { it.title.id == id }; preferences?.writeHistory(history) }
    fun clearHistory() { history = history.filterNot { catalogueMode?.accepts(it.title.provider) == true }; preferences?.writeHistory(history) }
    private fun recordPosition(position: Long) {
        val title = playingSelection ?: return
        history = (listOf(ViewingEntry(title, playingEpisode?.id, position.coerceAtLeast(0), System.currentTimeMillis())) + history.filterNot { it.title.id == title.id }).take(100)
        preferences?.writeHistory(history)
    }
    private var lastSavedPosition = -10000L
    fun updatePosition(position: Long) {
        playbackPosition = position
        if (kotlin.math.abs(position - lastSavedPosition) >= 5000) { recordPosition(position); lastSavedPosition = position }
    }
    fun continueWatching(title: DesignTitle) {
        val entry = history.find { it.title.id == title.id }
        val episode = title.seasons.flatMap { it.episodes }.find { it.id == entry?.episodeId }
        if (title.isTv && entry?.episodeId != null && episode == null) { error = "The previously selected episode is no longer listed. Select an available episode."; return }
        play(title, episode, resume = true)
    }
    var catalog by mutableStateOf<DesignCatalog?>(null); private set
    var catalogError by mutableStateOf<String?>(null); private set
    var warnings by mutableStateOf<List<String>>(emptyList()); private set
    var selected by mutableStateOf<DesignTitle?>(null); private set
    var tab by mutableStateOf("home")
    var loadingDetails by mutableStateOf(false); private set
    var resolving by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)
    var streams by mutableStateOf<List<ProviderStream>>(emptyList()); private set
    var playbackPosition by mutableLongStateOf(0L)
    var sourceNotice by mutableStateOf<String?>(null)
    private val attemptedSources = mutableSetOf<Int>()
    var sourceIndex by mutableIntStateOf(0)
    var playingTitle by mutableStateOf(""); private set
    val searchState = SearchState()
    private var job: Job? = null
    private var catalogJob: Job? = null
    private var searchJob: Job? = null
    init { if (catalogueMode != null) reload() }
    fun reload() {
        catalogJob?.cancel(); catalogError = null; warnings = emptyList()
        catalogJob = viewModelScope.launch {
            try { val result = repository.home(catalogueMode); catalog = result.catalog; warnings = result.warnings }
            catch (e: CancellationException) { throw e } catch (e: Exception) { catalogError = e.message ?: "Catalog request failed." }
        }
    }
    fun open(title: DesignTitle) {
        cancel(); selected = title; loadingDetails = true
        job = viewModelScope.launch {
            try { selected = repository.details(title) }
            catch (e: CancellationException) { throw e } catch (e: Exception) { error = "Could not load ${title.title}: ${e.message}" }
            finally { if (currentCoroutineContext().isActive) loadingDetails = false }
        }
    }
    fun play(title: DesignTitle, episode: DesignEpisode?, resume: Boolean = false) {
        if (title.provider == "hongguo") { error = HONGGUO_NOTICE; return }
        cancel(); resolving = true; playbackPosition = if (resume) history.find { it.title.id == title.id }?.position ?: 0 else 0; sourceNotice = null; attemptedSources.clear()
        playingTitle = if (episode == null) title.title else "${title.title} · ${if (title.provider == "cineby") "S${episode.season}:E${episode.number} · " else ""}${episode.title}"
        job = viewModelScope.launch {
            try { streams = repository.sources(title, episode); playingSelection = title; playingEpisode = episode ?: title.seasons.firstOrNull()?.episodes?.firstOrNull(); recordPosition(playbackPosition); sourceIndex = 0; attemptedSources += 0 }
            catch (e: CancellationException) { throw e } catch (e: Exception) { error = "Could not load this selection: ${e.message}" }
            finally { if (currentCoroutineContext().isActive) resolving = false }
        }
    }
    fun direct(url: String, name: String) { cancel(); playingSelection = null; playbackPosition = 0; sourceNotice = null; attemptedSources.clear(); playingTitle = name; sourceIndex = 0; streams = listOf(ProviderStream("Direct media", url, false, "")) }
    fun sourceFailed(reason: String): Boolean {
        if (streams.getOrNull(sourceIndex)?.referer?.contains("cineby.ws") != true) return false
        attemptedSources += sourceIndex
        val next = nextSourceIndex(sourceIndex, attemptedSources, streams.size)
        if (next == null) { sourceNotice = "No remaining source for this selection. Choose a source to retry."; return false }
        val previous = streams[sourceIndex].name
        sourceIndex = next; attemptedSources += next
        sourceNotice = "$previous unavailable. Switched to ${streams[next].name} for the same episode."
        return true
    }
    fun cancel() { job?.cancel(); loadingDetails = false; resolving = false }
    fun back() { cancel(); when { streams.isNotEmpty() -> { recordPosition(playbackPosition); streams = emptyList() }; selected != null -> selected = null; else -> tab = "home" } }
    fun home() { cancel(); streams = emptyList(); selected = null; tab = "home" }
    fun navigate(value: String) { cancel(); selected = null; if (tab != value) { homeFocusId = null; homeFocusRow = null }; tab = value }

    fun search(query: String, filters: SearchFilters = searchState.filters, page: Int = 1) {
        searchJob?.cancel()
        searchState.query = query
        searchState.filters = filters
        searchState.searching = true
        searchState.searchError = null
        searchJob = viewModelScope.launch {
            try {
                val res = repository.searchWithFilters(query, filters, page, searchState.nextPages, catalogueMode)
                if (page == 1) {
                    searchState.results = res.titles
                } else {
                    searchState.results = (searchState.results + res.titles).distinctBy { it.id }
                }
                searchState.providerStatuses = res.statuses
                searchState.localFilterNotice = res.localFilterNotice
                searchState.page = page
                searchState.nextPages = res.nextPages
                searchState.hasMore = res.nextPages.isNotEmpty()
                searchState.searchError = if (res.statuses.any { it.error != null }) "Some providers could not be searched. Results may be incomplete." else null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                searchState.searchError = e.message ?: "Search failed"
            } finally {
                if (currentCoroutineContext().isActive) searchState.searching = false
            }
        }
    }

    fun clearSearchFilters() {
        search(searchState.query, SearchFilters(), 1)
    }

    fun loadMoreSearch() {
        if (!searchState.searching && searchState.hasMore) {
            search(searchState.query, searchState.filters, searchState.page + 1)
        }
    }
}

@Composable
fun TvAppNavigation(state: StreamingState) {
    var tester by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    var validation by rememberSaveable { mutableStateOf<String?>(null) }
    var menuSignal by remember { mutableIntStateOf(0) }
    var sourcePicker by remember { mutableStateOf(false) }
    val screens = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val catalog = state.catalog
    val selected = state.selected
    val stream = state.streams.getOrNull(state.sourceIndex)
    BackHandler(stream != null || selected != null || state.tab != "home" || state.resolving) { state.back() }
    Box(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
        if (stream != null && state.streams.size > 1 && event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_MENU) {
            if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) sourcePicker = true
            true
        } else false
    }) {
        if (stream != null) {
            key(stream.url) {
                if (stream.embedded) EmbeddedPlayer(stream, state.playingTitle, initialPosition = state.playbackPosition, onPosition = state::updatePosition, onFailure = state::sourceFailed, onBack = { state.back() })
                else PlayerScreen(stream.url, state.playingTitle, headers = if (stream.referer.isBlank()) emptyMap() else mapOf("Referer" to stream.referer), initialPosition = state.playbackPosition, onPosition = state::updatePosition, onFailure = state::sourceFailed, onBack = { state.back() })
            }
            state.sourceNotice?.let { message -> Text(message, Modifier.align(Alignment.TopCenter).padding(top = 70.dp).background(Color(0xEE101010)).padding(12.dp), color = Accent) }
            if (state.streams.size > 1 && !stream.embedded) Box(Modifier.align(Alignment.TopEnd).padding(16.dp)) { Action("Source: ${stream.name}", dense = true) { sourcePicker = true } }
        } else if (state.catalogueMode == null || state.tab == "catalogue") {
            CatalogueChooser(state::chooseCatalogue)
        } else {
            DesignShell(state.tab, menuSignal, onTab = state::navigate, onTester = { tester = true }, mode = state.catalogueMode!!, rows = catalog?.rows.orEmpty()) { compact ->
                screens.SaveableStateProvider(if (selected != null) "details:${selected.id}" else "catalogue:${state.catalogueMode}:${state.catalogueSession}:${state.tab}") {
                when {
                    selected != null -> DesignDetails(selected, compact, onBack = state::back, onPlay = { state.play(selected, it) }, history = state.history.find { it.title.id == selected.id }, onContinue = { state.continueWatching(selected) })
                    state.tab == "history" -> ViewingHistory(state)
                    state.tab == "settings" -> Column(Modifier.padding(40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Settings / 设置", style = MaterialTheme.typography.headlineLarge)
                        Text("Catalogue: ${state.catalogueMode?.label}")
                        Action("Change catalogue / 切换片库") { state.navigate("catalogue") }
                        Action("Reload catalogue") { state.reload() }
                        Text("Player controls, subtitles and servers remain available in each provider’s player.")
                    }
                    state.tab == "search" -> DesignSearch(catalog?.titles.orEmpty(), compact, state, onSelect = state::open)
                    catalog == null -> Column(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.Center) {
                        Text("CineDBKTV", color = Accent, style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(24.dp))
                        if (state.catalogError == null) { CircularProgressIndicator(color = Accent); Text("Loading selected catalogue…", Modifier.padding(top = 16.dp)) }
                        else { Text(state.catalogError.orEmpty()); Action("Retry catalog") { state.reload() } }
                    }
                    else -> CatalogueHome(catalog, state.tab, state)
                }
                }
            }
            if (state.warnings.isNotEmpty()) Text(state.warnings.joinToString("\n"), Modifier.align(Alignment.BottomStart).padding(start = 112.dp, bottom = 12.dp), color = Color(0xFFFFB300))
        }
    }
    if (state.loadingDetails || state.resolving) Dialog(onDismissRequest = state::cancel) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF101010), border = BorderStroke(1.dp, Accent.copy(alpha = .4f))) {
            Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { CircularProgressIndicator(color = Accent); Text(if (state.resolving) "Loading selected playback sources…" else "Loading details and episodes…"); Action("Cancel", onClick = state::cancel) }
        }
    }
    state.error?.let { message -> AlertDialog(onDismissRequest = { state.error = null }, title = { Text("Unable to load selection") }, text = { Text(message) }, confirmButton = { Action("Close") { state.error = null } }) }
    if (sourcePicker) AlertDialog(onDismissRequest = { sourcePicker = false }, title = { Text("Playback sources", color = Accent) }, confirmButton = { Action("Close") { sourcePicker = false } }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { state.streams.forEachIndexed { index, source -> item { Action((if (index == state.sourceIndex) "✓ " else "") + source.name) { state.sourceIndex = index; state.sourceNotice = null; sourcePicker = false } } } }
    })
    if (tester) Dialog(onDismissRequest = { tester = false }) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF101010), border = BorderStroke(1.dp, Accent.copy(alpha = .4f))) {
            LazyColumn(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { Text("Stream Extractor API", style = MaterialTheme.typography.titleLarge, color = Accent) }
                item { Text("Catalog providers: cineby.ws, dbku.tv and bingebang.st. Enter a direct media URL for an independent player test.") }
                item { OutlinedTextField(input, { input = it; validation = null }, Modifier.fillMaxWidth(), label = { Text("HTTPS MP4, HLS or DASH URL") }, singleLine = true, isError = validation != null) }
                validation?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
                item { Action("Play entered URL", primary = true) { validation = MediaUrl.validate(input); if (validation == null) { tester = false; state.direct(input.trim(), "User-provided media") } } }
                item { HorizontalDivider(color = Color.DarkGray); Text("Explicit playback samples (test only)", color = Accent) }
                MockData.titles.forEach { title -> title.episodes.forEach { episode -> item { Action(episode.title) { tester = false; state.direct(episode.url, episode.title) } } } }
                item { Action("Close") { tester = false } }
            }
        }
    }
}

