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
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
        var written = if (isVod && file.exists() && item.segs == 0) file.length() else 0L
        var total = if (isVod && written > 0) item.total else 0L
        var hls = false
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
                var hlsText: String? = null
                var hlsUrl = item.url
                val rb = Request.Builder().url(item.url).header("User-Agent", "SCPlayer/0.1 (Android)")
                // بعض السيرفرات بتقطع الرد بعد شوية لو الطلب من غير Range، فبنطلب Range دايمًا ونكمّل من آخر نقطة
                if (isVod) rb.header("Range", "bytes=$written-")
                client.newCall(rb.build()).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        if (resp.code == 416 && total > 0 && written >= total) { reachedEnd = true; return@use }
                        throw IOException("HTTP ${resp.code}")
                    }
                    val ct = resp.header("Content-Type").orEmpty().lowercase()
                    val body = resp.body ?: throw IOException("empty body")
                    val looksText = ct.contains("mpegurl") || ct.contains("text/") || ct.contains("json") ||
                        item.url.substringBefore('?').endsWith(".m3u8")
                    if (looksText) {
                        // قوائم HLS (m3u8) بتتنزّل قطعة قطعة؛ أي نص تاني مش فيديو
                        val txt = body.string()
                        if (txt.trimStart().startsWith("#EXTM3U")) {
                            hlsText = txt
                            hlsUrl = resp.request.url.toString()
                            return@use
                        }
                        throw IllegalStateException("type: $ct")
                    }
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
                if (hlsText != null) {
                    hls = true
                    val r = downloadHls(ctx, id, item, hlsText!!, hlsUrl)
                    written = r.bytes
                    if (r.code == 1) stoppedByUser = true
                    if (r.code == 2) removed = true
                    break
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
            if (isVod && !hls && !removed && !stoppedByUser && w < 2L * 1024 * 1024) {
                throw IllegalStateException("small: ${w / 1024} KB")
            }
            when {
                removed -> { runCatching { file.delete() } }
                stoppedByUser && isVod -> DownloadStore.update(ctx, id) { it.copy(state = 4, bytes = w) }
                else -> DownloadStore.update(ctx, id) { it.copy(state = 2, bytes = w, total = if (it.total == 0L || !isVod || hls) w else it.total) }
            }
        } catch (e: CancellationException) {
            DownloadStore.update(ctx, id) { it.copy(state = 4, bytes = written) }
            throw e
        } catch (e: IllegalStateException) {
            if (isVod && !hls) runCatching { file.delete() }
            DownloadStore.update(ctx, id) { it.copy(state = 3, bytes = 0L, error = e.message.orEmpty()) }
        } catch (e: Exception) {
            val w = written
            DownloadStore.update(ctx, id) { it.copy(state = 3, bytes = w, error = e.message.orEmpty()) }
        }
        refreshNotification(true)
    }

    private class Seg(val uri: String, val seq: Long, val keyUri: String?, val iv: ByteArray?)

    private class Media(val segs: List<Seg>, val map: String?, val target: Int, val endList: Boolean)

    private class HlsResult(val code: Int, val bytes: Long)

    private fun resolve(base: String, ref: String): String =
        base.toHttpUrlOrNull()?.resolve(ref)?.toString() ?: ref

    private fun fetchText(url: String): String {
        val req = Request.Builder().url(url).header("User-Agent", "SCPlayer/0.1 (Android)").build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
            return r.body?.string() ?: throw IOException("empty body")
        }
    }

    private fun fetchBytes(url: String): ByteArray {
        val req = Request.Builder().url(url).header("User-Agent", "SCPlayer/0.1 (Android)").build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
            return r.body?.bytes() ?: throw IOException("empty body")
        }
    }

    private fun attr(line: String, name: String): String? =
        Regex("(?:^|[:,])$name=(\"[^\"]*\"|[^,]*)").find(line)?.groupValues?.get(1)?.trim('"')

    private fun parseMedia(text: String, url: String): Media {
        var seq = 0L
        var target = 6
        var map: String? = null
        var keyUri: String? = null
        var keyIv: ByteArray? = null
        var pending = false
        val out = ArrayList<Seg>()
        var end = false
        for (raw in text.lines()) {
            val l = raw.trim()
            when {
                l.startsWith("#EXT-X-MEDIA-SEQUENCE:") -> seq = l.substringAfter(':').trim().toLongOrNull() ?: 0L
                l.startsWith("#EXT-X-TARGETDURATION:") -> target = l.substringAfter(':').trim().toIntOrNull() ?: 6
                l.startsWith("#EXT-X-ENDLIST") -> end = true
                l.startsWith("#EXT-X-MAP:") -> map = attr(l, "URI")?.let { resolve(url, it) }
                l.startsWith("#EXT-X-KEY:") -> {
                    val method = attr(l, "METHOD") ?: "NONE"
                    if (method == "NONE") { keyUri = null; keyIv = null }
                    else if (method == "AES-128") {
                        keyUri = attr(l, "URI")?.let { resolve(url, it) }
                        keyIv = attr(l, "IV")?.removePrefix("0x")?.removePrefix("0X")?.let { hex ->
                            val padded = hex.padStart(32, '0')
                            ByteArray(16) { i -> padded.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
                        }
                    } else throw IllegalStateException("enc: $method")
                }
                l.startsWith("#EXTINF") -> pending = true
                l.isNotEmpty() && !l.startsWith("#") && pending -> {
                    out += Seg(resolve(url, l), seq, keyUri, keyIv)
                    seq++
                    pending = false
                }
            }
        }
        return Media(out, map, target, end)
    }

    private val keys = ConcurrentHashMap<String, ByteArray>()

    private fun segmentBytes(seg: Seg): ByteArray {
        var lastErr: IOException? = null
        repeat(3) {
            try {
                val raw = fetchBytes(seg.uri)
                val k = seg.keyUri ?: return raw
                val key = keys.getOrPut(k) { fetchBytes(k) }
                val iv = seg.iv ?: ByteArray(16).also { b ->
                    var v = seg.seq
                    for (i in 15 downTo 8) { b[i] = (v and 0xFF).toByte(); v = v shr 8 }
                }
                val c = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
                c.init(javax.crypto.Cipher.DECRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"), javax.crypto.spec.IvParameterSpec(iv))
                return c.doFinal(raw)
            } catch (e: IOException) {
                lastErr = e
            }
        }
        throw lastErr ?: IOException("segment failed")
    }

    /** تنزيل/تسجيل HLS: بيجيب القطع ويلصقها في ملف واحد. code: 0 خلص، 1 المستخدم وقّف، 2 اتحذف. */
    private fun downloadHls(ctx: android.content.Context, id: String, item: com.sc.player.data.DownloadItem, text0: String, url0: String): HlsResult {
        var url = url0
        var text = text0
        if (text.contains("#EXT-X-STREAM-INF")) {
            val lines = text.lines()
            var best = -1L
            var bestUri: String? = null
            for (i in lines.indices) {
                if (!lines[i].startsWith("#EXT-X-STREAM-INF")) continue
                val bw = attr(lines[i], "BANDWIDTH")?.toLongOrNull() ?: 0L
                val uri = lines.drop(i + 1).firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                if (uri != null && bw > best) { best = bw; bestUri = uri }
            }
            url = resolve(url, bestUri ?: throw IOException("no variant"))
            text = fetchText(url)
        }
        var media = parseMedia(text, url)
        if (media.segs.isEmpty()) throw IOException("empty playlist")
        val live = !media.endList && item.kind != 0
        val file = File(item.path)
        val resumeAt = if (!live) item.segs.coerceAtMost(media.segs.size) else 0
        if (resumeAt > 0) java.io.RandomAccessFile(file, "rw").use { it.setLength(item.bytes) }
        var written = if (resumeAt > 0) item.bytes else 0L
        val started = System.currentTimeMillis()
        FileOutputStream(file, resumeAt > 0).use { out ->
            if (resumeAt == 0) media.map?.let { m -> val b = fetchBytes(m); out.write(b); written += b.size }
            val seen = HashSet<Long>()
            var done = resumeAt
            var idx = resumeAt
            while (true) {
                while (idx < media.segs.size) {
                    val seg = media.segs[idx]
                    if (stopReq.remove(id)) return HlsResult(1, written)
                    if (DownloadStore.get(id) == null) return HlsResult(2, written)
                    if (item.limitMin > 0 && System.currentTimeMillis() - started > item.limitMin * 60_000L) return HlsResult(0, written)
                    idx++
                    if (live && !seen.add(seg.seq)) continue
                    val b = segmentBytes(seg)
                    out.write(b)
                    out.flush()
                    written += b.size
                    done++
                    val w = written
                    val d = done
                    val n = media.segs.size
                    DownloadStore.update(ctx, id) {
                        it.copy(bytes = w, segs = if (live) 0 else d, total = if (live || d == 0) 0L else w * n / d)
                    }
                    refreshNotification()
                }
                if (!live || media.endList) break
                Thread.sleep((media.target * 500L).coerceIn(1000L, 5000L))
                if (stopReq.remove(id)) return HlsResult(1, written)
                if (DownloadStore.get(id) == null) return HlsResult(2, written)
                if (item.limitMin > 0 && System.currentTimeMillis() - started > item.limitMin * 60_000L) break
                media = parseMedia(fetchText(url), url)
                idx = 0
            }
        }
        return HlsResult(0, written)
    }

    companion object {
        const val EXTRA_ID = "id"
        const val ACTION_START = "com.sc.player.DL_START"
        const val ACTION_STOP = "com.sc.player.DL_STOP"
        const val NOTIF_ID = 4100
    }
}
