package com.sc.player.ui.home

import android.content.Context
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.Dp
import com.sc.player.ui.theme.Blue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.Ink
import com.sc.player.ui.theme.Violet
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import com.sc.player.data.epg.EpgRepository
import com.sc.player.data.PlaylistRepository
import com.sc.player.data.ParentalSession
import com.sc.player.data.UserPrefs
import com.sc.player.data.AccountInfo
import com.sc.player.data.NotXtreamPanelException
import com.sc.player.data.XtreamClient
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import com.sc.player.data.appLanguages
import com.sc.player.data.AppLanguage
import com.sc.player.data.LocaleHelper
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import com.sc.player.R
import androidx.compose.ui.res.stringResource
import com.sc.player.data.update.UpdateInfo
import com.sc.player.data.update.Updater
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import com.sc.player.ui.update.UpdateDialog
import com.sc.player.data.activityOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private fun cacheBytes(ctx: Context): Long {
    val images = ctx.imageLoader.diskCache?.size ?: 0L
    val updates = File(ctx.cacheDir, "updates").walkTopDown().filter { it.isFile }.sumOf { it.length() }
    return images + updates
}

private fun mb(bytes: Long): String = "%.1f MB".format(bytes / 1048576.0)

@Composable
fun SettingsScreen(
    playlistId: String,
    groups: List<String>,
    checkable: List<com.sc.player.data.Channel>,
    onParentalChanged: () -> Unit,
    onProfileChanged: () -> Unit,
    onCleanupChanged: () -> Unit,
    onPlaylists: () -> Unit,
    onTool: (String) -> Unit,
    onAbout: () -> Unit,
    onResetUserData: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    ctx.contentResolver.openOutputStream(uri, "w")!!.use { it.write(com.sc.player.data.BackupManager.export(ctx).toByteArray()) }
                }.isSuccess
            }
            android.widget.Toast.makeText(ctx, if (ok) R.string.backup_done else R.string.backup_failed, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val restoreLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) } }
                    .mapCatching { com.sc.player.data.BackupManager.import(ctx, it).getOrThrow() }
            }
            if (res.isSuccess) {
                com.sc.player.ui.theme.ThemeState.current = com.sc.player.ui.theme.AppTheme.byId(com.sc.player.data.UserPrefs(ctx).theme())
                android.widget.Toast.makeText(ctx, R.string.restore_done, android.widget.Toast.LENGTH_LONG).show()
                ctx.activityOrNull()?.recreate()
            } else {
                android.widget.Toast.makeText(ctx, R.string.restore_failed, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    val updater = remember { Updater(ctx) }
    var cache by remember { mutableLongStateOf(0L) }
    var cacheLoaded by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var checking by remember { mutableStateOf(false) }
    var resetDone by remember { mutableStateOf(false) }
    var savedLang by remember { mutableStateOf(LocaleHelper.saved(ctx)) }
    var showLanguages by remember { mutableStateOf(false) }
    val prefs = remember { UserPrefs(ctx) }
    var showParental by remember { mutableStateOf(false) }
    var parentalTick by remember { mutableStateOf(0) }
    var showEpg by remember { mutableStateOf(false) }
    var showDiag by remember { mutableStateOf(false) }
    var dedupe by remember { mutableStateOf(prefs.dedupe()) }
    var hiddenSet by remember { mutableStateOf(prefs.hiddenGroups()) }
    var showHide by remember { mutableStateOf(false) }
    var showCheck by remember { mutableStateOf(false) }
    var hideDead by remember { mutableStateOf(prefs.hideDead()) }
    var deadCount by remember { mutableStateOf(prefs.deadUrls().size) }
    var epgUrl by remember { mutableStateOf(prefs.epgOverride(playlistId).orEmpty()) }

    LaunchedEffect(Unit) {
        cache = withContext(Dispatchers.IO) { cacheBytes(ctx) }
        cacheLoaded = true
    }

    var sectionName by rememberSaveable { mutableStateOf(Section.General.name) }
    var showSection by rememberSaveable { mutableStateOf(false) }
    var showPro by remember { mutableStateOf(false) }
    val section = Section.valueOf(sectionName)
    var autoPlay by remember { mutableStateOf(prefs.autoPlay()) }
    var hw by remember { mutableStateOf(prefs.hardwareDecoding()) }
    var bg by remember { mutableStateOf(prefs.backgroundPlay()) }
    var pip by remember { mutableStateOf(prefs.pip()) }
    var reconnect by remember { mutableStateOf(prefs.autoReconnect()) }
    var bufferMode by remember { mutableStateOf(prefs.bufferMode()) }
    var stats by remember { mutableStateOf(prefs.showStats()) }
    var introSkip by remember { mutableStateOf(prefs.introSkip()) }
    var engine by remember { mutableStateOf(prefs.playerEngine()) }
    var profilesTick by remember { mutableStateOf(0) }
    var editingProfile by remember { mutableStateOf<com.sc.player.data.Profile?>(null) }
    var addingProfile by remember { mutableStateOf(false) }
    // بيانات الحساب: null = بيتحمّل، Result = جاهز
    var account by remember { mutableStateOf<Result<AccountInfo>?>(null) }
    LaunchedEffect(section == Section.Account, playlistId) {
        if (section == Section.Account && account == null) {
            val url = PlaylistRepository(ctx.applicationContext).playlists().firstOrNull { it.id == playlistId }?.url.orEmpty()
            account = XtreamClient(ctx.applicationContext).accountInfo(url)
        }
    }
    val cfgS = LocalConfiguration.current
    val wide = cfgS.screenWidthDp >= 900 && cfgS.screenHeightDp >= 480
    val compactH = cfgS.screenHeightDp < 480

    val rows: @Composable () -> Unit = {
        val ac = sectionAccent(section)
        when (section) {
            Section.General -> {
                GroupLabel(stringResource(R.string.set_grp_appearance))
                SettingsCard {
                    SettingRow(
                        stringResource(R.string.set_language),
                        null,
                        value = appLanguages.firstOrNull { it.code == savedLang }?.nativeName ?: stringResource(R.string.set_language_system),
                        lead = Icons.Filled.Language,
                        accent = ac,
                    ) { showLanguages = true }
                    RowDivider()
                    ThemePicker(prefs)
                }
            }
            Section.Playback -> {
                GroupLabel(stringResource(R.string.set_grp_playback))
                SettingsCard {
                    ToggleRow(stringResource(R.string.set_auto_play), stringResource(R.string.set_auto_play_sub), autoPlay, Icons.Filled.PlayCircle, ac) {
                        autoPlay = it
                        prefs.setAutoPlay(it)
                    }
                    RowDivider()
                    ToggleRow(stringResource(R.string.set_bg_play), stringResource(R.string.set_bg_play_sub), bg, Icons.Filled.Headphones, ac) {
                        bg = it
                        prefs.setBackgroundPlay(it)
                    }
                    RowDivider()
                    ToggleRow(stringResource(R.string.set_pip), stringResource(R.string.set_pip_sub), pip, Icons.Filled.PictureInPicture, ac) {
                        pip = it
                        prefs.setPip(it)
                    }
                    RowDivider()
                    SettingRow(
                        stringResource(R.string.set_intro),
                        stringResource(R.string.set_intro_sub),
                        value = if (introSkip == 0) stringResource(R.string.set_intro_off) else stringResource(R.string.set_intro_sec, introSkip),
                        lead = Icons.Filled.SkipNext,
                        accent = ac,
                    ) {
                        val opts = listOf(0, 60, 85, 90, 120)
                        introSkip = opts[(opts.indexOf(introSkip).let { if (it < 0) 0 else it } + 1) % opts.size]
                        prefs.setIntroSkip(introSkip)
                    }
                }
                SettingsCard {
                    SettingRow(
                        stringResource(R.string.set_player_engine),
                        stringResource(R.string.set_player_engine_sub),
                        value = stringResource(if (engine == 1) R.string.engine_mpv else R.string.engine_exo),
                        lead = Icons.Filled.PlayCircle,
                        accent = ac,
                    ) {
                        engine = if (engine == 1) 0 else 1
                        prefs.setPlayerEngine(engine)
                    }
                }
                GroupLabel(stringResource(R.string.set_grp_performance))
                SettingsCard {
                    ToggleRow(stringResource(R.string.set_hw), stringResource(R.string.set_hw_sub), hw, Icons.Filled.Memory, ac) {
                        hw = it
                        prefs.setHardwareDecoding(it)
                    }
                    RowDivider()
                    ToggleRow(stringResource(R.string.set_reconnect), stringResource(R.string.set_reconnect_sub), reconnect, Icons.Filled.Sync, ac) {
                        reconnect = it
                        prefs.setAutoReconnect(it)
                    }
                    RowDivider()
                    SettingRow(
                        stringResource(R.string.set_buffer),
                        stringResource(R.string.set_buffer_sub),
                        value = stringResource(
                            when (bufferMode) {
                                0 -> R.string.buffer_low
                                2 -> R.string.buffer_high
                                else -> R.string.buffer_normal
                            },
                        ),
                        lead = Icons.Filled.Speed,
                        accent = ac,
                    ) {
                        bufferMode = (bufferMode + 1) % 3
                        prefs.setBufferMode(bufferMode)
                    }
                }
                GroupLabel(stringResource(R.string.set_grp_diag))
                SettingsCard {
                    ToggleRow(stringResource(R.string.set_stats), stringResource(R.string.set_stats_sub), stats, Icons.Filled.Equalizer, ac) {
                        stats = it
                        prefs.setShowStats(it)
                    }
                }
            }
            Section.Content -> {
                SettingsCard {
                    SettingRow(
                        stringResource(R.string.nav_playlist),
                        null,
                        value = stringResource(R.string.set_playlists_sub),
                        lead = Icons.Filled.VideoLibrary,
                        accent = ac,
                        onClick = onPlaylists,
                    )
                }
                Spacer(Modifier.height(12.dp))
                SettingsCard {
                    SettingRow(
                        stringResource(R.string.epg_title),
                        null,
                        value = if (epgUrl.isBlank()) stringResource(R.string.epg_auto) else epgUrl,
                        lead = Icons.Filled.DateRange,
                        accent = ac,
                    ) { showEpg = true }
                }
                Spacer(Modifier.height(12.dp))
                SettingsCard {
                    SettingRow(stringResource(R.string.diag_title), stringResource(R.string.diag_sub), value = null, lead = Icons.Filled.Build, accent = ac) { showDiag = true }
                }
                Spacer(Modifier.height(12.dp))
                GroupLabel(stringResource(R.string.set_sec_cleanup))
                SettingsCard {
                    ToggleRow(stringResource(R.string.set_dedupe), stringResource(R.string.set_dedupe_sub), dedupe, Icons.Filled.ContentCopy, ac) {
                        dedupe = it
                        prefs.setDedupe(it)
                        onCleanupChanged()
                    }
                    RowDivider()
                    SettingRow(
                        stringResource(R.string.set_hide_groups),
                        stringResource(R.string.set_hide_groups_sub),
                        stringResource(R.string.set_hidden_count, hiddenSet.size),
                        lead = Icons.Filled.VisibilityOff,
                        accent = ac,
                    ) { showHide = true }
                    RowDivider()
                    SettingRow(
                        stringResource(R.string.set_check),
                        stringResource(R.string.set_check_sub),
                        stringResource(R.string.set_dead_count, deadCount),
                        lead = Icons.Filled.Build,
                        accent = ac,
                    ) { showCheck = true }
                    RowDivider()
                    ToggleRow(stringResource(R.string.set_hide_dead), stringResource(R.string.set_hide_dead_sub), hideDead, Icons.Filled.Block, ac) {
                        hideDead = it
                        prefs.setHideDead(it)
                        onCleanupChanged()
                    }
                }
            }
            Section.Profiles -> {
                SettingsCard {
                    val all = profilesTick.let { prefs.profiles() }
                    val activeId = profilesTick.let { prefs.activeProfile().id }
                    all.forEach { p ->
                        SettingRow(
                            "${p.avatarEmoji()}  ${p.displayName()}",
                            if (p.kids) stringResource(R.string.profile_kids_badge) else null,
                            value = if (p.id == activeId) stringResource(R.string.profile_active) else null,
                            lead = Icons.Filled.Person,
                            accent = ac,
                        ) { editingProfile = p }
                        RowDivider()
                    }
                    if (all.size < com.sc.player.data.UserPrefs.MAX_PROFILES) {
                        SettingRow(stringResource(R.string.profile_add), null, value = null, lead = Icons.Filled.PersonAdd, accent = ac) { addingProfile = true }
                    } else {
                        Spacer(Modifier.height(2.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                SettingsCard {
                    SettingRow(
                        stringResource(R.string.parental_title),
                        null,
                        // parentalTick بس عشان السطر يتحدّث بعد أي تغيير
                        value = parentalTick.let {
                            val n = prefs.lockedGroups().size
                            when {
                                !prefs.hasPin() -> stringResource(R.string.parental_off)
                                ParentalSession.unlocked -> stringResource(R.string.parental_unlocked)
                                else -> stringResource(R.string.parental_on, n)
                            }
                        },
                        lead = Icons.Filled.Lock,
                        accent = ac,
                    ) { showParental = true }
                }
            }
            Section.Account -> SettingsCard {
                val acc = account
                when {
                    acc == null -> InfoRow(stringResource(R.string.set_sec_account), stringResource(R.string.acc_loading))
                    acc?.isFailure == true -> InfoRow(
                        stringResource(R.string.set_sec_account),
                        if (acc?.exceptionOrNull() is NotXtreamPanelException) stringResource(R.string.acc_not_xtream)
                        else stringResource(R.string.acc_failed),
                    )
                    else -> {
                        val a = acc!!.getOrThrow()
                        InfoRow(stringResource(R.string.acc_status), a.status.ifBlank { "—" })
                        RowDivider()
                        val exp = a.expires
                        InfoRow(
                            stringResource(R.string.acc_expires),
                            if (exp == null) stringResource(R.string.acc_unlimited) else {
                                val ms = exp * 1000
                                val date = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(ms))
                                val days = ((ms - System.currentTimeMillis()) / 86_400_000L).toInt()
                                if (days < 0) "$date — " + stringResource(R.string.acc_expired)
                                else "$date — " + stringResource(R.string.acc_days_left, days)
                            },
                        )
                        RowDivider()
                        InfoRow(
                            stringResource(R.string.acc_connections),
                            "${a.activeConnections ?: "—"} / ${a.maxConnections ?: "—"}",
                        )
                        RowDivider()
                        InfoRow(
                            stringResource(R.string.acc_type),
                            stringResource(if (a.trial) R.string.acc_trial else R.string.acc_full),
                        )
                    }
                }
            }
            Section.Data -> {
                GroupLabel(stringResource(R.string.set_grp_maintenance))
                SettingsCard {
                    SettingRow(
                        stringResource(R.string.set_clear_cache),
                        null,
                        value = if (cacheLoaded) mb(cache) else stringResource(R.string.calculating),
                        icon = Icons.Filled.Delete,
                        lead = Icons.Filled.Storage,
                        accent = ac,
                    ) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                ctx.imageLoader.diskCache?.clear()
                                File(ctx.cacheDir, "updates").deleteRecursively()
                            }
                            ctx.imageLoader.memoryCache?.clear()
                            cache = withContext(Dispatchers.IO) { cacheBytes(ctx) }
                        }
                    }
                    RowDivider()
                    SettingRow(stringResource(R.string.set_backup), stringResource(R.string.set_backup_sub), null, lead = Icons.Filled.Backup, accent = ac) {
                        backupLauncher.launch("scplayer-backup.json")
                    }
                    RowDivider()
                    SettingRow(stringResource(R.string.set_backup_restore), stringResource(R.string.set_backup_restore_sub), null, lead = Icons.Filled.Restore, accent = ac) {
                        restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                    }
                }
                GroupLabel(stringResource(R.string.set_grp_reset))
                SettingsCard {
                    SettingRow(
                        stringResource(R.string.set_reset),
                        null,
                        value = if (resetDone) stringResource(R.string.done) else stringResource(R.string.set_reset_sub),
                        lead = Icons.Filled.DeleteForever,
                        accent = ac,
                    ) {
                        com.sc.player.data.PositionStore(ctx).clear()
                        onResetUserData()
                        resetDone = true
                    }
                    RowDivider()
                    SettingRow(stringResource(R.string.set_restore), null, value = null, lead = Icons.Filled.RestartAlt, accent = ac) {
                        prefs.restoreDefaults()
                        autoPlay = prefs.autoPlay()
                        hw = prefs.hardwareDecoding()
                        bg = prefs.backgroundPlay()
                        pip = prefs.pip()
                        reconnect = prefs.autoReconnect()
                        bufferMode = prefs.bufferMode()
                        stats = prefs.showStats()
                        introSkip = prefs.introSkip()
                engine = prefs.playerEngine()
                    }
                }
            }
            Section.About -> SettingsCard {
                SettingRow(
                    stringResource(R.string.set_check_updates),
                    null,
                    value = if (checking) stringResource(R.string.checking)
                    else updateStatus ?: stringResource(R.string.installed_build, updater.currentVersionCode().toInt()),
                    icon = Icons.Filled.Refresh,
                    lead = Icons.Filled.Sync,
                    accent = ac,
                ) {
                    if (!checking) {
                        checking = true
                        scope.launch {
                            val info = updater.check()
                            checking = false
                            if (info == null) {
                                updateStatus = ctx.getString(R.string.up_to_date, updater.currentVersionCode().toInt())
                            } else {
                                updateStatus = ctx.getString(R.string.update_available_build, info.versionCode.toInt())
                                update = info
                            }
                        }
                    }
                }
                RowDivider()
                SettingRow(
                    stringResource(R.string.nav_about),
                    null,
                    value = stringResource(R.string.set_about_sub),
                    lead = Icons.Filled.Info,
                    accent = ac,
                    onClick = onAbout,
                )
            }
        }
    }

    if (cfgS.screenWidthDp >= 600) {
        val installedTxt = stringResource(R.string.installed_build, updater.currentVersionCode().toInt())
        val parentalValue = parentalTick.let {
            val n = prefs.lockedGroups().size
            when {
                !prefs.hasPin() -> stringResource(R.string.parental_off)
                ParentalSession.unlocked -> stringResource(R.string.parental_unlocked)
                else -> stringResource(R.string.parental_on, n)
            }
        }
        val tiles = listOf(
            TileData(Icons.Filled.WorkspacePremium, "Pro", stringResource(if (com.sc.player.data.Premium.active) R.string.pro_status_on else R.string.pro_status_off), com.sc.player.data.Premium.active) {
                showPro = true
            },
            TileData(Icons.Filled.Palette, stringResource(Section.General.titleRes), appLanguages.firstOrNull { it.code == savedLang }?.nativeName ?: stringResource(R.string.set_language_system), null) {
                sectionName = Section.General.name; showSection = true
            },
            TileData(Icons.Filled.PlayArrow, stringResource(Section.Playback.titleRes), stringResource(if (engine == 1) R.string.engine_mpv else R.string.engine_exo), null) {
                sectionName = Section.Playback.name; showSection = true
            },
            TileData(Icons.Filled.VideoLibrary, stringResource(Section.Content.titleRes), stringResource(R.string.set_dead_count, deadCount), null) {
                sectionName = Section.Content.name; showSection = true
            },
            TileData(Icons.Filled.Person, stringResource(Section.Profiles.titleRes), parentalValue, null) {
                sectionName = Section.Profiles.name; showSection = true
            },
            TileData(Icons.Filled.AccountCircle, stringResource(Section.Account.titleRes), null, null) {
                sectionName = Section.Account.name; showSection = true
            },
            TileData(Icons.Filled.Storage, stringResource(Section.Data.titleRes), if (cacheLoaded) mb(cache) else stringResource(R.string.calculating), null) {
                sectionName = Section.Data.name; showSection = true
            },
            TileData(Icons.Filled.Info, stringResource(R.string.nav_about), installedTxt, null) {
                sectionName = Section.About.name; showSection = true
            },
        )
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = if (compactH) 8.dp else 20.dp)) {
            Text(
                stringResource(R.string.nav_settings),
                color = TextMain,
                fontSize = if (compactH) 20.sp else 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(if (compactH) 170.dp else 210.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(tiles) { t -> SettingTile(t) }
            }
        }
        if (showSection) {
            Dialog(onDismissRequest = { showSection = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                val shape = RoundedCornerShape(24.dp)
                Column(
                    Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth(0.9f)
                        .heightIn(max = (cfgS.screenHeightDp - 32).dp)
                        .clip(shape)
                        .background(com.sc.player.ui.theme.Bg)
                        .border(1.dp, GlassBorder, shape)
                        .padding(16.dp),
                ) {
                    Text(
                        stringResource(section.titleRes),
                        color = TextMain,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                    Column(Modifier.verticalScroll(rememberScrollState())) { rows() }
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(top = if (compactH) 10.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(if (compactH) 8.dp else 12.dp)) {
            if (!compactH) Box(Modifier.padding(horizontal = 24.dp)) { ScreenTitle(stringResource(R.string.nav_settings)) }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { SectionChip("👑 Pro", false) { showPro = true } }
                items(Section.values().toList()) { s ->
                    SectionChip(stringResource(s.titleRes), s == section) { sectionName = s.name }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                rows()
            }
        }
    }

    if (showPro) com.sc.player.ui.premium.ProSheet { showPro = false }
    update?.let { UpdateDialog(it, onDismiss = { update = null }) }

    if (editingProfile != null || addingProfile) {
        ProfileEditDialog(
            prefs = prefs,
            initial = editingProfile,
            isActive = editingProfile?.id == prefs.activeProfile().id,
            onChanged = { profilesTick++; onProfileChanged() },
            onClose = { editingProfile = null; addingProfile = false },
        )
    }

    if (showParental) {
        ParentalFlow(
            prefs = prefs,
            groups = groups,
            onChanged = {
                parentalTick++
                onParentalChanged()
            },
            onClose = { showParental = false },
        )
    }

    if (showDiag) DiagnosticsDialog(playlistId, onClose = { showDiag = false })

    if (showEpg) {
        var text by remember { mutableStateOf(epgUrl) }
        val applyEpg: (String) -> Unit = { value ->
            val url = value.trim()
            prefs.setEpgOverride(playlistId, url.ifBlank { null })
            epgUrl = url
            showEpg = false
            scope.launch {
                val repo = EpgRepository(ctx.applicationContext)
                repo.clearCache(playlistId)
                PlaylistRepository(ctx.applicationContext).playlists()
                    .firstOrNull { it.id == playlistId }
                    ?.let { repo.load(it, force = true) }
            }
        }
        AlertDialog(
            onDismissRequest = { showEpg = false },
            title = { Text(stringResource(R.string.epg_title)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.epg_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            },
            confirmButton = { TextButton(onClick = { applyEpg(text) }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { applyEpg("") }) { Text(stringResource(R.string.clear)) } },
        )
    }

    if (showCheck) {
        ChannelCheckDialog(
            all = checkable,
            favs = prefs.favorites(),
            previousDead = prefs.deadUrls(),
            onDismiss = { showCheck = false },
            onFinished = { dead, hide ->
                prefs.setDeadUrls(dead)
                deadCount = dead.size
                if (hide) {
                    prefs.setHideDead(true)
                    hideDead = true
                }
                showCheck = false
                onCleanupChanged()
            },
        )
    }
    if (showHide) {
        HideGroupsDialog(
            groups = groups,
            hidden = hiddenSet,
            onDismiss = { showHide = false },
            onSave = {
                prefs.setHiddenGroups(it)
                hiddenSet = it
                showHide = false
                onCleanupChanged()
            },
        )
    }
    if (showLanguages) {
        val options = listOf<AppLanguage?>(null) + appLanguages
        AlertDialog(
            onDismissRequest = { showLanguages = false },
            title = { Text(stringResource(R.string.set_language)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { lang ->
                        val label = lang?.nativeName ?: stringResource(R.string.set_language_system)
                        val chosen = lang?.code == savedLang
                        TextButton(
                            onClick = {
                                showLanguages = false
                                if (lang?.code != savedLang) {
                                    LocaleHelper.save(ctx, lang?.code)
                                    savedLang = lang?.code
                                    ctx.findActivity()?.recreate()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                (if (chosen) "✓  " else "") + label,
                                fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showLanguages = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private enum class Section(val titleRes: Int, val icon: ImageVector) {
    General(R.string.set_sec_general, Icons.Filled.Palette),
    Playback(R.string.set_sec_playback, Icons.Filled.PlayArrow),
    Content(R.string.set_sec_content, Icons.Filled.VideoLibrary),
    Profiles(R.string.set_sec_profiles, Icons.Filled.Person),
    Account(R.string.set_sec_account, Icons.Filled.AccountCircle),
    Data(R.string.set_sec_data, Icons.Filled.Storage),
    About(R.string.nav_about, Icons.Filled.Info),
}

@Composable
private fun sectionAccent(s: Section): Color = when (s.ordinal % 5) {
    0 -> Cyan
    1 -> Violet
    2 -> Blue
    3 -> Color(0xFFFFA94D)
    else -> Color(0xFFB388FF)
}

@Composable
private fun IconBadge(icon: ImageVector, accent: Color, size: Dp = 38.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size / 3.4f)).background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = accent, modifier = Modifier.size(size * 0.55f)) }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(shape)
            .background(Glass)
            .border(1.dp, GlassBorder, shape)
            .padding(vertical = 4.dp),
        content = content,
    )
}

@Composable
private fun SectionItem(s: Section, selected: Boolean, accent: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (selected) BrandBrush else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
            .border(if (focused) 2.dp else 0.dp, if (focused) Cyan else Color.Transparent, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(s.icon, null, tint = if (selected) Ink else accent, modifier = Modifier.size(21.dp))
        Text(
            stringResource(s.titleRes),
            color = if (selected) Ink else TextMain,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun SectionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Text(
        text,
        color = if (selected) Ink else TextMain,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) BrandBrush else Brush.linearGradient(listOf(Glass, Glass)))
            .border(1.dp, if (selected) Color.Transparent else GlassBorder, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(value, color = TextMuted, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.weight(1.4f))
    }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 62.dp, end = 14.dp).height(1.dp).background(GlassBorder))
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String?,
    value: String?,
    icon: ImageVector? = null,
    lead: ImageVector? = null,
    accent: Color = Cyan,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .border(if (focused) 2.dp else 0.dp, if (focused) Cyan else Color.Transparent, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // قيمة طويلة تنزل تحت العنوان بدل ما تضغطه
        val stacked = value != null && value.length > 18
        if (lead != null) IconBadge(lead, accent)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, color = TextMuted, fontSize = 13.sp) }
            if (stacked) Text(value!!, color = TextMuted, fontSize = 13.sp)
        }
        if (!stacked) value?.let {
            Text(it, color = TextMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp))
        }
        if (icon != null) Icon(icon, null, tint = Cyan, modifier = Modifier.size(22.dp))
        else Text("›", color = TextMuted, fontSize = 24.sp)
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        color = Cyan,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 2.dp),
    )
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, lead: ImageVector? = null, accent: Color = Cyan, onChange: (Boolean) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .border(if (focused) 2.dp else 0.dp, if (focused) Cyan else Color.Transparent, shape)
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (lead != null) IconBadge(lead, accent)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextMuted, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Violet,
                checkedThumbColor = Ink,
                uncheckedTrackColor = Glass,
                uncheckedThumbColor = TextMuted,
                uncheckedBorderColor = GlassBorder,
            ),
        )
    }
}

private class TileData(
    val icon: ImageVector,
    val title: String,
    val value: String?,
    val on: Boolean?,
    val onClick: () -> Unit,
)

/** زر في شبكة الإعدادات: أيقونة + عنوان + قيمة صغيرة، ونقطة خضرا/رمادي للمفاتيح. */
@Composable
private fun SettingTile(t: TileData) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(Color.White.copy(alpha = if (focused) 0.12f else 0.06f))
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else Color.White.copy(alpha = 0.14f), shape)
            .clickable(role = Role.Button, onClick = t.onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(t.icon, null, tint = Cyan, modifier = Modifier.size(26.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(t.title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            t.value?.let { Text(it, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        if (t.on != null) {
            Box(
                Modifier
                    .size(11.dp)
                    .background(if (t.on) Color(0xFF34D399) else Color.White.copy(alpha = 0.25f), androidx.compose.foundation.shape.CircleShape),
            )
        }
    }
}
