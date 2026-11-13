package com.sc.player.ui.multi

import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.sc.player.R
import com.sc.player.data.Channel
import com.sc.player.data.PlayQueue
import com.sc.player.ui.home.CategoryChip
import com.sc.player.ui.theme.BackButton
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.Ink
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

/** Multi-view: لحد 4 قنوات مع بعض. الصوت للخانة المختارة بس. */
@Composable
fun MultiViewScreen(onFullscreen: (Int) -> Unit, onBack: () -> Unit) {
    val queue = PlayQueue.list
    val view = LocalView.current
    val landscape = LocalConfiguration.current.let { it.screenWidthDp > it.screenHeightDp }
    var count by remember { mutableIntStateOf(if (landscape) 4 else 2) }
    val cells = remember { mutableStateListOf(-1, -1, -1, -1) }
    var audio by remember { mutableIntStateOf(0) }
    var pickFor by remember { mutableIntStateOf(-1) }
    var menuFor by remember { mutableIntStateOf(-1) }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Column(Modifier.fillMaxSize().background(Ink).padding(8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BackButton(onBack)
            Text(
                stringResource(R.string.multi_view),
                color = TextMain,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            CategoryChip("2", count == 2) { count = 2; if (audio >= 2) audio = 0 }
            CategoryChip("4", count == 4) { count = 4 }
        }

        val cols = if (count == 4 || landscape) 2 else 1
        val rows = count / cols
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(rows) { r ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(cols) { c ->
                        val slot = r * cols + c
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            val q = cells[slot]
                            val ch = queue.getOrNull(q)
                            Cell(
                                channel = ch,
                                selected = audio == slot,
                                onClick = {
                                    when {
                                        ch == null -> pickFor = slot
                                        audio != slot -> audio = slot
                                        else -> menuFor = slot
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (pickFor >= 0) {
        Picker(
            queue = queue,
            onPick = { idx -> cells[pickFor] = idx; audio = pickFor; pickFor = -1 },
            onDismiss = { pickFor = -1 },
        )
    }
    if (menuFor >= 0) {
        val slot = menuFor
        AlertDialog(
            onDismissRequest = { menuFor = -1 },
            title = { Text(queue.getOrNull(cells[slot])?.name.orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    TextButton(onClick = { menuFor = -1; pickFor = slot }) { Text(stringResource(R.string.multi_change)) }
                    TextButton(onClick = { val i = cells[slot]; menuFor = -1; onFullscreen(i) }) { Text(stringResource(R.string.multi_fullscreen)) }
                    TextButton(onClick = { cells[slot] = -1; menuFor = -1 }) { Text(stringResource(R.string.multi_remove)) }
                }
            },
            confirmButton = { TextButton(onClick = { menuFor = -1 }) { Text(stringResource(R.string.done)) } },
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun Cell(channel: Channel?, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .fillMaxSize()
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(Glass)
            .border(if (selected || focused) 3.dp else 1.dp, if (selected || focused) Cyan else GlassBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (channel == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("＋", color = Cyan, fontSize = 36.sp)
                Text(stringResource(R.string.multi_add), color = TextMuted, fontSize = 13.sp)
            }
            return@Box
        }
        val ctx = LocalContext.current
        var buffering by remember(channel.url) { mutableStateOf(true) }
        var failed by remember(channel.url) { mutableStateOf(false) }
        val player = remember(channel.url) {
            val http = DefaultHttpDataSource.Factory()
                .setUserAgent("SCPlayer/0.1 (Android)")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(20_000)
            ExoPlayer.Builder(ctx)
                .setMediaSourceFactory(DefaultMediaSourceFactory(ctx).setDataSourceFactory(http))
                .build()
                .apply {
                    setMediaItem(MediaItem.fromUri(channel.url))
                    prepare()
                    playWhenReady = true
                }
        }
        DisposableEffect(player) {
            val l = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    buffering = state == Player.STATE_BUFFERING
                    if (state == Player.STATE_READY) failed = false
                }

                override fun onPlayerError(error: PlaybackException) {
                    failed = true
                    buffering = false
                }
            }
            player.addListener(l)
            onDispose { player.removeListener(l); player.release() }
        }
        LaunchedEffect(player, selected) { player.volume = if (selected) 1f else 0f }

        AndroidView(
            factory = { c ->
                PlayerView(c).apply {
                    useController = false
                    this.player = player
                    isFocusable = false
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )
        if (buffering && !failed) CircularProgressIndicator(color = Cyan, modifier = Modifier.padding(8.dp))
        if (failed) Text("⚠", color = Cyan, fontSize = 32.sp)
        Text(
            (if (selected) "🔊 " else "") + channel.name,
            color = TextMain,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color(0x99000000))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun Picker(queue: List<Channel>, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val shown = remember(q, queue) {
        queue.withIndex().filter { q.isBlank() || it.value.name.contains(q, ignoreCase = true) }.take(300)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.multi_pick)) },
        text = {
            Column {
                OutlinedTextField(
                    value = q,
                    onValueChange = { q = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.nav_search)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 320.dp).padding(top = 8.dp)) {
                    items(shown) { (idx, ch) ->
                        Text(
                            ch.name,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().clickable { onPick(idx) }.padding(vertical = 12.dp, horizontal = 4.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}
