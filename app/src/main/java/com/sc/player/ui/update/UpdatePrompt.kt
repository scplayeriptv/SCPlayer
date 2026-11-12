package com.sc.player.ui.update

import com.sc.player.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.data.update.UpdateInfo
import com.sc.player.data.update.UpdateManager
import com.sc.player.data.update.UpdateState
import com.sc.player.data.update.Updater
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.TextMuted

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

/** نافذة التحديث: التحميل بيكمّل في الخلفية (خدمة + إشعار) حتى لو قفلت النافذة. */
@Composable
fun UpdateDialog(update: UpdateInfo, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val updater = remember { Updater(ctx) }
    val state by UpdateManager.state.collectAsState()
    var message by remember { mutableStateOf<String?>(null) }
    fun mb(b: Long) = "%.1f MB".format(b / 1048576.0)
    val downloading = state as? UpdateManager.State.Downloading

    LaunchedEffect(state) {
        val s = state
        if (s is UpdateManager.State.Ready) {
            UpdateManager.cancelReadyNotification(ctx)
            UpdateManager.reset()
            onDismiss()
            updater.install(s.file)
        } else if (s is UpdateManager.State.Failed) {
            message = ctx.getString(R.string.download_failed)
            UpdateManager.reset()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_title)) },
        text = {
            Column {
                Text(stringResource(R.string.update_text, update.versionCode.toInt()))
                if (update.downloadSize > 0 && downloading == null) {
                    Text(
                        stringResource(
                            if (update.isDelta) R.string.update_size_delta else R.string.update_size,
                            mb(update.downloadSize),
                        ),
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                downloading?.let { d ->
                    LinearProgressIndicator(
                        progress = { d.progress },
                        color = Cyan,
                        trackColor = GlassBorder,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                    Text(
                        if (d.total > 0) stringResource(R.string.downloading_size, (d.progress * 100).toInt(), mb(d.done), mb(d.total))
                        else stringResource(R.string.downloading, (d.progress * 100).toInt()),
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                message?.let { Text(it, color = Color(0xFFFCA5A5), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = downloading == null,
                onClick = {
                    message = null
                    if (!updater.canInstall()) {
                        updater.openInstallPermissionSettings()
                        message = ctx.getString(R.string.allow_install)
                    } else {
                        UpdateManager.start(ctx, update)
                    }
                },
            ) { Text(stringResource(R.string.update_now)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(if (downloading != null) R.string.update_bg else R.string.later))
            }
        },
    )
}
