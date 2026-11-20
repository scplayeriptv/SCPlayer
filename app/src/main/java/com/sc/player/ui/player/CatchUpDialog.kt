package com.sc.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.R
import com.sc.player.data.CatchUpItem
import com.sc.player.data.Channel
import com.sc.player.data.XtreamClient
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** البرامج اللي فاتت للقناة؛ اللمس على برنامج مسجّل بيشغّله من البداية (لو السيرفر بيدعم الأرشيف). */
@Composable
internal fun CatchUpDialog(channel: Channel, onPlay: (CatchUpItem) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var result by remember { mutableStateOf<Result<List<CatchUpItem>>?>(null) }
    LaunchedEffect(channel.url) { result = XtreamClient(ctx.applicationContext).catchUp(channel) }
    val hm = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val day = remember { SimpleDateFormat("EEEE d MMM", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.catchup_title) + " — " + channel.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            val r = result
            Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                when {
                    r == null -> CircularProgressIndicator(color = Cyan)
                    r.isFailure -> Text(stringResource(R.string.catchup_error), color = TextMuted, fontSize = 14.sp)
                    r.getOrNull().orEmpty().none { it.url != null } -> Text(stringResource(R.string.catchup_none), color = TextMuted, fontSize = 14.sp)
                    else -> {
                        val list = r.getOrNull().orEmpty()
                        LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(list.size) { i ->
                                val it = list[i]
                                val d = day.format(Date(it.start))
                                if (i == 0 || d != day.format(Date(list[i - 1].start))) {
                                    Text(d, color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                                }
                                val playable = it.url != null
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .alpha(if (playable) 1f else 0.4f)
                                        .clickable(enabled = playable) { onPlay(it) }
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(hm.format(Date(it.start)), color = TextMuted, fontSize = 12.sp)
                                    Text(it.title, color = TextMain, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    if (playable) Text("▶", color = Cyan, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.diag_close)) } },
    )
}
