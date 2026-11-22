package com.sc.player.ui.home

import com.sc.player.R
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sc.player.data.Channel
import com.sc.player.data.MediaInfo
import com.sc.player.data.PositionStore
import com.sc.player.data.XtreamClient
import com.sc.player.ui.theme.BackButton
import com.sc.player.ui.theme.GradientButton

/** تفاصيل فيلم: بوستر، قصة، تقييم، ممثلين، تريلر، وزر تشغيل/استكمال. */
@Composable
fun MovieDetailsScreen(
    list: List<Channel>,
    index: Int,
    fav: Boolean,
    onToggleFav: () -> Unit,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onOpenOther: (Int) -> Unit = {},
) {
    val ch = list[index]
    val ctx = LocalContext.current
    val meta by produceState(MediaInfo(), ch.url) {
        value = XtreamClient(ctx.applicationContext).vodInfo(ch).getOrDefault(MediaInfo())
    }
    val resumeAt = remember(ch.url) { PositionStore(ctx).resumeAt(ch.url) }
    val wide = LocalConfiguration.current.screenWidthDp >= 720

    // أفلام مشابهة: من نفس القائمة (التصنيف المفتوح)، بادئين من اللي بعد الفيلم الحالي
    val similar = remember(list, index) {
        val after = (index + 1 until list.size).map { it }
        val before = (0 until index).map { it }
        (after + before).take(24)
    }

    var askQuality by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    if (askQuality) {
        com.sc.player.ui.downloads.QualityDialog(
            onPick = { h -> askQuality = false; download(ctx, ch, h) },
            onClose = { askQuality = false },
        )
    }

    androidx.compose.runtime.key(ch.url) {
    Box(Modifier.fillMaxSize()) {
        (meta.backdrop ?: meta.poster ?: ch.logo)?.let {
            AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xAA0B0714), Color(0xF20B0714))),
                ),
            )
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BackButton(onBack)
            MediaHeader(
                title = ch.name,
                poster = ch.logo,
                meta = meta,
                posterWidth = if (wide) 200.dp else 120.dp,
                plotLines = if (wide) 8 else 5,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    DetailActions(
                        resumeAt = resumeAt,
                        fav = fav,
                        trailer = meta.trailer,
                        onPlay = onPlay,
                        onToggleFav = onToggleFav,
                        onDownload = { askQuality = true },
                        wide = wide,
                    )
                }
            }
            if (!wide) {
                // على الموبايل الأزرار تحت العنوان وتاخد العرض كله
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientButton(
                        if (resumeAt != null) stringResource(R.string.det_resume, clock(resumeAt)) else stringResource(R.string.play),
                        Modifier.fillMaxWidth(),
                        onClick = onPlay,
                    )
                    CategoryChip((if (!com.sc.player.data.Premium.active) "🔒 " else "") + stringResource(R.string.dl_download), false) { askQuality = true }
                }
            }
            if (similar.isNotEmpty()) {
                androidx.compose.material3.Text(
                    stringResource(R.string.det_more_like),
                    color = com.sc.player.ui.theme.TextMain,
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(similar) { j ->
                        Box(Modifier.width(104.dp)) {
                            PosterCard(ch = list[j], fav = false, onFocus = {}, onToggleFav = {}) { onOpenOther(j) }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun DetailActions(
    resumeAt: Long?,
    fav: Boolean,
    trailer: String,
    onPlay: () -> Unit,
    onToggleFav: () -> Unit,
    onDownload: () -> Unit,
    wide: Boolean,
) {
    val ctx = LocalContext.current
    if (wide) {
        GradientButton(
            if (resumeAt != null) stringResource(R.string.det_resume, clock(resumeAt)) else stringResource(R.string.play),
            onClick = onPlay,
        )
    }
    CategoryChip(stringResource(R.string.dl_download), false, onDownload)
    CategoryChip(if (fav) stringResource(R.string.in_favorites) else stringResource(R.string.add_favorites), fav, onToggleFav)
    if (trailer.isNotBlank()) {
        CategoryChip(stringResource(R.string.det_trailer), false) {
            val url = if (trailer.startsWith("http")) trailer else "https://www.youtube.com/watch?v=$trailer"
            runCatching {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}

private fun download(ctx: android.content.Context, ch: Channel, maxHeight: Int) {
    val added = com.sc.player.data.Downloads.enqueue(ctx, ch.name, ch.url, ch.logo, 0, 0, maxHeight)
    android.widget.Toast.makeText(ctx, if (added) R.string.dl_added else R.string.dl_exists, android.widget.Toast.LENGTH_SHORT).show()
}

private fun clock(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}
