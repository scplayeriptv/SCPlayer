package com.sc.player.ui.guide

import com.sc.player.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.data.Channel
import com.sc.player.data.PlayQueue
import com.sc.player.data.Reminder
import com.sc.player.data.ReminderStore
import com.sc.player.data.epg.EpgStore
import com.sc.player.data.epg.Programme
import com.sc.player.ui.home.ChannelLogo
import com.sc.player.ui.theme.BackButton
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.ScreenBackground
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import com.sc.player.ui.theme.Violet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DP_PER_MIN = 4.dp
private const val HALF_HOUR = 30 * 60_000L
private const val WINDOW_MS = 12 * 60 * 60_000L
private val ROW_H = 64.dp
private val CHAN_W = 104.dp
private val NowRed = Color(0xFFEF4444)

/** دليل البرامج: شبكة قنوات × وقت (12 ساعة قدّام) مع تذكيرات بالبرامج القادمة. */
@Composable
fun GuideScreen(onPlay: (Int) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val queue = PlayQueue.list
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val nowMs = remember { System.currentTimeMillis() }
    // بداية النافذة: نص ساعة قبل الوقت الحالي (مقرّب لأقرب نص ساعة)
    val winStart = remember { (nowMs / HALF_HOUR) * HALF_HOUR - HALF_HOUR }
    val winEnd = winStart + WINDOW_MS
    val totalW = DP_PER_MIN * (WINDOW_MS / 60_000L).toInt()

    val reminders = remember { ReminderStore(ctx) }
    var remTick by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Pair<Int, Programme>?>(null) }
    var showReminders by remember { mutableStateOf(false) }
    val hScroll = rememberScrollState()

    ScreenBackground {
        Column(Modifier.fillMaxSize().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BackButton(onBack)
                Text(
                    stringResource(R.string.guide_title),
                    color = TextMain,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                val count = remTick.let { reminders.list().size }
                Text(
                    "🔔 $count",
                    color = TextMain,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Glass)
                        .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                        .clickable(role = Role.Button) { showReminders = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
                Text(stringResource(R.string.channels_count, queue.size), color = TextMuted, fontSize = 12.sp)
            }
            if (!EpgStore.hasData()) {
                Text(
                    stringResource(R.string.guide_none),
                    color = TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            // شريط الوقت
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.width(CHAN_W))
                Box(Modifier.weight(1f).horizontalScroll(hScroll, enabled = false)) {
                    Box(Modifier.width(totalW).height(26.dp)) {
                        var t = winStart
                        while (t < winEnd) {
                            val x = DP_PER_MIN * ((t - winStart) / 60_000L).toInt()
                            Text(
                                fmt.format(Date(t)),
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.offset(x = x + 4.dp).align(Alignment.CenterStart),
                            )
                            t += HALF_HOUR
                        }
                    }
                }
            }

            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(queue) { i, ch ->
                    GuideRow(
                        ch = ch,
                        nowMs = nowMs,
                        winStart = winStart,
                        winEnd = winEnd,
                        totalW = totalW,
                        hScroll = hScroll,
                        fmt = fmt,
                        remTick = remTick,
                        hasReminder = { p -> reminders.has(ch.url, p.start) },
                        onChannel = { onPlay(i) },
                        onProgramme = { p -> selected = i to p },
                    )
                }
            }
        }
    }

    selected?.let { (i, p) ->
        val ch = queue[i]
        val future = p.start > System.currentTimeMillis()
        val has = remTick.let { reminders.has(ch.url, p.start) }
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(p.title, maxLines = 3, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${ch.name} · ${fmt.format(Date(p.start))}–${fmt.format(Date(p.stop))}", color = TextMuted, fontSize = 13.sp)
                    if (p.desc.isNotBlank()) {
                        Text(p.desc, fontSize = 13.sp, maxLines = 8, overflow = TextOverflow.Ellipsis)
                    }
                    if (future) {
                        TextButton(onClick = {
                            val r = Reminder(ch.url, ch.name, p.title, p.start)
                            if (has) reminders.remove(r) else reminders.add(r)
                            remTick++
                        }) {
                            Text(stringResource(if (has) R.string.guide_remind_off else R.string.guide_remind))
                        }
                        if (has) Text(stringResource(R.string.guide_reminder_set), color = Cyan, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selected = null; onPlay(i) }) { Text(stringResource(R.string.guide_watch)) }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text(stringResource(R.string.done)) } },
        )
    }

    if (showReminders) {
        val list = remTick.let { reminders.list() }
        AlertDialog(
            onDismissRequest = { showReminders = false },
            title = { Text(stringResource(R.string.guide_reminders)) },
            text = {
                if (list.isEmpty()) {
                    Text(stringResource(R.string.guide_no_reminders), color = TextMuted)
                } else {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        list.forEach { r ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(r.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${r.channelName} · " + SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date(r.start)),
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                TextButton(onClick = { reminders.remove(r); remTick++ }) { Text("✕") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showReminders = false }) { Text(stringResource(R.string.done)) } },
        )
    }
}

@Composable
private fun GuideRow(
    ch: Channel,
    nowMs: Long,
    winStart: Long,
    winEnd: Long,
    totalW: Dp,
    hScroll: androidx.compose.foundation.ScrollState,
    fmt: SimpleDateFormat,
    remTick: Int,
    hasReminder: (Programme) -> Boolean,
    onChannel: () -> Unit,
    onProgramme: (Programme) -> Unit,
) {
    val programmes = remember(ch.url) {
        EpgStore.forChannel(ch).filter { it.stop > winStart && it.start < winEnd }
    }
    Row(Modifier.fillMaxWidth().height(ROW_H)) {
        Row(
            Modifier
                .width(CHAN_W)
                .fillMaxHeight()
                .clickable(role = Role.Button, onClick = onChannel)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ChannelLogo(ch, 26.dp)
            Text(ch.name, color = TextMain, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.weight(1f).fillMaxHeight().horizontalScroll(hScroll)) {
            Box(Modifier.width(totalW).fillMaxHeight()) {
                if (programmes.isEmpty()) {
                    Text(
                        stringResource(R.string.guide_no_data_channel),
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
                    )
                }
                programmes.forEach { p ->
                    val s = maxOf(p.start, winStart)
                    val e = minOf(p.stop, winEnd)
                    val x = DP_PER_MIN * ((s - winStart) / 60_000f)
                    val w = (DP_PER_MIN * ((e - s) / 60_000f) - 3.dp).coerceAtLeast(8.dp)
                    ProgrammeBlock(
                        p = p,
                        isNow = nowMs >= p.start && nowMs < p.stop,
                        isPast = p.stop <= nowMs,
                        bell = remTick.let { hasReminder(p) },
                        fmt = fmt,
                        modifier = Modifier.offset(x = x).width(w),
                        onClick = { if (nowMs >= p.start && nowMs < p.stop) onChannel() else onProgramme(p) },
                    )
                }
                // خط "دلوقتي"
                Box(
                    Modifier
                        .offset(x = DP_PER_MIN * ((nowMs - winStart) / 60_000f))
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(NowRed),
                )
            }
        }
    }
}

@Composable
private fun ProgrammeBlock(
    p: Programme,
    isNow: Boolean,
    isPast: Boolean,
    bell: Boolean,
    fmt: SimpleDateFormat,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .fillMaxHeight()
            .padding(vertical = 2.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (isNow) Violet.copy(alpha = 0.55f) else if (isPast) Glass.copy(alpha = 0.5f) else Glass)
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else GlassBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            (if (bell) "🔔 " else "") + p.title,
            color = if (isPast) TextMuted else TextMain,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${fmt.format(Date(p.start))} – ${fmt.format(Date(p.stop))}",
            color = TextMuted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}
