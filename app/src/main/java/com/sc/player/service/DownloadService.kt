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
        val isVod = item.kind == 0
        var written = if (isVod && file.exists()) file.length() else 0L
        var total = if (isVod && written > 0) item.total else 0L
        DownloadStore.update(ctx, id) { it.copy(state = 1, error = "") }
        refreshNotification(true)
        var removed = false
        var stoppedByUser = false
        var limitHit = false
        val started = System.currentTimeMillis()
        var stalls = 0
        try {
            while (true) {
                var reachedEnd = false
                var progressed = false
                val rb = Request.Builder().url(item.url).header("User-Agent", "SCPlayer/0.1 (Android)")
                // بعض السيرفرات بتقطع الرد بعد شوية لو الطلب من غير Range، فبنطلب Range دايمًا ونكمّل من آخر نقطة
                if (isVod) rb.header("Range", "bytes=$written-")
                client.newCall(rb.build()).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        if (resp.code == 416 && total > 0 && written >= total) { reachedEnd = true; return@use }
                        throw IOException("HTTP ${resp.code}")
                    }
                    val ct = resp.header("Content-Type").orEmpty().lowercase()
                    if (!isVod && (ct.contains("mpegurl") || item.url.substringBefore('?').endsWith(".m3u8"))) {
                        throw IllegalStateException("hls")
                    }
                    if (isVod && (ct.contains("text/") || ct.contains("json") || ct.contains("mpegurl"))) {
                        throw IllegalStateException("type: $ct")
                    }
                    val body = resp.body ?: throw IOException("empty body")
                    if (isVod && resp.code == 200 && written > 0) written = 0L // السيرفر تجاهل Range: نبدأ من الأول
                    val append = resp.code == 206 && written > 0
                    val len = body.contentLength()
                    val range = resp.header("Content-Range")?.substringAfter('/', "")?.toLongOrNull()
                    total = when {
                        range != null && range > 0 -> range
                        len > 0 -> written + len
                        else -> total
                    }
                    DownloadStore.update(ctx, id, false) { it.copy(total = total, bytes = written) }
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
                                progressed = true
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
                if (removed || stoppedByUser || limitHit || !isVod) break
                if (total > 0 && written >= total) break
                if (total == 0L && reachedEnd) break
                if (!progressed) {
                    if (++stalls >= 3) throw IOException("stalled at ${written / 1024} KB")
                } else {
                    stalls = 0
                }
            }
            val w = written
            if (isVod && !removed && !stoppedByUser && w < 2L * 1024 * 1024) {
                throw IllegalStateException("small: ${w / 1024} KB")
            }
            when {
                removed -> { runCatching { file.delete() } }
                stoppedByUser && isVod -> DownloadStore.update(ctx, id) { it.copy(state = 4, bytes = w) }
                else -> DownloadStore.update(ctx, id) { it.copy(state = 2, bytes = w, total = if (it.total == 0L || !isVod) w else it.total) }
            }
        } catch (e: CancellationException) {
            DownloadStore.update(ctx, id) { it.copy(state = 4, bytes = written) }
            throw e
        } catch (e: IllegalStateException) {
            if (isVod && e.message != "hls") runCatching { file.delete() }
            DownloadStore.update(ctx, id) { it.copy(state = 3, bytes = 0L, error = e.message.orEmpty()) }
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
