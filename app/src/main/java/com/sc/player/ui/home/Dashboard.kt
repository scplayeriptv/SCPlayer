package com.sc.player.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.data.Channel
import com.sc.player.data.Kind
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.GradientButton
import com.sc.player.ui.theme.Ink
import com.sc.player.ui.theme.SCLogo
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

private class Tile(val dest: Dest, val kind: Kind, val emoji: String, val subtitle: String, val from: Color, val to: Color)

private val tiles = listOf(
    Tile(Dest.LiveTv, Kind.Live, "📺", "Watch Live Channels", Color(0xFF8B5CF6), Color(0xFF4C1D95)),
    Tile(Dest.Movies, Kind.Movies, "🎬", "Watch Movies", Color(0xFF3B82F6), Color(0xFF1E3A8A)),
    Tile(Dest.Series, Kind.Series, "🎞", "Watch Series", Color(0xFFF97316), Color(0xFFC2410C)),
    Tile(Dest.Radio, Kind.Radio, "📻", "Listen Radio", Color(0xFF22C55E), Color(0xFF15803D)),
)

@Composable
internal fun Dashboard(
    counts: Map<Kind, Int>,
    recents: List<Channel>,
    favs: Set<String>,
    epgTick: Int,
    wide: Boolean,
    canWatch: Boolean,
    onOpen: (Dest) -> Unit,
    onWatchNow: () -> Unit,
    onPlayRecent: (Int) -> Unit,
    onToggleFav: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        if (!wide) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SCLogo(36.dp)
                Text(
                    "SC Player",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = TextStyle(brush = BrandBrush),
                )
            }
        }

        Hero(wide, canWatch, onWatchNow)

        if (wide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                tiles.forEach { t ->
                    BigTile(t, counts[t.kind] ?: 0, Modifier.weight(1f)) { onOpen(t.dest) }
                }
            }
        } else {
            SectionTitle("Categories")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                tiles.forEach { t ->
                    SmallTile(t, counts[t.kind] ?: 0, Modifier.weight(1f)) { onOpen(t.dest) }
                }
            }
        }

        if (recents.isNotEmpty()) {
            SectionTitle("Continue Watching")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                itemsIndexed(recents) { i, ch ->
                    Box(Modifier.width(150.dp)) {
                        ChannelCard(
                            ch = ch,
                            now = epgNow(ch, epgTick),
                            fav = ch.url in favs,
                            onToggleFav = { onToggleFav(ch.url) },
                        ) { onPlayRecent(i) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(wide: Boolean, canWatch: Boolean, onWatchNow: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(if (wide) 240.dp else 190.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2E0F6B), Color(0xFF6D28D9), Color(0xFF1E3A8A)),
                    start = Offset(0f, 0f),
                    end = Offset(1600f, 900f),
                ),
            )
            .border(1.dp, GlassBorder, shape)
            .drawBehind {
                // زخرفة تجريدية: دوائر مضيئة
                drawCircle(Color.White.copy(alpha = 0.08f), radius = size.height * 0.9f, center = Offset(size.width * 0.85f, size.height * 0.1f))
                drawCircle(Color(0xFF38BDF8).copy(alpha = 0.18f), radius = size.height * 0.6f, center = Offset(size.width * 0.6f, size.height * 1.05f))
                drawCircle(Color(0xFFC084FC).copy(alpha = 0.22f), radius = size.height * 0.35f, center = Offset(size.width * 0.95f, size.height * 0.85f))
            }
            .padding(if (wide) 32.dp else 20.dp),
    ) {
        Column(Modifier.align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Enjoy Unlimited",
                color = Ink,
                fontSize = if (wide) 34.sp else 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
            )
            Text(
                "Entertainment",
                fontSize = if (wide) 34.sp else 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                style = TextStyle(brush = Brush.linearGradient(listOf(Color(0xFFC4B5FD), Color(0xFF7DD3FC)))),
            )
            Text("Your playlists. Your channels.", color = Ink.copy(alpha = 0.75f), fontSize = 13.sp)
            Box(Modifier.height(4.dp))
            GradientButton(if (canWatch) "▶  Watch Now" else "Add a playlist", enabled = canWatch, onClick = onWatchNow)
        }
        if (wide) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    .clickable(enabled = canWatch, role = Role.Button, onClick = onWatchNow),
                contentAlignment = Alignment.Center,
            ) {
                Text("▶", color = Ink, fontSize = 40.sp)
            }
        }
    }
}

@Composable
private fun BigTile(t: Tile, count: Int, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .height(150.dp)
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer { scaleX = if (focused) 1.05f else 1f; scaleY = if (focused) 1.05f else 1f }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(t.from, t.to)))
            .border(if (focused) 3.dp else 1.dp, if (focused) Color.White else GlassBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(t.emoji, fontSize = 40.sp)
            Text(t.kind.title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(t.subtitle, color = Ink.copy(alpha = 0.8f), fontSize = 12.sp)
        }
        Text(
            count.toString(),
            color = Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun SmallTile(t: Tile, count: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(t.from, t.to))),
            contentAlignment = Alignment.Center,
        ) {
            Text(t.emoji, fontSize = 28.sp)
        }
        Text(t.kind.title, color = TextMain, fontSize = 12.sp, maxLines = 1)
        Text("$count", color = TextMuted, fontSize = 11.sp)
    }
}
