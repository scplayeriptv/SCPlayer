package com.sc.player.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sc.player.R
import com.sc.player.data.DownloadStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** خدمة أمامية بتنزّل الأفلام وتسجّل البث (ملف واحد لكل عنصر، اتنين في نفس الوقت). */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val stopReq = ConcurrentHashMap.newKeySet<String>()
    private val gate = Semaphore(2)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    @Volatile private var lastNotify = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifications.ensureChannels(this)
        DownloadStore.load(applicationContext)
        showForeground()
        val id = intent?.getStringExtra(EXTRA_ID)
        if (id != null) {
            if (intent?.action == ACTION_STOP) stopReq.add(id) else launchJob(id)
        }
        if (jobs.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    private fun notification(): android.app.Notification {
        val running = DownloadStore.items.value.filter { it.state == 1 }
        val first = running.firstOrNull()
        val pct = if (first != null && first.total > 0) (first.bytes * 100 / first.total).toInt() else -1
        return NotificationCompat.Builder(this, Notifications.CH_DOWNLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.dl_notif_title, running.size.coerceAtLeast(1)))
            .setContentText(first?.title.orEmpty())
            .setProgress(100, pct.coerceAtLeast(0), pct < 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun showForeground() {
        val n = notification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIF_ID, n)
            }
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun refreshNotification(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastNotify < 1000) return
        lastNotify = now
        runCatching { getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, notification()) }
    }

    private fun launchJob(id: String) {
        if (jobs[id]?.isActive == true) return
        stopReq.remove(id)
        DownloadStore.update(applicationContext, id) { it.copy(state = 0, error = "") }
        val job = scope.launch { gate.withPermit { execute(id) } }
        jobs[id] = job
        job.invokeOnCompletion {
            jobs.remove(id)
            if (jobs.isEmpty()) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun execute(id: String) {
        val ctx = applicationContext
        val item = DownloadStore.get(id) ?: return
        val file = File(item.path)
        file.parentFile?.mkdirs()
        var offset = if (item.kind == 0 && file.exists()) file.length() else 0L
        DownloadStore.update(ctx, id) { it.copy(state = 1, error = "") }
        refreshNotification(true)
        var removed = false
        var stoppedByUser = false
        var reachedEnd = false
        var limitHit = false
        var written = offset
        var total = 0L
        try {
            val rb = Request.Builder().url(item.url).header("User-Agent", "VLC/3.0.18 LibVLC/3.0.18")
            if (offset > 0) rb.header("Range", "bytes=$offset-")
            client.newCall(rb.build()).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                val ct = resp.header("Content-Type").orEmpty().lowercase()
                if (item.kind != 0 && (ct.contains("mpegurl") || item.url.substringBefore('?').endsWith(".m3u8"))) {
                    throw IllegalStateException("hls")
                }
                val append = resp.code == 206 && offset > 0
                if (!append) { offset = 0L; written = 0L }
                val body = resp.body ?: throw IOException("empty body")
                val len = body.contentLength()
                total = if (len > 0) offset + len else 0L
                DownloadStore.update(ctx, id, false) { it.copy(total = total, bytes = written) }
                val started = System.currentTimeMillis()
                var lastUi = 0L
                val buf = ByteArray(64 * 1024)
                FileOutputStream(file, append).use { out ->
                    body.byteStream().use { input ->
                        while (true) {
                            if (stopReq.remove(id)) { stoppedByUser = true; break }
                            if (DownloadStore.get(id) == null) { removed = true; break }
                            if (item.limitMin > 0 && System.currentTimeMillis() - started > item.limitMin * 60_000L) { limitHit = true; break }
                            val n = input.read(buf)
                            if (n < 0) { reachedEnd = true; break }
                            out.write(buf, 0, n)
                            written += n
                            val now = System.currentTimeMillis()
                            if (now - lastUi > 500) {
                                lastUi = now
                                val w = written
                                DownloadStore.update(ctx, id, false) { it.copy(bytes = w) }
                                refreshNotification()
                            }
                        }
                    }
                }
            }
            val w = written
            when {
                removed -> { runCatching { file.delete() } }
                stoppedByUser && item.kind == 0 -> DownloadStore.update(ctx, id) { it.copy(state = 4, bytes = w) }
                reachedEnd && item.kind == 0 && total > 0 && w < total ->
                    DownloadStore.update(ctx, id) { it.copy(state = 3, bytes = w, error = "incomplete") }
                else -> DownloadStore.update(ctx, id) { it.copy(state = 2, bytes = w, total = if (it.total == 0L) w else it.total) }
            }
        } catch (e: CancellationException) {
            DownloadStore.update(ctx, id) { it.copy(state = 4, bytes = written) }
            throw e
        } catch (e: IllegalStateException) {
            DownloadStore.update(ctx, id) { it.copy(state = 3, error = e.message.orEmpty()) }
        } catch (e: Exception) {
            val w = written
            DownloadStore.update(ctx, id) { it.copy(state = 3, bytes = w, error = e.message.orEmpty()) }
        }
        refreshNotification(true)
    }

    companion object {
        const val EXTRA_ID = "id"
        const val ACTION_START = "com.sc.player.DL_START"
        const val ACTION_STOP = "com.sc.player.DL_STOP"
        const val NOTIF_ID = 4100
    }
}
