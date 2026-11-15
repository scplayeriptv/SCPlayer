package com.sc.player.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.R
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

/** اختيار المجموعات اللي عايز تخفيها من كل الشاشات. */
@Composable
internal fun HideGroupsDialog(
    groups: List<String>,
    hidden: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit,
) {
    var chosen by remember { mutableStateOf(hidden) }
    var query by remember { mutableStateOf("") }
    val shown = remember(groups, query) {
        val q = query.trim()
        if (q.isEmpty()) groups else groups.filter { it.contains(q, ignoreCase = true) }
    }
    val h = (LocalConfiguration.current.screenHeightDp * 0.45f).dp
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_hide_groups)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchPill(query, { query = it }, stringResource(R.string.search_hint), Modifier.fillMaxWidth().height(42.dp))
                LazyColumn(Modifier.fillMaxWidth().height(h)) {
                    items(shown) { g ->
                        val on = g in chosen
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { chosen = if (on) chosen - g else chosen + g }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(if (on) "☑" else "☐", color = if (on) Cyan else TextMuted, fontSize = 20.sp)
                            Text(g, color = TextMain, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(chosen) }) { Text(stringResource(R.string.done)) } },
        dismissButton = {
            Row {
                TextButton(onClick = { chosen = emptySet() }) { Text(stringResource(R.string.hide_show_all)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

/** اختبار القنوات المباشرة: اختيار النطاق، تقدّم، ونتيجة مع خيار إخفاء الميت. */
@Composable
internal fun ChannelCheckDialog(
    all: List<com.sc.player.data.Channel>,
    favs: Set<String>,
    previousDead: Set<String>,
    onDismiss: () -> Unit,
    onFinished: (Set<String>, Boolean) -> Unit,
) {
    val favList = remember(all, favs) { all.filter { it.url in favs } }
    var phase by remember { mutableStateOf(0) } // 0 اختيار، 1 شغّال، 2 خلص
    var target by remember { mutableStateOf<List<com.sc.player.data.Channel>>(emptyList()) }
    var done by remember { mutableStateOf(0) }
    var okN by remember { mutableStateOf(0) }
    var deadN by remember { mutableStateOf(0) }
    var unkN by remember { mutableStateOf(0) }
    val deadFound = remember { HashSet<String>() }
    val processed = remember { HashSet<String>() }

    androidx.compose.runtime.LaunchedEffect(phase) {
        if (phase == 1) {
            com.sc.player.data.ChannelChecker.check(target, 4).collect { (url, st) ->
                processed.add(url)
                done++
                when (st) {
                    com.sc.player.data.ChannelChecker.OK -> okN++
                    com.sc.player.data.ChannelChecker.DEAD -> { deadN++; deadFound.add(url) }
                    else -> unkN++
                }
            }
            phase = 2
        }
    }
    fun merged(): Set<String> = (previousDead - processed) + deadFound

    AlertDialog(
        onDismissRequest = { if (phase == 0) onDismiss() },
        title = { Text(stringResource(R.string.chk_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (phase) {
                    0 -> {
                        Text(stringResource(R.string.chk_intro), color = TextMuted, fontSize = 13.sp)
                        com.sc.player.ui.theme.GradientButton(
                            stringResource(R.string.chk_all, all.size),
                            Modifier.fillMaxWidth(),
                            onClick = { target = all; phase = 1 },
                        )
                        if (favList.isNotEmpty()) {
                            CategoryChip(stringResource(R.string.chk_favs, favList.size), false) { target = favList; phase = 1 }
                        }
                    }
                    1 -> {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { if (target.isEmpty()) 0f else done.toFloat() / target.size },
                            modifier = Modifier.fillMaxWidth(),
                            color = Cyan,
                        )
                        Text(stringResource(R.string.chk_progress, done, target.size), color = TextMain, fontSize = 14.sp)
                        Text(stringResource(R.string.chk_result, okN, deadN, unkN), color = TextMuted, fontSize = 13.sp)
                    }
                    else -> {
                        Text(stringResource(R.string.chk_progress, done, target.size), color = TextMain, fontSize = 14.sp)
                        Text(stringResource(R.string.chk_result, okN, deadN, unkN), color = TextMain, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            when (phase) {
                1 -> TextButton(onClick = { phase = 2 }) { Text(stringResource(R.string.chk_stop)) }
                2 -> if (deadN > 0) {
                    TextButton(onClick = { onFinished(merged(), true) }) { Text(stringResource(R.string.chk_hide)) }
                }
                else -> {}
            }
        },
        dismissButton = {
            when (phase) {
                0 -> TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                2 -> TextButton(onClick = { onFinished(merged(), false) }) { Text(stringResource(R.string.done)) }
                else -> {}
            }
        },
    )
}
