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

    LaunchedEffect(Unit) {
        cache = withContext(Dispatchers.IO) { cacheBytes(ctx) }
        cacheLoaded = true
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Settings")

        SettingRow("Playlists", "Add, switch or remove playlists", onClick = onPlaylists)

        SettingRow(
            "Clear Cache",
            if (cacheLoaded) mb(cache) else "Calculating...",
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
            "Check for Updates",
            if (checking) "Checking..." else updateStatus ?: "Installed build ${updater.currentVersionCode()}",
        ) {
            if (!checking) {
                checking = true
                scope.launch {
                    val info = updater.check()
                    checking = false
                    if (info == null) {
                        updateStatus = "You're up to date (build ${updater.currentVersionCode()})"
                    } else {
                        updateStatus = "Build ${info.versionCode} is available"
                        update = info
                    }
                }
            }
        }

        SettingRow(
            "Reset Favorites & Recents",
            if (resetDone) "Done" else "Clears your favorites and watch history",
        ) {
            onResetUserData()
            resetDone = true
        }

        SettingRow("About", "Version and info", onClick = onAbout)
    }

    update?.let { UpdateDialog(it, onDismiss = { update = null }) }
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
