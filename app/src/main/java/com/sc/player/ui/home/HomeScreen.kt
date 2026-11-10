package com.sc.player.ui.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.data.Channel
import com.sc.player.data.Kind
import com.sc.player.data.PlaylistRepository
import com.sc.player.data.UserPrefs
import com.sc.player.data.epg.EpgRepository
import com.sc.player.data.kind
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.Ink
import com.sc.player.ui.theme.Panel
import com.sc.player.ui.theme.SCLogo
import com.sc.player.ui.theme.ScreenBackground
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import com.sc.player.ui.update.UpdatePrompt

enum class Dest(val label: String, val glyph: String) {
    Home("Home", "🏠"),
    LiveTv("Live TV", "📺"),
    Movies("Movies", "🎬"),
    Series("Series", "🎞"),
    Radio("Radio", "📻"),
    CatchUp("Catch Up", "⏪"),
    Playlist("Playlist", "📋"),
    Settings("Settings", "⚙️"),
    About("About", "ℹ️"),
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** الشاشة الرئيسية: شريط جانبي (شاشات كبيرة/TV) أو شريط سفلي (موبايل) + المحتوى. */
@Composable
fun HomeScreen(
    playlistId: String,
    onPlay: (List<Channel>, Int) -> Unit,
    onPlaylists: () -> Unit,
    onGuide: (List<Channel>) -> Unit,
) {
    val ctx = LocalContext.current
    val repo = remember { PlaylistRepository(ctx.applicationContext) }
    val prefs = remember { UserPrefs(ctx) }
    val epg = remember { EpgRepository(ctx.applicationContext) }
    var epgTick by remember { mutableIntStateOf(0) }
    val loaded by produceState<List<Channel>?>(null, playlistId) { value = repo.channels(playlistId) }

    // دليل البرامج بيتحمّل في الخلفية، والواجهة بتتحدّث لما يخلص
    LaunchedEffect(playlistId) {
        val p = repo.playlists().firstOrNull { it.id == playlistId } ?: return@LaunchedEffect
        if (epg.load(p)) epgTick++
    }

    ScreenBackground {
        UpdatePrompt()
        val channels = loaded
        if (channels == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading channels...", color = TextMuted, fontSize = 16.sp)
            }
        } else {
            Shell(channels, epgTick, prefs, onPlay, onPlaylists, onGuide)
        }
    }
}

