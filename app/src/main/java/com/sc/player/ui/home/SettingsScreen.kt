package com.sc.player.ui.home

import android.content.Context
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
    onParentalChanged: () -> Unit,
    onProfileChanged: () -> Unit,
    onPlaylists: () -> Unit,
    onAbout: () -> Unit,
    onResetUserData: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var epgUrl by remember { mutableStateOf(prefs.epgOverride(playlistId).orEmpty()) }

    LaunchedEffect(Unit) {
        cache = withContext(Dispatchers.IO) { cacheBytes(ctx) }
        cacheLoaded = true
    }

    var sectionName by rememberSaveable { mutableStateOf(Section.General.name) }
    val section = Section.valueOf(sectionName)
    var autoPlay by remember { mutableStateOf(prefs.autoPlay()) }
    var hw by remember { mutableStateOf(prefs.hardwareDecoding()) }
    var bg by remember { mutableStateOf(prefs.backgroundPlay()) }
    var pip by remember { mutableStateOf(prefs.pip()) }
    var reconnect by remember { mutableStateOf(prefs.autoReconnect()) }
    var bufferMode by remember { mutableStateOf(prefs.bufferMode()) }
    var stats by remember { mutableStateOf(prefs.showStats()) }
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
    val wide = LocalConfiguration.current.screenWidthDp >= 720

    val rows: @Composable () -> Unit = {
        when (section) {
            Section.General -> {
                GroupLabel(stringResource(R.string.set_grp_appearance))
                SettingRow(
                    stringResource(R.string.set_language),
                    null,
                    value = appLanguages.firstOrNull { it.code == savedLang }?.nativeName ?: stringResource(R.string.set_language_system),
                ) { showLanguages = true }
                RowDivider()
                ThemePicker(prefs)
                GroupLabel(stringResource(R.string.set_grp_maintenance))
                SettingRow(
                    stringResource(R.string.set_clear_cache),
                    null,
                    value = if (cacheLoaded) mb(cache) else stringResource(R.string.calculating),
                    icon = Icons.Filled.Delete,
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
                SettingRow(
                    stringResource(R.string.set_check_updates),
                    null,
                    value = if (checking) stringResource(R.string.checking)
                    else updateStatus ?: stringResource(R.string.installed_build, updater.currentVersionCode().toInt()),
                    icon = Icons.Filled.Refresh,
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
                GroupLabel(stringResource(R.string.set_grp_reset))
                SettingRow(
                    stringResource(R.string.set_reset),
                    null,
                    value = if (resetDone) stringResource(R.string.done) else stringResource(R.string.set_reset_sub),
                ) {
                    com.sc.player.data.PositionStore(ctx).clear()
                    onResetUserData()
                    resetDone = true
                }
                RowDivider()
                SettingRow(
                    stringResource(R.string.set_restore),
                    null,
                    value = null,
                ) {
                    prefs.restoreDefaults()
                    autoPlay = prefs.autoPlay()
                    hw = prefs.hardwareDecoding()
                    bg = prefs.backgroundPlay()
                    pip = prefs.pip()
                    reconnect = prefs.autoReconnect()
                    bufferMode = prefs.bufferMode()
                    stats = prefs.showStats()
                }
            }
            Section.Account -> {
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
            Section.Profiles -> {
                val all = profilesTick.let { prefs.profiles() }
                val activeId = profilesTick.let { prefs.activeProfile().id }
                all.forEach { p ->
                    SettingRow(
                        "${p.avatarEmoji()}  ${p.displayName()}",
                        if (p.kids) stringResource(R.string.profile_kids_badge) else null,
                        value = if (p.id == activeId) stringResource(R.string.profile_active) else null,
                    ) { editingProfile = p }
                    RowDivider()
                }
                if (all.size < com.sc.player.data.UserPrefs.MAX_PROFILES) {
                    SettingRow(stringResource(R.string.profile_add), null, value = null) { addingProfile = true }
                }
            }
            Section.Parental -> SettingRow(
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
            ) { showParental = true }
            Section.Playback -> {
                GroupLabel(stringResource(R.string.set_grp_playback))
                ToggleRow(stringResource(R.string.set_auto_play), stringResource(R.string.set_auto_play_sub), autoPlay) {
                    autoPlay = it
                    prefs.setAutoPlay(it)
                }
                RowDivider()
                ToggleRow(stringResource(R.string.set_bg_play), stringResource(R.string.set_bg_play_sub), bg) {
                    bg = it
                    prefs.setBackgroundPlay(it)
                }
                RowDivider()
                ToggleRow(stringResource(R.string.set_pip), stringResource(R.string.set_pip_sub), pip) {
                    pip = it
                    prefs.setPip(it)
                }
                GroupLabel(stringResource(R.string.set_grp_performance))
                ToggleRow(stringResource(R.string.set_hw), stringResource(R.string.set_hw_sub), hw) {
                    hw = it
                    prefs.setHardwareDecoding(it)
                }
                RowDivider()
                ToggleRow(stringResource(R.string.set_reconnect), stringResource(R.string.set_reconnect_sub), reconnect) {
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
                ) {
                    bufferMode = (bufferMode + 1) % 3
                    prefs.setBufferMode(bufferMode)
                }
                GroupLabel(stringResource(R.string.set_grp_diag))
                ToggleRow(stringResource(R.string.set_stats), stringResource(R.string.set_stats_sub), stats) {
                    stats = it
                    prefs.setShowStats(it)
                }
            }
            Section.Guide -> SettingRow(
                stringResource(R.string.epg_title),
                null,
                value = if (epgUrl.isBlank()) stringResource(R.string.epg_auto) else epgUrl,
            ) { showEpg = true }
            Section.Playlists -> SettingRow(
                stringResource(R.string.nav_playlist),
                null,
                value = stringResource(R.string.set_playlists_sub),
                onClick = onPlaylists,
            )
            Section.About -> SettingRow(
                stringResource(R.string.nav_about),
                null,
                value = stringResource(R.string.set_about_sub),
                onClick = onAbout,
            )
        }
    }

    if (wide) {
        Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            GlassCard(Modifier.width(280.dp).fillMaxHeight()) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        stringResource(R.string.nav_settings),
                        color = TextMain,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 10.dp),
                    )
                    Section.values().forEach { s ->
                        SectionItem(s, s == section) { sectionName = s.name }
                    }
                }
            }
            GlassCard(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text(
                        stringResource(section.titleRes),
                        color = TextMain,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 14.dp, top = 14.dp, bottom = 8.dp),
                    )
                    rows()
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.padding(horizontal = 24.dp)) { ScreenTitle(stringResource(R.string.nav_settings)) }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Section.values().toList()) { s ->
                    SectionChip(stringResource(s.titleRes), s == section) { sectionName = s.name }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 6.dp)) { rows() }
                }
            }
        }
    }

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
    General(R.string.set_sec_general, Icons.Filled.Settings),
    Account(R.string.set_sec_account, Icons.Filled.AccountCircle),
    Profiles(R.string.set_sec_profiles, Icons.Filled.Person),
    Parental(R.string.parental_title, Icons.Filled.Lock),
    Playback(R.string.set_sec_playback, Icons.Filled.PlayArrow),
    Guide(R.string.set_sec_guide, Icons.Filled.DateRange),
    Playlists(R.string.nav_playlist, Icons.Filled.VideoLibrary),
    About(R.string.nav_about, Icons.Filled.Info),
}

@Composable
private fun SectionItem(s: Section, selected: Boolean, onClick: () -> Unit) {
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
        Icon(s.icon, null, tint = if (selected) Ink else TextMuted, modifier = Modifier.size(20.dp))
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
    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(GlassBorder))
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String?,
    value: String?,
    icon: ImageVector? = null,
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
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
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
