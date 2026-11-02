package com.cinestream.tv

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import org.json.JSONArray
import org.json.JSONObject

enum class CatalogueMode(val label: String) {
    ENGLISH("English"), CHINESE("中文");
    fun accepts(provider: String) = if (this == ENGLISH) provider in setOf("cineby", "bingebang") else provider == "dbku"
}

data class ViewingEntry(val title: DesignTitle, val episodeId: String?, val position: Long, val updated: Long)

/** Local provider-qualified identities; embedded players only resume when they report position. */
class CataloguePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("catalogue", Context.MODE_PRIVATE)
    var mode: CatalogueMode?
        get() = CatalogueMode.entries.find { it.name == prefs.getString("mode", null) }
        set(value) { prefs.edit().putString("mode", value?.name).apply() }
    fun readHistory(): List<ViewingEntry> = runCatching {
        val data = JSONArray(prefs.getString("history", "[]"))
        (0 until data.length()).map { index ->
            val item = data.getJSONObject(index)
            val title = liveTitle(item.getString("provider"), item.getString("url"), item.getString("title"), item.optString("poster"), tv = item.optBoolean("tv"))
                .copy(id = item.getString("id"))
            ViewingEntry(title, item.optString("episode").takeIf { it.isNotBlank() }, item.optLong("position"), item.optLong("updated"))
        }.filter { it.title.provider in setOf("cineby", "bingebang", "dbku") }
    }.getOrDefault(emptyList())
    fun writeHistory(entries: List<ViewingEntry>) {
        val data = JSONArray()
        entries.take(100).forEach { entry -> data.put(JSONObject().put("id", entry.title.id).put("provider", entry.title.provider)
            .put("url", entry.title.pageUrl).put("title", entry.title.title).put("poster", entry.title.poster).put("tv", entry.title.isTv)
            .put("episode", entry.episodeId.orEmpty()).put("position", entry.position).put("updated", entry.updated)) }
        prefs.edit().putString("history", data.toString()).apply()
    }
}

@Composable
fun CatalogueChooser(onSelect: (CatalogueMode) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }
    Column(Modifier.fillMaxSize().padding(64.dp), verticalArrangement = Arrangement.Center) {
        Text("Choose your catalogue / 选择片库", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))
        Action("English · Cineby + BingeBang", requester = first) { onSelect(CatalogueMode.ENGLISH) }
        Spacer(Modifier.height(16.dp))
        Action("中文 · 独播库 DBKU") { onSelect(CatalogueMode.CHINESE) }
        Text("Catalogue preference is separate from interface language.", Modifier.padding(top = 20.dp))
    }
}

internal fun catalogueRows(catalog: DesignCatalog, tab: String): List<DesignRow> = when (tab) {
    "home" -> catalog.rows
    "movies", "series" -> catalog.rows.map { it.copy(titles = it.titles.filter { title -> title.isTv == (tab == "series") }) }.filter { it.titles.isNotEmpty() }
    "new" -> catalog.rows.filter { it.id == "new-releases" }
    "trending" -> catalog.rows.filter { it.id.startsWith("bingebang-") }
    else -> catalog.rows.filter { it.id == tab }
}

@Composable
fun CatalogueHome(catalog: DesignCatalog, tab: String, state: StreamingState) {
    val rows = catalogueRows(catalog, tab)
    val first = remember { FocusRequester() }
    val list = rememberLazyListState()
    val restoreId = state.homeFocusId
    val restoreRow = state.homeFocusRow
    LaunchedEffect(tab) {
        if (restoreId != null && rows.any { row -> row.id == restoreRow && row.titles.any { it.id == restoreId } }) {
            list.scrollToItem(rows.indexOfFirst { it.id == restoreRow } + 1)
        }
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 28.dp), state = list, verticalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(state.catalogueMode?.label.orEmpty(), color = Accent)
                val featured = rows.flatMap { it.titles }.firstOrNull { it.featured } ?: rows.firstOrNull()?.titles?.firstOrNull()
                if (featured != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        OriginalArtwork(featured.backdrop, featured.title, Modifier.width(320.dp).height(180.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(featured.title, style = MaterialTheme.typography.headlineLarge)
                            Text(featured.provider.uppercase(), color = Accent)
                            Text(featured.overview, maxLines = 3)
                            Action("Details & Play", requester = if (restoreId == null) first else null) { state.homeFocusId = null; state.open(featured) }
                        }
                    }
                } else Text("No titles available in this category.")
            }
        }
        items(rows, key = { it.id }) { row ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(row.title, style = MaterialTheme.typography.titleLarge)
                val index = if (row.id == restoreRow) row.titles.indexOfFirst { it.id == restoreId }.coerceAtLeast(0) else 0
                val rowState = rememberLazyListState(index)
                LazyRow(state = rowState, horizontalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(8.dp)) {
                    items(row.titles, key = { it.id }) { title ->
                        DesignPoster(title, { state.homeFocusId = it.id; state.homeFocusRow = row.id; state.open(it) },
                            (if (title.id == restoreId && row.id == restoreRow) Modifier.focusRequester(first) else Modifier)
                                .onFocusChanged { if (it.isFocused) { state.homeFocusId = title.id; state.homeFocusRow = row.id } })
                    }
                }
            }
        }
    }
}

@Composable
fun ViewingHistory(state: StreamingState) {
    val entries = state.history.filter { state.catalogueMode?.accepts(it.title.provider) == true }
    var lastTitle by rememberSaveable { mutableStateOf<String?>(null) }
    val restore = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; runCatching { restore.requestFocus() } }
    LazyColumn(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Watch History / 观看记录", style = MaterialTheme.typography.headlineMedium) }
        item { Text("Open a title to continue. Exact resume is available only when its player reports progress.") }
        item { Action("Clear this catalogue’s history", requester = if (lastTitle == null || entries.none { it.title.id == lastTitle }) restore else null) { lastTitle = null; state.clearHistory() } }
        if (entries.isEmpty()) item { Text("No viewing history yet.") }
        items(entries, key = { it.title.id }) { entry ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Action("${entry.title.title} · ${entry.title.provider.uppercase()}${if (entry.position > 0) " · ${entry.position / 60000} min" else ""}", Modifier.weight(1f), requester = if (lastTitle == entry.title.id) restore else null) { lastTitle = entry.title.id; state.open(entry.title) }
                Action("Remove") { state.removeHistory(entry.title.id) }
            }
        }
    }
}