@Composable
private fun Shell(
    channels: List<Channel>,
    epgTick: Int,
    prefs: UserPrefs,
    onPlay: (List<Channel>, Int) -> Unit,
    onPlaylists: () -> Unit,
    onGuide: (List<Channel>) -> Unit,
) {
    val ctx = LocalContext.current
    val wide = LocalConfiguration.current.screenWidthDp >= 720
    var destName by rememberSaveable { mutableStateOf(Dest.Home.name) }
    val dest = Dest.valueOf(destName)
    var favs by remember { mutableStateOf(prefs.favorites()) }
    var recentUrls by remember { mutableStateOf(prefs.recents()) }

    val byKind = remember(channels) { channels.groupBy { it.kind() } }
    val counts = remember(byKind) { Kind.values().associateWith { byKind[it]?.size ?: 0 } }
    val byUrl = remember(channels) { channels.associateBy { it.url } }
    val recents = remember(byUrl, recentUrls) { recentUrls.mapNotNull { byUrl[it] } }

    fun play(list: List<Channel>, i: Int) {
        prefs.addRecent(list[i].url)
        onPlay(list, i)
    }

    fun go(d: Dest) {
        if (d == Dest.Playlist) onPlaylists() else destName = d.name
    }

    BackHandler(enabled = dest != Dest.Home) { destName = Dest.Home.name }

    val content: @Composable () -> Unit = {
        when (dest) {
            Dest.Home -> Dashboard(
                counts = counts,
                recents = recents,
                favs = favs,
                epgTick = epgTick,
                wide = wide,
                canWatch = channels.isNotEmpty(),
                onOpen = { go(it) },
                onWatchNow = {
                    if (recents.isNotEmpty()) play(recents, 0)
                    else (byKind[Kind.Live]?.takeIf { it.isNotEmpty() } ?: channels).let { if (it.isNotEmpty()) play(it, 0) }
                },
                onPlayRecent = { play(recents, it) },
                onToggleFav = { favs = prefs.toggleFavorite(it) },
            )
            Dest.LiveTv, Dest.Movies, Dest.Series, Dest.Radio -> {
                val kind = when (dest) {
                    Dest.Movies -> Kind.Movies
                    Dest.Series -> Kind.Series
                    Dest.Radio -> Kind.Radio
                    else -> Kind.Live
                }
                ContentScreen(
                    kind = kind,
                    items = byKind[kind].orEmpty(),
                    favs = favs,
                    epgTick = epgTick,
                    wide = wide,
                    onToggleFav = { favs = prefs.toggleFavorite(it) },
                    onPlay = { list, i -> play(list, i) },
                    onGuide = onGuide,
                )
            }
            Dest.CatchUp -> ComingSoonScreen("Catch Up", "Replay past programs. Needs provider archive support.")
            Dest.Playlist -> Unit
            Dest.Settings -> SettingsScreen(
                onPlaylists = onPlaylists,
                onAbout = { destName = Dest.About.name },
                onResetUserData = {
                    prefs.clearUserData()
                    favs = prefs.favorites()
                    recentUrls = prefs.recents()
                },
            )
            Dest.About -> AboutScreen()
        }
    }

    if (wide) {
        Row(Modifier.fillMaxSize()) {
            Sidebar(dest, onSelect = { go(it) }, onExit = { ctx.findActivity()?.finish() })
            Box(Modifier.weight(1f).fillMaxHeight()) { content() }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            BottomBar(dest, onSelect = { go(it) })
        }
    }
}

private val sidebarItems = listOf(
    Dest.Home, Dest.LiveTv, Dest.Movies, Dest.Series, Dest.Radio,
    Dest.CatchUp, Dest.Playlist, Dest.Settings, Dest.About,
)

@Composable
private fun Sidebar(current: Dest, onSelect: (Dest) -> Unit, onExit: () -> Unit) {
    Column(
        Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(Panel.copy(alpha = 0.85f))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SCLogo(44.dp)
            Column {
                Text("SC Player", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("Your IPTV Partner", color = TextMuted, fontSize = 11.sp)
            }
        }
        sidebarItems.forEach { d -> SidebarItem(d.glyph, d.label, d == current) { onSelect(d) } }
        Box(Modifier.height(10.dp))
        SidebarItem("⏻", "Exit", false, onClick = onExit)
    }
}

@Composable
private fun SidebarItem(glyph: String, label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .then(if (selected) Modifier.background(BrandBrush) else Modifier)
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else if (selected) Color.Transparent else GlassBorder, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(glyph, fontSize = 18.sp)
        Text(
            label,
            color = if (selected) Ink else TextMain,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private val bottomItems = listOf(Dest.Home, Dest.LiveTv, Dest.Playlist, Dest.Settings)

@Composable
private fun BottomBar(current: Dest, onSelect: (Dest) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Panel).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        bottomItems.forEach { d ->
            // About/Movies/... بتتفتح من الرئيسية والإعدادات، فبنعلّم أقرب تبويب
            val active = d == current || (d == Dest.Settings && current == Dest.About) ||
                (d == Dest.LiveTv && current in listOf(Dest.Movies, Dest.Series, Dest.Radio))
            Column(
                Modifier
                    .weight(1f)
                    .clickable(role = Role.Tab) { onSelect(d) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(d.glyph, fontSize = 20.sp)
                Text(d.label, color = if (active) Cyan else TextMuted, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
