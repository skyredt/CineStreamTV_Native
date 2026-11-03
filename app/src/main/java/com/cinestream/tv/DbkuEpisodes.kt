package com.cinestream.tv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

internal const val EPISODES_PER_PAGE = 50
internal fun episodePage(index: Int) = index.coerceAtLeast(0) / EPISODES_PER_PAGE

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DbkuEpisodes(title: DesignTitle, onPlay: (DesignEpisode?) -> Unit, history: ViewingEntry? = null, restoreEpisodeId: String? = null) {
    var group by rememberSaveable(title.id) { mutableIntStateOf(0) }
    var page by rememberSaveable(title.id, group) { mutableIntStateOf(0) }
    var jump by rememberSaveable(title.id) { mutableStateOf("") }
    var jumpError by remember { mutableStateOf<String?>(null) }
    val catalogOnly = title.provider == "hongguo"
    val seasons = title.seasons
    val episodes = seasons[group.coerceIn(seasons.indices)].episodes
    val pages = episodes.chunked(EPISODES_PER_PAGE)
    val keyboard = LocalSoftwareKeyboardController.current
    val rangeFocus = remember { FocusRequester() }
    val rangeState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val imeVisible = WindowInsets.isImeVisible
    val leaveInput: () -> Unit = {
        keyboard?.hide()
        if (pages.isNotEmpty()) scope.launch { rangeState.scrollToItem(page.coerceIn(pages.indices)); withFrameNanos { }; rangeFocus.requestFocus() }
    }
    BackHandler(imeVisible, onBack = leaveInput)
    val episodeFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (restoreEpisodeId != null) { withFrameNanos { }; runCatching { episodeFocus.requestFocus() } }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("选集 · Episodes (${episodes.size})", style = MaterialTheme.typography.titleLarge, color = Accent)
        if (seasons.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(seasons.size) { index -> Action(seasons[index].name, primary = group == index, dense = true) { group = index; page = 0 } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(jump, { jump = it.filter(Char::isDigit); jumpError = null }, Modifier.width(200.dp), label = { Text("Jump to episode / 跳至集数") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { leaveInput() }))
            Action("Go / 跳转", dense = true) {
                val index = episodes.indexOfFirst { it.number == jump.toIntOrNull() }
                if (index >= 0) { page = episodePage(index); jumpError = null } else jumpError = "Episode not listed / 未找到该集"
            }
        }
        jumpError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (catalogOnly) {
            Text(title.playbackNotice, color = Muted)
            episodes.firstOrNull { it.number == jump.toIntOrNull() }?.let { Text("${it.title} · ${it.availability}", color = Accent) }
        }
        LazyRow(state = rangeState, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(pages.size) { index ->
                val range = pages[index]
                Action("${range.first().number}–${range.last().number}", primary = index == page, dense = true, requester = if (index == page) rangeFocus else null) { page = index }
            }
        }
        pages.getOrNull(page)?.chunked(5)?.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { episode -> Action((if (episode.id == history?.episodeId) "▶ " else "") + episode.title, Modifier.weight(1f).then(if (episode.id == restoreEpisodeId) Modifier.focusRequester(episodeFocus) else Modifier), primary = episode.number == jump.toIntOrNull(), dense = true) { if (catalogOnly) jump = episode.number.toString() else onPlay(episode) } }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
