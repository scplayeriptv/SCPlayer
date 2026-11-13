package com.sc.player.ui.home

import com.sc.player.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sc.player.data.Channel
import com.sc.player.data.MediaInfo
import com.sc.player.data.PositionStore
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.Ink
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

/** صورة بوستر بنسبة 2:3؛ لو مفيش صورة بنعرض أول حرف على خلفية متدرّجة. */
@Composable
internal fun PosterImage(url: String?, title: String, modifier: Modifier = Modifier) {
    var failed by remember(url) { mutableStateOf(false) }
    Box(modifier.background(BrandBrush), contentAlignment = Alignment.Center) {
        Text(
            title.trim().take(1).uppercase(),
            color = Ink.copy(alpha = 0.85f),
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        if (url != null && !failed) {
            AsyncImage(
                model = url,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** كارت بوستر في شبكة الأفلام/المسلسلات. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PosterCard(
    ch: Channel,
    fav: Boolean,
    onFocus: () -> Unit,
    onToggleFav: () -> Unit,
    onClick: () -> Unit,
) {
    val ctx = LocalContext.current
    val progress = remember(ch.url) { PositionStore(ctx).progress(ch.url) }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .graphicsLayer { scaleX = if (focused) 1.05f else 1f; scaleY = if (focused) 1.05f else 1f }
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onToggleFav),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(shape)
                .border(if (focused) 3.dp else 1.dp, if (focused) Cyan else GlassBorder, shape),
        ) {
            PosterImage(ch.logo, ch.name, Modifier.fillMaxSize())
            FavStar(fav, onToggleFav, Modifier.align(Alignment.TopEnd).padding(2.dp).background(Color(0x66000000), RoundedCornerShape(20.dp)))
            if (progress != null && progress > 0.01f) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color(0x99000000))) {
                    Box(Modifier.fillMaxWidth(progress).height(4.dp).background(Cyan))
                }
            }
        }
        Text(
            ch.name,
            color = TextMain,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** رأس شاشة التفاصيل: بوستر + سطر معلومات + نوع + قصة + ممثلين. */
@Composable
internal fun MediaHeader(
    title: String,
    poster: String?,
    meta: MediaInfo,
    posterWidth: Dp = 120.dp,
    plotLines: Int = 5,
    extra: @Composable () -> Unit = {},
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        PosterImage(
            poster ?: meta.poster,
            title,
            Modifier.width(posterWidth).aspectRatio(2f / 3f).clip(RoundedCornerShape(14.dp)).border(1.dp, GlassBorder, RoundedCornerShape(14.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val line = listOf(meta.year, meta.duration, if (meta.rating.isNotBlank()) "★ ${meta.rating}" else "")
                .filter { it.isNotBlank() }.joinToString("  ·  ")
            if (line.isNotEmpty()) Text(line, color = Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (meta.genre.isNotBlank()) Text(meta.genre, color = Cyan, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (meta.plot.isNotBlank()) Text(meta.plot, color = TextMuted, fontSize = 13.sp, maxLines = plotLines, overflow = TextOverflow.Ellipsis)
            if (meta.director.isNotBlank()) Text(stringResource(R.string.det_director, meta.director), color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (meta.cast.isNotBlank()) Text(stringResource(R.string.det_cast, meta.cast), color = TextMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            extra()
        }
    }
}
