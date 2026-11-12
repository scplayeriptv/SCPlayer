package com.sc.player.data.update

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.sc.player.R
import com.sc.player.service.Notifications
import com.sc.player.service.UpdateService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** تحميل التحديث على مستوى التطبيق كله (مش مربوط بالنافذة) عشان يكمّل في الخلفية. */
object UpdateManager {
    sealed interface State {
        data object Idle : State
        data class Downloading(val progress: Float, val done: Long, val total: Long) : State
        data class Ready(val file: File) : State
        data object Failed : State
    }

    const val ID_PROGRESS = 2002
    private const val ID_READY = 2003

    val state = MutableStateFlow<State>(State.Idle)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var lastPct = -1

    fun start(ctx: Context, info: UpdateInfo) {
        if (job?.isActive == true) return
        val app = ctx.applicationContext
        Notifications.ensureChannels(app)
        state.value = State.Downloading(0f, 0, info.downloadSize)
        lastPct = -1
        runCatching { ContextCompat.startForegroundService(app, Intent(app, UpdateService::class.java)) }
        job = scope.launch {
            runCatching {
                Updater(app).download(info) { p, d, t ->
                    state.value = State.Downloading(p, d, t)
                    val pct = (p * 100).toInt()
                    if (pct != lastPct) {
                        lastPct = pct
                        notify(app, ID_PROGRESS, progressNotification(app, pct))
                    }
                }
            }.fold(
                onSuccess = {
                    state.value = State.Ready(it)
                    runCatching { app.stopService(Intent(app, UpdateService::class.java)) }
                    notify(app, ID_READY, readyNotification(app, it))
                },
                onFailure = {
                    state.value = State.Failed
                    runCatching { app.stopService(Intent(app, UpdateService::class.java)) }
                },
            )
        }
    }

    fun reset() {
        if (job?.isActive != true) state.value = State.Idle
    }

    fun cancelReadyNotification(ctx: Context) {
        runCatching { NotificationManagerCompat.from(ctx).cancel(ID_READY) }
    }

    /** pct < 0 = غير محدد. */
    fun progressNotification(ctx: Context, pct: Int): Notification =
        NotificationCompat.Builder(ctx, Notifications.CH_UPDATE)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(ctx.getString(R.string.notif_update_downloading))
            .setProgress(100, pct.coerceAtLeast(0), pct < 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun readyNotification(ctx: Context, apk: File): Notification {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = PendingIntent.getActivity(ctx, 3, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(ctx, Notifications.CH_UPDATE_READY)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(ctx.getString(R.string.notif_update_ready))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
    }

    private fun notify(ctx: Context, id: Int, n: Notification) {
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (e: SecurityException) {
            // مفيش إذن إشعارات: التحميل بيكمّل برضه
        }
    }
}
