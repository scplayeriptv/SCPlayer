package com.sc.player.ui.home

import com.sc.player.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.data.Channel
import com.sc.player.data.Episode
import com.sc.player.data.SeriesDetails
import com.sc.player.data.XtreamClient
import com.sc.player.data.friendlyError
import com.sc.player.ui.theme.BackButton
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

private sealed interface SeriesState {
    data object Loading : SeriesState
    data class Failed(val message: String) : SeriesState
    data class Ready(val details: SeriesDetails) : SeriesState
}

/** شاشة مسلسل: المواسم والحلقات. الضغط على حلقة بيشغّل باقي حلقات الموسم كقائمة. */
@Composable
fun SeriesScreen(entry: Channel, onBack: () -> Unit, onPlay: (List<Channel>, Int) -> Unit) {
    val ctx = LocalContext.current
    val state by produceState<SeriesState>(SeriesState.Loading, entry.url) {
        value = SeriesState.Loading
        value = XtreamClient(ctx.applicationContext).seriesInfo(entry).fold(
            onSuccess = { SeriesState.Ready(it) },
            onFailure = { SeriesState.Failed(friendlyError(ctx, it)) },
        )
    }

    Column(Modifier.fillMaxSize().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackButton(onBack)
            Text(
                entry.name,
                color = TextMain,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        when (val s = state) {
            SeriesState.Loading -> Message(stringResource(R.string.series_loading))
            is SeriesState.Failed -> Message(stringResource(R.string.series_failed, s.message))
            is SeriesState.Ready -> SeriesBody(entry, s.details, onPlay)
        }
    }
}

@Composable
private fun Message(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = TextMuted, fontSize = 15.sp, modifier = Modifier.padding(24.dp))
    }
}

@Composable
private fun SeriesBody(entry: Channel, d: SeriesDetails, onPlay: (List<Channel>, Int) -> Unit) {
    val seasonNumbers = remember(d) { d.seasons.keys.toList() }
    if (seasonNumbers.isEmpty()) {
        Message(stringResource(R.string.series_empty))
        return
    }
    var season by rememberSaveable(entry.url) { mutableStateOf(seasonNumbers.first()) }
    if (season !in d.seasons) season = seasonNumbers.first()
    val episodes = d.seasons[season].orEmpty()
    val asChannels = remember(episodes, entry) {
        episodes.map {
            Channel(
                name = "S%02dE%02d · %s".format(it.season, it.number, it.title),
                url = it.url,
                group = entry.name,
                logo = it.image ?: d.cover ?: entry.logo,
                tvgId = null,
            )
        }
    }

    Box(Modifier.padding(horizontal = 24.dp)) {
        MediaHeader(
            title = entry.name,
            poster = d.meta.poster ?: d.cover ?: entry.logo,
            meta = if (d.meta.plot.isBlank() && d.plot.isNotBlank()) com.sc.player.data.MediaInfo(plot = d.plot) else d.meta,
            posterWidth = 90.dp,
            plotLines = 4,
        )
    }
    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(seasonNumbers) { n ->
            CategoryChip(stringResource(R.string.series_season, n), n == season) { season = n }
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(episodes) { i, ep -> EpisodeRow(ep, asChannels[i]) { onPlay(asChannels, i) } }
    }
}

@Composable
private fun EpisodeRow(ep: Episode, ch: Channel, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) com.sc.player.ui.theme.Violet.copy(alpha = 0.2f) else Glass)
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else GlassBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChannelLogo(ch, 48.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${ep.number}. ${ep.title}",
                color = TextMain,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            ep.duration?.let { Text(it, color = TextMuted, fontSize = 12.sp) }
        }
        Text("▶", color = Cyan, fontSize = 18.sp)
    }
}
