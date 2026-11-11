package com.sc.player.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.R
import com.sc.player.data.Channel
import com.sc.player.data.Kind
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.SCLogo
import com.sc.player.ui.theme.TextMuted

private enum class TileStyle { Hero, Card, Action, Mini }

private val Purple = Color(0xFF8B5CF6) to Color(0xFF4C1D95)
private val Blue = Color(0xFF3B82F6) to Color(0xFF1E3A8A)
private val Orange = Color(0xFFF97316) to Color(0xFFB45309)
private val Green = Color(0xFF22C55E) to Color(0xFF166534)
private val Teal = Color(0xFF14B8A6) to Color(0xFF115E59)
private val Pink = Color(0xFFEC4899) to Color(0xFF9D174D)
private val Slate = Color(0xFF64748B) to Color(0xFF1E293B)
private val Red = Color(0xFFEF4444) to Color(0xFF7F1D1D)

private fun Color.darken(f: Float) = Color(red * f, green * f, blue * f, alpha)

/** الشاشة الرئيسية على شكل مشغّل (Launcher) بمربعات ثلاثية الأبعاد. */
@Composable
internal fun Launcher(
    playlistName: String,
    counts: Map<Kind, Int>,
    recents: List<Channel>,
    favs: Set<String>,
    epgTick: Int,
    wide: Boolean,
    reloading: Boolean,
    version: String,
    onOpen: (Dest) -> Unit,
    onGuide: () -> Unit,
    onReload: () -> Unit,
    onExit: () -> Unit,
    onPlayRecent: (Int) -> Unit,
    onToggleFav: (String) -> Unit,
) {
    val live = stringResource(R.string.nav_live_tv)
    val movies = stringResource(R.string.nav_movies)
    val series = stringResource(R.string.nav_series)
    val radio = stringResource(R.string.nav_radio)
    val guide = stringResource(R.string.guide)
    val playlists = stringResource(R.string.change_playlist)
    val reload = if (reloading) stringResource(R.string.reloading) else stringResource(R.string.tile_reload)
    val settings = stringResource(R.string.nav_settings)
    val exit = stringResource(R.string.nav_exit)
    val status = stringResource(R.string.home_playlist, playlistName)

    if (wide) {
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 14.dp)) {
            Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                Brand()
                Text(
                    status,
                    color = TextMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterEnd).widthIn(max = 260.dp),
                )
            }
            Row(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Tile3D(live, Icons.Filled.LiveTv, Purple, TileStyle.Hero, Modifier.weight(2f).fillMaxHeight(), badge = counts[Kind.Live]?.toString()) {
                    onOpen(Dest.LiveTv)
                }
                Column(Modifier.weight(2.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Tile3D(movies, Icons.Filled.Movie, Blue, TileStyle.Card, Modifier.weight(1f).fillMaxHeight(), badge = counts[Kind.Movies]?.toString()) { onOpen(Dest.Movies) }
                        Tile3D(series, Icons.Filled.VideoLibrary, Orange, TileStyle.Card, Modifier.weight(1f).fillMaxHeight(), badge = counts[Kind.Series]?.toString()) { onOpen(Dest.Series) }
                    }
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Tile3D(radio, Icons.Filled.Radio, Green, TileStyle.Card, Modifier.weight(1f).fillMaxHeight(), badge = counts[Kind.Radio]?.toString()) { onOpen(Dest.Radio) }
                        Tile3D(guide, Icons.Filled.CalendarMonth, Teal, TileStyle.Card, Modifier.weight(1f).fillMaxHeight()) { onGuide() }
                    }
                }
                Column(Modifier.weight(1.7f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val m = Modifier.weight(1f).fillMaxWidth()
                    Tile3D(playlists, Icons.AutoMirrored.Filled.PlaylistPlay, Pink, TileStyle.Action, m) { onOpen(Dest.Playlist) }
                    Tile3D(reload, Icons.Filled.Refresh, Slate, TileStyle.Action, m) { if (!reloading) onReload() }
                    Tile3D(settings, Icons.Filled.Settings, Slate, TileStyle.Action, m) { onOpen(Dest.Settings) }
                    Tile3D(exit, Icons.AutoMirrored.Filled.ExitToApp, Red, TileStyle.Action, m) { onExit() }
                }
            }
            Text(
                "v$version",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End).padding(top = 6.dp),
            )
        }
    } else {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Brand()
                Text(status, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Tile3D(live, Icons.Filled.LiveTv, Purple, TileStyle.Hero, Modifier.fillMaxWidth().height(170.dp), badge = counts[Kind.Live]?.toString()) {
                onOpen(Dest.LiveTv)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Tile3D(movies, Icons.Filled.Movie, Blue, TileStyle.Card, Modifier.weight(1f).height(120.dp), badge = counts[Kind.Movies]?.toString()) { onOpen(Dest.Movies) }
                Tile3D(series, Icons.Filled.VideoLibrary, Orange, TileStyle.Card, Modifier.weight(1f).height(120.dp), badge = counts[Kind.Series]?.toString()) { onOpen(Dest.Series) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Tile3D(radio, Icons.Filled.Radio, Green, TileStyle.Card, Modifier.weight(1f).height(120.dp), badge = counts[Kind.Radio]?.toString()) { onOpen(Dest.Radio) }
                Tile3D(guide, Icons.Filled.CalendarMonth, Teal, TileStyle.Card, Modifier.weight(1f).height(120.dp)) { onGuide() }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val m = Modifier.weight(1f).height(88.dp)
                Tile3D(playlists, Icons.AutoMirrored.Filled.PlaylistPlay, Pink, TileStyle.Mini, m) { onOpen(Dest.Playlist) }
                Tile3D(reload, Icons.Filled.Refresh, Slate, TileStyle.Mini, m) { if (!reloading) onReload() }
                Tile3D(settings, Icons.Filled.Settings, Slate, TileStyle.Mini, m) { onOpen(Dest.Settings) }
                Tile3D(exit, Icons.AutoMirrored.Filled.ExitToApp, Red, TileStyle.Mini, m) { onExit() }
            }
            if (recents.isNotEmpty()) {
                SectionTitle(stringResource(R.string.continue_watching))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(recents) { i, ch ->
                        Box(Modifier.size(width = 140.dp, height = 150.dp)) {
                            ChannelCard(ch, epgNow(ch, epgTick), ch.url in favs, { onToggleFav(ch.url) }) { onPlayRecent(i) }
                        }
                    }
                }
            }
            Text("v$version", color = TextMuted, fontSize = 12.sp, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
private fun Brand() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SCLogo(40.dp)
        Text(
            "SC Player",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            style = TextStyle(brush = BrandBrush),
        )
    }
}

/**
 * مربع بعمق ثلاثي الأبعاد: وجه متدرّج بلمعة علوية وحافة مضيئة، وسُمك سفلي وظل ملوّن.
 * بيميل ناحية اللمس (perspective)، وبيكبر ويتوهّج مع الـ focus (ريموت TV).
 */
@Composable
private fun Tile3D(
    label: String,
    icon: ImageVector,
    colors: Pair<Color, Color>,
    style: TileStyle,
    modifier: Modifier,
    badge: String? = null,
    onClick: () -> Unit,
) {
    val (from, to) = colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var pressed by remember { mutableStateOf(false) }
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    val springSpec = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    val aTiltX by animateFloatAsState(tiltX, animationSpec = springSpec, label = "tiltX")
    val aTiltY by animateFloatAsState(tiltY, animationSpec = springSpec, label = "tiltY")
    val scale by animateFloatAsState(
        when {
            pressed -> 0.97f
            focused -> 1.06f
            else -> 1f
        },
        animationSpec = springSpec,
        label = "scale",
    )
    val density = LocalDensity.current.density
    val radius = if (style == TileStyle.Hero) 28.dp else 22.dp
    val shape = RoundedCornerShape(radius)
    val lip = if (style == TileStyle.Mini) 5.dp else 8.dp

    val medallion = when (style) {
        TileStyle.Hero -> 112.dp
        TileStyle.Card -> 62.dp
        TileStyle.Action -> 44.dp
        TileStyle.Mini -> 40.dp
    }
    val iconSize = medallion * 0.58f
    val labelSize = when (style) {
        TileStyle.Hero -> 24.sp
        TileStyle.Card -> 16.sp
        TileStyle.Action -> 16.sp
        TileStyle.Mini -> 11.sp
    }

    Box(
        modifier
            .graphicsLayer {
                rotationX = aTiltX
                rotationY = aTiltY
                scaleX = scale
                scaleY = scale
                cameraDistance = 16f * density
            }
            .pointerInput(Unit) {
                // بنراقب اللمس من غير ما نستهلكه عشان الضغط يفضل شغّال
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    pressed = true
                    fun tilt(p: Offset) {
                        tiltY = ((p.x / size.width) - 0.5f) * 2f * 9f
                        tiltX = -((p.y / size.height) - 0.5f) * 2f * 9f
                    }
                    tilt(down.position)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.firstOrNull()?.let { tilt(it.position) }
                    } while (event.changes.any { it.pressed })
                    pressed = false
                    tiltX = 0f
                    tiltY = 0f
                }
            }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
    ) {
        // السُمك: طبقة أغمق مزاحة لتحت + ظل ملوّن
        Box(
            Modifier
                .matchParentSize()
                .padding(top = lip)
                .shadow(if (focused) 22.dp else 14.dp, shape, ambientColor = from, spotColor = from)
                .background(to.darken(0.55f), shape),
        )
        // الوجه
        Box(
            Modifier
                .matchParentSize()
                .padding(bottom = lip)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(from, to)))
                .border(
                    if (focused) 3.dp else 1.5.dp,
                    if (focused) Brush.linearGradient(listOf(Color.White, Color.White))
                    else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.04f))),
                    shape,
                ),
        ) {
            // لمعة زجاجية في النص العلوي
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.26f), Color.Transparent))),
            )
            val content: @Composable () -> Unit = {
                Box(
                    Modifier
                        .size(medallion)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.08f))))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
                }
                Text(
                    label,
                    color = Color.White,
                    fontSize = labelSize,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 3f), 6f)),
                )
            }
            if (style == TileStyle.Action) {
                Row(
                    Modifier.align(Alignment.CenterStart).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) { content() }
            } else {
                Column(
                    Modifier.align(Alignment.Center).padding(horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (style == TileStyle.Hero) 10.dp else 6.dp),
                ) { content() }
            }
            if (badge != null) {
                Text(
                    badge,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.32f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}
