package com.sc.player.ui.home

import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.focus.onFocusChanged
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
import com.sc.player.data.Profile
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.SCLogo
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.AppTheme
import com.sc.player.ui.theme.ThemeState
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
    playlistId: String,
    playlistName: String,
    counts: Map<Kind, Int>,
    recents: List<Channel>,
    quick: List<Channel>,
    favs: Set<String>,
    epgTick: Int,
    wide: Boolean,
    reloading: Boolean,
    version: String,
    onOpen: (Dest) -> Unit,
    onGuide: () -> Unit,
    onMulti: () -> Unit,
    onReload: () -> Unit,
    onExit: () -> Unit,
    onPlayRecent: (Int) -> Unit,
    onPlayQuick: (Int) -> Unit,
    onToggleFav: (String) -> Unit,
    profile: Profile,
    onProfile: () -> Unit,
) {
    val live = stringResource(R.string.nav_live_tv)
    val movies = stringResource(R.string.nav_movies)
    val series = stringResource(R.string.nav_series)
    val radio = stringResource(R.string.nav_radio)
    val guide = stringResource(R.string.guide)
    val multi = stringResource(R.string.multi_view)
    val search = stringResource(R.string.nav_search)
    val playlists = stringResource(R.string.change_playlist)
    val reload = if (reloading) stringResource(R.string.reloading) else stringResource(R.string.tile_reload)
    val settings = stringResource(R.string.nav_settings)
    val exit = stringResource(R.string.nav_exit)
    val status = stringResource(R.string.home_playlist, playlistName)

    if (wide) {
        val crimson = ThemeState.current == AppTheme.Crimson
        val cLive = if (crimson) Color(0xFFE11D2E) to Color(0xFF2A0508) else Purple
        val cMovies = if (crimson) Color(0xFFB91C1C) to Color(0xFF1C0306) else Blue
        val cSeries = if (crimson) Color(0xFF9F1239) to Color(0xFF1C0306) else Orange
        val cMini = if (crimson) Color(0xFFB91C1C) to Color(0xFF140406) else Slate
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 14.dp)) {
            Row(
                Modifier.fillMaxWidth().height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Brand()
                TopPill(playlistId, status, profile, onProfile, Modifier.weight(1f, fill = false).padding(horizontal = 12.dp))
                ClockBlock(big = true)
            }
            Row(
                Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                HubPanel(live, Icons.Filled.LiveTv, cLive, counts[Kind.Live]?.toString(), "LIVE", Modifier.weight(1f).fillMaxHeight()) { onOpen(Dest.LiveTv) }
                Column(Modifier.weight(1.7f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(Modifier.weight(1.3f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        HubPanel(movies, Icons.Filled.Movie, cMovies, counts[Kind.Movies]?.toString(), null, Modifier.weight(1f).fillMaxHeight()) { onOpen(Dest.Movies) }
                        HubPanel(series, Icons.Filled.VideoLibrary, cSeries, counts[Kind.Series]?.toString(), null, Modifier.weight(1f).fillMaxHeight()) { onOpen(Dest.Series) }
                    }
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        val m = Modifier.weight(1f).fillMaxHeight()
                        Tile3D(settings, Icons.Filled.Settings, cMini, TileStyle.Mini, m) { onOpen(Dest.Settings) }
                        Tile3D(playlists, Icons.Filled.VideoLibrary, cMini, TileStyle.Mini, m) { onOpen(Dest.Playlist) }
                        Tile3D(reload, Icons.Filled.Refresh, cMini, TileStyle.Mini, m) { if (!reloading) onReload() }
                        Tile3D(exit, Icons.AutoMirrored.Filled.ExitToApp, Red, TileStyle.Mini, m) { onExit() }
                    }
                }
            }
        }
    } else {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Brand()
                Text(status, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                ProfileChip(profile, onProfile, Modifier.padding(top = 4.dp))
            }
            ClockBlock()
            Tile3D(live, Icons.Filled.LiveTv, Purple, TileStyle.Hero, Modifier.fillMaxWidth().height(150.dp), badge = counts[Kind.Live]?.toString()) {
                onOpen(Dest.LiveTv)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Tile3D(movies, Icons.Filled.Movie, Blue, TileStyle.Card, Modifier.weight(1f).height(112.dp), badge = counts[Kind.Movies]?.toString()) { onOpen(Dest.Movies) }
                Tile3D(series, Icons.Filled.VideoLibrary, Orange, TileStyle.Card, Modifier.weight(1f).height(112.dp), badge = counts[Kind.Series]?.toString()) { onOpen(Dest.Series) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val m = Modifier.weight(1f).height(92.dp)
                Tile3D(reload, Icons.Filled.Refresh, Slate, TileStyle.Mini, m) { if (!reloading) onReload() }
                Tile3D(settings, Icons.Filled.Settings, Slate, TileStyle.Mini, m) { onOpen(Dest.Settings) }
                Tile3D(exit, Icons.AutoMirrored.Filled.ExitToApp, Red, TileStyle.Mini, m) { onExit() }
            }
            PhysicalLeft { ExpiryPill(playlistId) }
        }
    }
    if (reloading) {
        // شاشة تحميل واضحة وقت إعادة تحميل القائمة (وبتمنع اللمس لحد ما يخلص)
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xAA000000))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.CircularProgressIndicator(color = com.sc.player.ui.theme.Cyan)
                Text(reload, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** ساعة كبيرة وتاريخ اليوم (بتتحدّث كل 10 ثواني). */
@Composable
private fun ClockBlock(modifier: Modifier = Modifier, big: Boolean = false) {
    val locale = LocalConfiguration.current.locales[0]
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(10_000)
            value = System.currentTimeMillis()
        }
    }
    val time = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, locale).format(java.util.Date(now))
    val date = java.text.SimpleDateFormat("EEE, d MMM yyyy", locale).format(java.util.Date(now))
    if (big) {
        Column(modifier, horizontalAlignment = Alignment.End) {
            Text(time, color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(date, color = TextMuted, fontSize = 12.sp)
        }
        return
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Filled.Schedule, null, tint = com.sc.player.ui.theme.Cyan, modifier = Modifier.size(26.dp))
        Column {
            Text(time, color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(date, color = TextMuted, fontSize = 11.sp)
        }
    }
}

