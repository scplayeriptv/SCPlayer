package com.sc.player.ui.home

import androidx.annotation.StringRes
import com.sc.player.R
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
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
import com.sc.player.data.isSeriesEntry
import com.sc.player.data.Kind
import com.sc.player.data.PlaylistRepository
import com.sc.player.data.ParentalSession
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

enum class Dest(@StringRes val labelRes: Int, val glyph: String) {
    Home(R.string.nav_home, "🏠"),
    LiveTv(R.string.nav_live_tv, "📺"),
    Movies(R.string.nav_movies, "🎬"),
    Series(R.string.nav_series, "🎞"),
    Radio(R.string.nav_radio, "📻"),
    Search(R.string.nav_search, "🔍"),
    CatchUp(R.string.nav_catch_up, "⏪"),
    Playlist(R.string.nav_playlist, "📋"),
    Settings(R.string.nav_settings, "⚙️"),
    About(R.string.nav_about, "ℹ️"),
}

internal fun Context.findActivity(): Activity? {
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
    var reloadKey by remember { mutableIntStateOf(0) }
    var reloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val playlistName = remember(playlistId) { repo.playlists().firstOrNull { it.id == playlistId }?.name.orEmpty() }
    val loaded by produceState<List<Channel>?>(null, playlistId, reloadKey) { value = repo.channels(playlistId) }

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
                Text(stringResource(R.string.loading_channels), color = TextMuted, fontSize = 16.sp)
            }
        } else {
            Shell(
                playlistId = playlistId,
                playlistName = playlistName,
                channels = channels,
                epgTick = epgTick,
                prefs = prefs,
                reloading = reloading,
                onReload = {
                    reloading = true
                    scope.launch {
                        repo.refresh(playlistId)
                        reloadKey++
                        reloading = false
                    }
                },
                onPlay = onPlay,
                onPlaylists = onPlaylists,
                onGuide = onGuide,
            )
        }
    }
}

