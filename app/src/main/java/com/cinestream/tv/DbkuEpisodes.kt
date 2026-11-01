package com.cinestream.tv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal const val EPISODES_PER_PAGE = 50
internal fun episodePage(index: Int) = index.coerceAtLeast(0) / EPISODES_PER_PAGE

@Composable
fun DbkuEpisodes(title: DesignTitle, onPlay: (DesignEpisode?) -> Unit) {
    var group by rememberSaveable(title.id) { mutableIntStateOf(0) }
    var page by rememberSaveable(title.id, group) { mutableIntStateOf(0) }
    var jump by rememberSaveable(title.id) { mutableStateOf("") }
    var jumpError by remember { mutableStateOf<String?>(null) }
    val catalogOnly = title.provider == "hongguo"
    val seasons = title.seasons
    val episodes = seasons[group.coerceIn(seasons.indices)].episodes
    val pages = episodes.chunked(EPISODES_PER_PAGE)
    Column(Modifier.fillMaxWidth().padding(horizontal = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("选集 · Episodes (${episodes.size})", style = MaterialTheme.typography.titleLarge, color = Accent)
        if (seasons.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(seasons.size) { index -> Action(seasons[index].name, primary = group == index, dense = true) { group = index; page = 0 } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(jump, { jump = it.filter(Char::isDigit); jumpError = null }, Modifier.width(200.dp), label = { Text("Jump to episode / 跳至集数") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
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
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(pages.size) { index ->
                val range = pages[index]
                Action("${range.first().number}–${range.last().number}", primary = index == page, dense = true) { page = index }
            }
        }
        pages.getOrNull(page)?.chunked(5)?.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { episode -> Action(episode.title, Modifier.weight(1f), primary = episode.number == jump.toIntOrNull(), dense = true) { if (catalogOnly) jump = episode.number.toString() else onPlay(episode) } }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