/** كبسولة انتهاء الاشتراك (لو القائمة Xtream) بلون حسب الأيام المتبقية. */
@Composable
private fun ExpiryPill(playlistId: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    val ctx = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val expiry by produceState<Pair<Boolean, Long?>>(false to null, playlistId) {
        val url = com.sc.player.data.PlaylistRepository(ctx.applicationContext).playlists()
            .firstOrNull { it.id == playlistId }?.url.orEmpty()
        com.sc.player.data.XtreamClient(ctx.applicationContext).accountInfo(url).getOrNull()?.let { value = true to it.expires }
    }
    val (loaded, exp) = expiry
    if (!loaded) return
    val now = remember { System.currentTimeMillis() }
    val ms = (exp ?: 0L) * 1000
    val days = if (exp == null) Int.MAX_VALUE else Math.floorDiv(ms - now, 86_400_000L).toInt()
    val color = when {
        days <= 7 -> Color(0xFFF87171)
        days <= 30 -> Color(0xFFFBBF24)
        else -> Color(0xFF34D399)
    }
    val dateTxt = if (exp == null) stringResource(R.string.acc_unlimited)
    else java.text.SimpleDateFormat("d MMM yyyy", locale).format(java.util.Date(ms))
    val sub = when {
        exp == null -> null
        days < 0 -> stringResource(R.string.acc_expired)
        else -> stringResource(R.string.acc_days_left, days)
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        if (compact) modifier else modifier
            .clip(shape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.5f), shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.CalendarMonth, null, tint = color, modifier = Modifier.size(22.dp))
        Column {
            Text(stringResource(R.string.acc_expires), color = TextMuted, fontSize = 10.sp)
            Text(dateTxt, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        if (sub != null) Text(sub, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (compact) VDivider()
    }
}

@Composable
private fun VDivider() {
    Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.2f)))
}

/** كبسولة علوية زجاجية: انتهاء الاشتراك | اسم القائمة | البروفايل. */
@Composable
private fun TopPill(playlistId: String, status: String, profile: Profile, onProfile: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ExpiryPill(playlistId, compact = true)
        Text(status, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 150.dp))
        VDivider()
        ProfileChip(profile, onProfile, Modifier.widthIn(max = 170.dp))
    }
}

/** الجانب الفعلي (يسار الشاشة) بغض النظر عن اتجاه اللغة. */
@Composable
private fun PhysicalLeft(content: @Composable () -> Unit) {
    val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    Box(Modifier.fillMaxWidth(), contentAlignment = if (rtl) Alignment.CenterEnd else Alignment.CenterStart) { content() }
}

@Composable
private fun DockButton(icon: ImageVector, desc: String, tint: Color, spinning: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.15f else 1f, label = "dock")
    val spin = androidx.compose.animation.core.rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(
        0f, 360f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.LinearEasing)),
        label = "angle",
    )
    Box(
        Modifier
            .size(44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.30f), tint.copy(alpha = 0.10f))))
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else tint.copy(alpha = 0.55f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = desc,
            tint = Color.White,
            modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = if (spinning) angle else 0f },
        )
    }
}

