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
import androidx.compose.foundation.layout.width
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

/** صف قناة في القائمة (Live TV / Radio): لوجو في بلاطة، شريط تقدّم للبرنامج الحالي، وتوهّج عند التحديد. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChannelRow(
    ch: Channel,
    now: Programme?,
    fav: Boolean,
    live: Boolean,
    onFocus: () -> Unit,
    onToggleFav: () -> Unit,
    number: Int? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    val bg = if (focused) {
        androidx.compose.ui.graphics.Brush.horizontalGradient(
            listOf(com.sc.player.ui.theme.Violet.copy(alpha = 0.40f), Cyan.copy(alpha = 0.12f)),
        )
    } else {
        androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Glass, Glass))
    }
    val stroke: androidx.compose.ui.graphics.Brush = if (focused) {
        androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Cyan, com.sc.player.ui.theme.Violet))
    } else androidx.compose.ui.graphics.SolidColor(GlassBorder)
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .graphicsLayer { scaleX = if (focused) 1.012f else 1f; scaleY = if (focused) 1.012f else 1f }
            .clip(shape)
            .background(bg)
            .border(if (focused) 2.dp else 1.dp, stroke, shape)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onToggleFav)
            .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (number != null) {
            Text(
                number.toString(),
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.width(34.dp),
            )
        }
        Box(
            Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x33000000)),
            contentAlignment = Alignment.Center,
        ) { ChannelLogo(ch, 42.dp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                ch.name,
                color = TextMain,
                fontWeight = FontWeight.Bold,
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
            if (now != null && now.stop > now.start) {
                val frac = ((System.currentTimeMillis() - now.start).toFloat() / (now.stop - now.start).toFloat()).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x33FFFFFF))) {
                    Box(Modifier.fillMaxWidth(frac).height(3.dp).background(BrandBrush))
                }
            }
        }
        if (live) LiveBadge()
        FavStar(fav, onToggleFav)
    }
}

/** خانة بحث على شكل كبسولة زجاجية. */
@Composable
internal fun SearchPill(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier
            .height(48.dp)
            .clip(shape)
            .background(Glass)
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else GlassBorder, shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("🔍", fontSize = 16.sp)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(hint, color = TextMuted, fontSize = 15.sp, maxLines = 1)
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = TextMain, fontSize = 15.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Cyan),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            )
        }
        if (value.isNotEmpty()) {
            Text(
                "✕",
                color = TextMuted,
                fontSize = 16.sp,
                modifier = Modifier.clip(CircleShape).clickable { onChange("") }.padding(6.dp),
            )
        }
    }
}

@Composable
internal fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(modifier.clip(shape).background(Glass).border(1.dp, GlassBorder, shape)) { content() }
}

@Composable
internal fun ScreenTitle(text: String, subtitle: String? = null) {
    Column(Modifier.padding(bottom = 2.dp)) {
        Text(text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, style = androidx.compose.ui.text.TextStyle(brush = BrandBrush))
        if (subtitle != null) Text(subtitle, color = TextMuted, fontSize = 12.sp)
    }
}

@Composable
internal fun Spacer8() {
    Box(Modifier.height(8.dp))
}
