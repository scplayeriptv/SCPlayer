package com.sc.player.ui.downloads

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sc.player.R
import com.sc.player.data.Channel
import com.sc.player.data.DownloadItem
import com.sc.player.data.DownloadStore
import com.sc.player.data.Downloads
import com.sc.player.data.PlayQueue
import com.sc.player.ui.theme.BackButton
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.ScreenBackground
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

/** شاشة التحميلات والتسجيلات: تقدّم، تشغيل، إيقاف/استكمال، حذف. */
@Composable
fun DownloadsScreen(onPlay: () -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { DownloadStore.load(ctx.applicationContext) }
    val list by DownloadStore.items.collectAsState()
    val sStore = remember { com.sc.player.data.ScheduleStore(ctx) }
    var sTick by remember { mutableIntStateOf(0) }
    val scheds = remember(sTick) { sStore.list() }
    ScreenBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                BackButton(onBack)
                Text(stringResource(R.string.dl_title), color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            var showLock by remember { mutableStateOf(false) }
            if (!com.sc.player.data.Premium.active) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Glass)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .clickable { showLock = true }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("🔒", fontSize = 18.sp)
                    Text(stringResource(R.string.premium_banner), color = TextMain, fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
            }
            if (showLock) ActivationDialog { showLock = false }
            if (list.isEmpty() && scheds.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.dl_empty), color = TextMuted, fontSize = 15.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (scheds.isNotEmpty()) {
                        item(key = "sched_h") {
                            Text(stringResource(R.string.rec_sched_title), color = Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        items(scheds, key = { "s" + it.key }) { r ->
                            ScheduledRow(r) { sStore.remove(r); sTick++ }
                        }
                    }
                    items(list, key = { it.id }) { item ->
                        DownloadRow(item, onPlay = {
                            PlayQueue.list = listOf(Channel(item.title, "file://" + item.path, "", item.logo, null))
                            onPlay()
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(item: DownloadItem, onPlay: () -> Unit) {
    val ctx = LocalContext.current
    val shape = RoundedCornerShape(16.dp)
    val sizeTxt = Formatter.formatShortFileSize(ctx, item.bytes)
    val totalTxt = if (item.total > 0) Formatter.formatShortFileSize(ctx, item.total) else null
    val status = when (item.state) {
        0 -> stringResource(R.string.dl_state_queued)
        1 -> if (item.kind == 0) "$sizeTxt" + (totalTxt?.let { " / $it" } ?: "") else stringResource(R.string.dl_recording) + " · $sizeTxt"
        2 -> stringResource(R.string.dl_state_done) + " · $sizeTxt"
        3 -> if (item.error == "hls") stringResource(R.string.dl_hls) else stringResource(R.string.dl_state_failed) + (if (item.error.isNotBlank()) " · " + item.error else "")
        else -> stringResource(R.string.dl_state_stopped) + " · $sizeTxt"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Glass)
            .border(1.dp, GlassBorder, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Glass), contentAlignment = Alignment.Center) {
            if (item.logo != null) AsyncImage(item.logo, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Text(if (item.kind == 0) "🎬" else "⏺", fontSize = 22.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(status, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if ((item.state == 1 || item.state == 4) && item.total > 0) {
                LinearProgressIndicator(
                    progress = { (item.bytes.toFloat() / item.total).coerceIn(0f, 1f) },
                    color = Cyan,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (item.state == 1 || item.state == 0) {
                LinearProgressIndicator(color = Cyan, modifier = Modifier.fillMaxWidth())
            }
        }
        when (item.state) {
            2 -> Action("▶", stringResource(R.string.dl_play)) { onPlay() }
            1, 0 -> Action("⏹", stringResource(if (item.kind == 0) R.string.dl_stop else R.string.dl_stop_save)) { Downloads.stop(ctx, item.id) }
            else -> if (item.kind == 0) Action("↻", stringResource(R.string.dl_resume)) { Downloads.resume(ctx, item.id) }
        }
        Action("🗑", stringResource(R.string.dl_delete)) { Downloads.delete(ctx, item.id) }
    }
}

@Composable
private fun Action(glyph: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Glass)
            .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 18.sp, color = TextMain)
    }
}

@Composable
private fun ScheduledRow(r: com.sc.player.data.ScheduledRec, onCancel: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val fmt = java.text.SimpleDateFormat("EEE HH:mm", java.util.Locale.getDefault())
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Glass).border(1.dp, GlassBorder, shape).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("⏰", fontSize = 22.sp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(r.title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${r.channelName} · ${fmt.format(java.util.Date(r.start))}",
                color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Action("✕", stringResource(R.string.rec_sched_off)) { onCancel() }
    }
}