@Composable
private fun Shell(
    playlistId: String,
    playlistName: String,
    channels: List<Channel>,
    epgTick: Int,
    prefs: UserPrefs,
    reloading: Boolean,
    onReload: () -> Unit,
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

    var lockedGroups by remember { mutableStateOf(prefs.lockedGroups()) }
    var unlocked by remember { mutableStateOf(ParentalSession.unlocked) }

    // البروفايلات: مفضلة وسجل منفصلين لكل بروفايل، وبروفايل الأطفال بيفضل مخفي عنه التصنيفات المقفولة
    var profile by remember { mutableStateOf(prefs.activeProfile()) }
    var needPick by remember { mutableStateOf(prefs.profiles().size > 1 && !com.sc.player.data.ProfileSession.chosen) }
    var showSwitcher by remember { mutableStateOf(false) }
    var pinForProfile by remember { mutableStateOf<com.sc.player.data.Profile?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var settingsPinOk by remember { mutableStateOf(false) }
    var settingsPin by remember { mutableStateOf(false) }
    val kids = profile.kids
    fun refreshProfile() {
        profile = prefs.activeProfile()
        favs = prefs.favorites()
        recentUrls = prefs.recents()
        settingsPinOk = false
    }
    fun applyProfile(p: com.sc.player.data.Profile) {
        prefs.setActiveProfile(p.id)
        com.sc.player.data.ProfileSession.chosen = true
        refreshProfile()
        needPick = false
        showSwitcher = false
        if (destName == Dest.Settings.name) destName = Dest.Home.name
    }
    fun pickProfile(p: com.sc.player.data.Profile) {
        // الخروج من بروفايل الأطفال محتاج PIN لو متحدد
        if (p.id != profile.id && profile.kids && prefs.hasPin()) {
            pinError = null
            pinForProfile = p
        } else {
            applyProfile(p)
        }
    }

    val visible = remember(channels, lockedGroups, unlocked, kids) {
        if ((unlocked && !kids) || lockedGroups.isEmpty()) channels else channels.filter { it.group !in lockedGroups }
    }
    val allGroups = remember(channels) { channels.map { it.group }.distinct().sorted() }

    val byKind = remember(visible) { visible.groupBy { it.kind() } }
    val counts = remember(byKind) { Kind.values().associateWith { byKind[it]?.size ?: 0 } }
    val byUrl = remember(visible) { visible.associateBy { it.url } }
    val recents = remember(byUrl, recentUrls) { recentUrls.mapNotNull { byUrl[it] } }

    var openSeries by remember { mutableStateOf<Channel?>(null) }
    var openDetails by remember { mutableStateOf<Pair<List<Channel>, Int>?>(null) }

    fun play(list: List<Channel>, i: Int) {
        // عنصر مسلسل (مش حلقة) → نفتح شاشة المواسم بدل التشغيل
        if (isSeriesEntry(list[i].url)) {
            openSeries = list[i]
            return
        }
        prefs.addRecent(list[i].url)
        onPlay(list, i)
    }

    fun go(d: Dest) {
        openSeries = null
        openDetails = null
        if (d == Dest.Settings && kids && prefs.hasPin() && !settingsPinOk) {
            pinError = null
            settingsPin = true
            return
        }
        if (d == Dest.Playlist) onPlaylists() else destName = d.name
    }

    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    LaunchedEffect(needPick) {
        if (!needPick && !com.sc.player.data.AutoPlaySession.handled) {
            com.sc.player.data.AutoPlaySession.handled = true
            if (prefs.autoPlay() && recents.isNotEmpty()) play(recents, 0)
        }
    }

    BackHandler(enabled = dest != Dest.Home) { destName = Dest.Home.name }
    BackHandler(enabled = openSeries != null) { openSeries = null }
    BackHandler(enabled = openDetails != null) { openDetails = null }

    // من القوائم/البحث: الفيلم يفتح تفاصيله، والباقي يتشغّل (المسلسل بيفتح شاشته من play)
    fun openItem(list: List<Channel>, i: Int) {
        val ch = list[i]
        if (!isSeriesEntry(ch.url) && ch.kind() == Kind.Movies) openDetails = list to i else play(list, i)
    }

    val content: @Composable () -> Unit = content@{
        openDetails?.let { (list, i) ->
            MovieDetailsScreen(
                list = list,
                index = i,
                fav = list[i].url in favs,
                onToggleFav = { favs = prefs.toggleFavorite(list[i].url) },
                onBack = { openDetails = null },
                onPlay = { play(list, i) },
            )
            return@content
        }
        openSeries?.let { entry ->
            SeriesScreen(entry, onBack = { openSeries = null }, onPlay = { l, i -> play(l, i) })
            return@content
        }
        when (dest) {
            Dest.Home -> Launcher(
                playlistName = playlistName,
                counts = counts,
                recents = recents,
                favs = favs,
                epgTick = epgTick,
                wide = wide,
                reloading = reloading,
                version = version,
                onOpen = { go(it) },
                onGuide = { onGuide(byKind[Kind.Live]?.takeIf { it.isNotEmpty() } ?: visible) },
                onReload = onReload,
                onExit = { ctx.findActivity()?.finish() },
                onPlayRecent = { play(recents, it) },
                onToggleFav = { favs = prefs.toggleFavorite(it) },
                profile = profile,
                onProfile = { showSwitcher = true },
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
                    onPlay = { list, i -> openItem(list, i) },
                    onGuide = onGuide,
                )
            }
            Dest.Search -> SearchScreen(
                items = visible,
                favs = favs,
                epgTick = epgTick,
                onToggleFav = { favs = prefs.toggleFavorite(it) },
                onPlay = { list, i -> openItem(list, i) },
            )
            Dest.CatchUp -> ComingSoonScreen(stringResource(R.string.nav_catch_up), stringResource(R.string.catch_up_note))
            Dest.Playlist -> Unit
            Dest.Settings -> SettingsScreen(
                playlistId = playlistId,
                groups = allGroups,
                onParentalChanged = {
                    lockedGroups = prefs.lockedGroups()
                    unlocked = ParentalSession.unlocked
                },
                onPlaylists = onPlaylists,
                onProfileChanged = { refreshProfile() },
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

    androidx.activity.compose.BackHandler(enabled = showSwitcher && !needPick) { showSwitcher = false }

    if (needPick || showSwitcher) {
        ProfilePickerScreen(
            profiles = prefs.profiles(),
            activeId = profile.id,
            onPick = { pickProfile(it) },
            onCancel = if (showSwitcher && !needPick) ({ showSwitcher = false }) else null,
        )
    } else if (dest == Dest.Home) {
        // الرئيسية = مشغّل بمربعات، من غير شريط تنقّل
        Box(Modifier.fillMaxSize()) { content() }
    } else if (wide) {
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

    pinForProfile?.let { target ->
        PinDialog(
            title = stringResource(R.string.profile_pin_leave),
            error = pinError,
            onDismiss = { pinForProfile = null },
        ) { pin ->
            if (prefs.checkPin(pin)) {
                pinForProfile = null
                applyProfile(target)
            } else {
                pinError = ctx.getString(R.string.pin_wrong)
            }
        }
    }
    if (settingsPin) {
        PinDialog(
            title = stringResource(R.string.profile_pin_settings),
            error = pinError,
            onDismiss = { settingsPin = false },
        ) { pin ->
            if (prefs.checkPin(pin)) {
                settingsPin = false
                settingsPinOk = true
                destName = Dest.Settings.name
            } else {
                pinError = ctx.getString(R.string.pin_wrong)
            }
        }
    }
}

private val sidebarItems = listOf(
    Dest.Home, Dest.LiveTv, Dest.Movies, Dest.Series, Dest.Radio, Dest.Search,
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
                Text(stringResource(R.string.tagline), color = TextMuted, fontSize = 11.sp)
            }
        }
        sidebarItems.forEach { d -> SidebarItem(d.glyph, stringResource(d.labelRes), d == current) { onSelect(d) } }
        Box(Modifier.height(10.dp))
        SidebarItem("⏻", stringResource(R.string.nav_exit), false, onClick = onExit)
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
                Text(stringResource(d.labelRes), color = if (active) Cyan else TextMuted, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
