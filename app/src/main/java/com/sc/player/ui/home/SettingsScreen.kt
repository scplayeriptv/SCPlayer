package com.sc.player.ui.home

import android.content.Context
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

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(stringResource(R.string.nav_settings))

        SettingRow(
            stringResource(R.string.set_language),
            appLanguages.firstOrNull { it.code == savedLang }?.nativeName ?: stringResource(R.string.set_language_system),
        ) { showLanguages = true }

        SettingRow(
            stringResource(R.string.parental_title),
            // parentalTick بس عشان السطر يتحدّث بعد أي تغيير
            parentalTick.let {
                val n = prefs.lockedGroups().size
                when {
                    !prefs.hasPin() -> stringResource(R.string.parental_off)
                    ParentalSession.unlocked -> stringResource(R.string.parental_unlocked)
                    else -> stringResource(R.string.parental_on, n)
                }
            },
        ) { showParental = true }

        SettingRow(
            stringResource(R.string.epg_title),
            if (epgUrl.isBlank()) stringResource(R.string.epg_auto) else epgUrl,
        ) { showEpg = true }

        SettingRow(stringResource(R.string.nav_playlist), stringResource(R.string.set_playlists_sub), onClick = onPlaylists)

        SettingRow(
            stringResource(R.string.set_clear_cache),
            if (cacheLoaded) mb(cache) else stringResource(R.string.calculating),
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

        SettingRow(
            stringResource(R.string.set_check_updates),
            if (checking) stringResource(R.string.checking)
            else updateStatus ?: stringResource(R.string.installed_build, updater.currentVersionCode().toInt()),
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

        SettingRow(
            stringResource(R.string.set_reset),
            if (resetDone) stringResource(R.string.done) else stringResource(R.string.set_reset_sub),
        ) {
            onResetUserData()
            resetDone = true
        }

        SettingRow(stringResource(R.string.nav_about), stringResource(R.string.set_about_sub), onClick = onAbout)
    }

    update?.let { UpdateDialog(it, onDismiss = { update = null }) }

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

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    GlassCard(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .border(if (focused) 2.dp else 0.dp, if (focused) Cyan else GlassBorder, shape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextMuted, fontSize = 13.sp)
            }
            Text("›", color = TextMuted, fontSize = 24.sp)
        }
    }
}
