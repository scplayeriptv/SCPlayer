package com.sc.player.ui.home

import com.sc.player.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sc.player.data.Channel
import com.sc.player.data.epg.EpgStore
import com.sc.player.data.epg.Programme
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.Ink
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

internal val Gold = Color(0xFFFBBF24)
internal val LiveRed = Color(0xFFEF4444)

/** البرنامج الحالي للقناة (الـ tick بس عشان الواجهة تتحدّث لما الدليل يتحمّل). */
@Suppress("UNUSED_PARAMETER")
internal fun epgNow(ch: Channel, tick: Int): Programme? = EpgStore.now(ch, System.currentTimeMillis())

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
internal fun ChannelLogo(ch: Channel, boxSize: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(boxSize), contentAlignment = Alignment.Center) {
        var failed by remember(ch.logo) { mutableStateOf(false) }
        if (ch.logo == null || failed) {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(BrandBrush), contentAlignment = Alignment.Center) {
                Text(
                    ch.name.trim().take(1).uppercase(),
                    color = Ink,
                    fontSize = (boxSize.value * 0.4f).sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        } else {
            AsyncImage(
                model = ch.logo,
                contentDescription = ch.name,
                contentScale = ContentScale.Fit,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun LiveBadge() {
    Box(
        Modifier.clip(RoundedCornerShape(6.dp)).background(LiveRed).padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text("LIVE", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
internal fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    val look = if (selected) Modifier.background(BrandBrush) else Modifier.background(Glass)
    Box(
        Modifier
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer { scaleX = if (focused) 1.06f else 1f; scaleY = if (focused) 1.06f else 1f }
            .clip(shape)
            .then(look)
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else GlassBorder, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) Ink else TextMain,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}

/** كارت قناة صغير (صف "Continue Watching"). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChannelCard(ch: Channel, now: Programme?, fav: Boolean, onToggleFav: () -> Unit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer { scaleX = if (focused) 1.05f else 1f; scaleY = if (focused) 1.05f else 1f }
            .clip(shape)
            .background(Glass)
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else GlassBorder, shape)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onToggleFav),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ChannelLogo(ch, 64.dp)
            Text(
                ch.name,
                color = TextMain,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                now?.let { stringResource(R.string.now_prefix, it.title) } ?: ch.group,
                color = if (now != null) Cyan else TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        FavStar(fav, onToggleFav, Modifier.align(Alignment.TopEnd).padding(4.dp))
    }
}

@Composable
internal fun FavStar(fav: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (fav) "★" else "☆", color = if (fav) Gold else TextMuted, fontSize = 20.sp)
    }
}

/** صف قناة في القائمة (Live TV / Movies / ...). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChannelRow(
    ch: Channel,
    now: Programme?,
    fav: Boolean,
    live: Boolean,
    onFocus: () -> Unit,
    onToggleFav: () -> Unit,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .clip(shape)
            .background(if (focused) com.sc.player.ui.theme.Violet.copy(alpha = 0.2f) else Glass)
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else GlassBorder, shape)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onToggleFav)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChannelLogo(ch, 48.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                ch.name,
                color = TextMain,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                now?.let { stringResource(R.string.now_prefix, it.title) } ?: ch.group,
                color = if (now != null) Cyan else TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (live) LiveBadge()
        FavStar(fav, onToggleFav)
    }
}

@Composable
internal fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(modifier.clip(shape).background(Glass).border(1.dp, GlassBorder, shape)) { content() }
}

@Composable
internal fun ScreenTitle(text: String) {
    Text(text, color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
internal fun Spacer8() {
    Box(Modifier.height(8.dp))
}