/** لوحة سينمائية: خلفية متحركة بكرات ضوء، أيقونة عملاقة شفافة، عدّاد، وميل 3D مع اللمس. */
@Composable
private fun HubPanel(
    label: String,
    icon: ImageVector,
    colors: Pair<Color, Color>,
    count: String?,
    tag: String?,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val (from, to) = colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var pressed by remember { mutableStateOf(false) }
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    val springSpec = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    val aTiltX by animateFloatAsState(tiltX, animationSpec = springSpec, label = "tx")
    val aTiltY by animateFloatAsState(tiltY, animationSpec = springSpec, label = "ty")
    val scale by animateFloatAsState(if (pressed) 0.97f else if (focused) 1.05f else 1f, animationSpec = springSpec, label = "sc")
    val bar by androidx.compose.animation.core.animateDpAsState(if (focused) 96.dp else 36.dp, label = "bar")
    val density = LocalDensity.current.density
    val shape = RoundedCornerShape(32.dp)

    val inf = androidx.compose.animation.core.rememberInfiniteTransition(label = "orbs")
    val t by inf.animateFloat(
        0f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(7000, easing = androidx.compose.animation.core.LinearEasing),
            androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "t",
    )
    val pulse by inf.animateFloat(
        0.55f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(900),
            androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier
            .graphicsLayer {
                rotationX = aTiltX
                rotationY = aTiltY
                scaleX = scale
                scaleY = scale
                cameraDistance = 16f * density
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    pressed = true
                    fun tilt(p: Offset) {
                        tiltY = ((p.x / size.width) - 0.5f) * 2f * 7f
                        tiltX = -((p.y / size.height) - 0.5f) * 2f * 7f
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
            .shadow(if (focused) 28.dp else 16.dp, shape, ambientColor = from, spotColor = from)
            .clip(shape)
            .background(Brush.linearGradient(listOf(from.darken(0.50f), to.darken(0.28f), Color(0xFF070B18))))
            .drawBehind {
                val w = size.width
                val h = size.height
                val c1 = Offset(w * (0.15f + 0.55f * t), h * (0.20f + 0.25f * t))
                drawCircle(
                    Brush.radialGradient(listOf(from.copy(alpha = 0.60f), Color.Transparent), center = c1, radius = size.minDimension * 0.95f),
                    radius = size.minDimension * 0.95f,
                    center = c1,
                )
                val c2 = Offset(w * (0.95f - 0.5f * t), h * (0.95f - 0.35f * t))
                drawCircle(
                    Brush.radialGradient(listOf(Color.White.copy(alpha = 0.14f), Color.Transparent), center = c2, radius = size.minDimension * 0.8f),
                    radius = size.minDimension * 0.8f,
                    center = c2,
                )
            }
            .border(
                if (focused) 3.dp else 1.5.dp,
                if (focused) Brush.linearGradient(listOf(Color.White, from)) else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.03f))),
                shape,
            )
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
    ) {
        val compact = maxHeight < 230.dp
        val med = if (compact) 58.dp else 76.dp
        // أيقونة عملاقة شفافة كعلامة مائية
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.09f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(maxHeight * 0.95f)
                .offset(x = maxHeight * 0.18f, y = maxHeight * 0.12f)
                .rotate(-14f),
        )
        // لمعة زجاجية علوية
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.38f)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.Transparent))),
        )
        // الميدالية المضيئة
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(18.dp)
                .size(med)
                .shadow(18.dp, CircleShape, ambientColor = from, spotColor = from)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(from, to)))
                .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(med * 0.52f))
        }
        // العدّاد + وسم LIVE
        Column(Modifier.align(Alignment.TopEnd).padding(16.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (count != null) {
                Text(
                    count,
                    color = Color.White,
                    fontSize = if (compact) 20.sp else 26.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(shadow = Shadow(from, Offset(0f, 0f), 18f)),
                )
            }
            if (tag != null) {
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFEF4444).copy(alpha = 0.9f)).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(Modifier.size(7.dp).graphicsLayer { alpha = pulse }.clip(CircleShape).background(Color.White))
                    Text(tag, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }
        }
        // العنوان + شريط مضيء
        Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 20.dp, end = 64.dp)) {
            Text(
                label,
                color = Color.White,
                fontSize = if (compact) 22.sp else 28.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 4f), 10f)),
            )
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .size(width = bar, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.horizontalGradient(listOf(from, Color.White))),
            )
        }
        // سهم زجاجي
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (focused) 0.30f else 0.14f))
                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("›", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
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
        TileStyle.Action -> 38.dp
        TileStyle.Mini -> 40.dp
    }
    val iconSize = medallion * 0.58f
    val labelSize = when (style) {
        TileStyle.Hero -> 24.sp
        TileStyle.Card -> 16.sp
        TileStyle.Action -> 15.sp
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
                    maxLines = if (style == TileStyle.Mini) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = if (style == TileStyle.Mini) 13.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                    style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 3f), 6f)),
                )
            }
            if (style == TileStyle.Action) {
                Row(
                    Modifier.align(Alignment.CenterStart).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
