package com.sc.player.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.data.update.UpdateInfo
import com.sc.player.data.update.UpdateState
import com.sc.player.data.update.Updater
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.TextMuted
import kotlinx.coroutines.launch

/** بيفحص التحديث مرة في كل تشغيل، ولو فيه نسخة أحدث بيعرض نافذة تحديث. */
@Composable
fun UpdatePrompt() {
    val ctx = LocalContext.current
    val updater = remember { Updater(ctx) }
    var info by remember { mutableStateOf<UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        if (!UpdateState.checked) {
            UpdateState.checked = true
            info = updater.check()
        }
    }

    val update = info ?: return
    UpdateDialog(update, onDismiss = { info = null })
}

/** نافذة التحديث: تحميل الـ APK وفتح شاشة التثبيت. */
@Composable
fun UpdateDialog(update: UpdateInfo, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val updater = remember { Updater(ctx) }
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf<Float?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (progress == null) onDismiss() },
        title = { Text("Update available") },
        text = {
            Column {
                Text("A newer version of SC Player is available (build ${update.versionCode}). Update now?")
                progress?.let { p ->
                    LinearProgressIndicator(
                        progress = { p },
                        color = Cyan,
                        trackColor = GlassBorder,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                    Text("Downloading ${(p * 100).toInt()}%", color = TextMuted, fontSize = 13.sp)
                }
                message?.let { Text(it, color = Color(0xFFFCA5A5), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = progress == null,
                onClick = {
                    message = null
                    if (!updater.canInstall()) {
                        updater.openInstallPermissionSettings()
                        message = "Enable \"Allow from this source\", then come back and tap Update again."
                    } else {
                        progress = 0f
                        scope.launch {
                            runCatching { updater.download(update) { progress = it } }.fold(
                                onSuccess = {
                                    progress = null
                                    onDismiss()
                                    updater.install(it)
                                },
                                onFailure = {
                                    progress = null
                                    message = "Download failed. Try again."
                                },
                            )
                        }
                    }
                },
            ) { Text("Update now") }
        },
        dismissButton = {
            TextButton(enabled = progress == null, onClick = onDismiss) { Text("Later") }
        },
    )
}
