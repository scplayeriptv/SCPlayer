package com.sc.player.ui.guide

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.data.Channel
import com.sc.player.data.PlayQueue
import com.sc.player.data.epg.EpgStore
import com.sc.player.data.epg.Programme
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.ScreenBackground
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GuideScreen(onPlay: (Int) -> Unit, onBack: () -> Unit) {
    val queue = PlayQueue.list
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val nowMs = remember { System.currentTimeMillis() }

    ScreenBackground {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GuideButton("رجوع", onBack)
                Text(
                    "دليل البرامج",
                    color = TextMain,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                Text("${queue.size} قناة", color = TextMuted, fontSize = 13.sp)
            }
            if (!EpgStore.hasData()) {
                Text(
                    "مفيش دليل متاح لهذه القائمة. الدليل بيتحمّل من رابط XMLTV (Xtream بيجيبه لوحده، وM3U لازم يكون فيه url-tvg).",
                    color = TextMuted,
                    fontSize = 14.sp,
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                itemsIndexed(queue) { i, ch ->
                    GuideRow(
                        ch = ch,
                        now = EpgStore.now(ch, nowMs),
                        next = EpgStore.next(ch, nowMs),
                        nowMs = nowMs,
                        fmt = fmt,
                        onClick = { onPlay(i) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideRow(
    ch: Channel,
    now: Programme?,
    next: Programme?,
    nowMs: Long,
    fmt: SimpleDateFormat,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .onFocusChanged { focused = it.isFocused }
            .fillMaxWidth()
            .clip(shape)
            .background(Glass)
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            ch.name,
            color = TextMain,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (now == null) {
            Text("مفيش بيانات برامج لهذه القناة", color = TextMuted, fontSize = 13.sp)
        } else {
            Text(
                "الآن: ${now.title}  (${fmt.format(Date(now.start))} - ${fmt.format(Date(now.stop))})",
                color = Cyan,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val total = (now.stop - now.start).coerceAtLeast(1L)
            val fraction = ((nowMs - now.start).toFloat() / total).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { fraction },
                color = Cyan,
                trackColor = GlassBorder,
                modifier = Modifier.fillMaxWidth(),
            )
            if (next != null) {
                Text(
                    "بعده: ${next.title}  (${fmt.format(Date(next.start))})",
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun GuideButton(text: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(Glass)
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}
