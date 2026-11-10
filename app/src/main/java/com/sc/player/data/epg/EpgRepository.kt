package com.sc.player.data.epg

import android.content.Context
import android.net.Uri
import com.sc.player.data.Playlist
import com.sc.player.data.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * يجيب دليل البرامج (XMLTV) للقائمة: من رابط الـ Xtream (xmltv.php) أو من url-tvg في أول سطر M3U.
 * بيتخزّن مختصر في ملف (epg_<id>.tsv) وبيتحدّث كل 6 ساعات.
 */
class EpgRepository(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun cacheFile(id: String) = File(context.filesDir, "epg_$id.tsv")
    private fun playlistFile(id: String) = File(context.filesDir, "playlist_$id.m3u")

    /** رابط XMLTV للقائمة لو اتعرف. */
    fun epgUrl(p: Playlist): String? {
        UserPrefs(context).epgOverride(p.id)?.let { return it }
        if (p.url.contains("get.php", ignoreCase = true)) {
            val u = Uri.parse(p.url)
            val user = u.getQueryParameter("username")
            val pass = u.getQueryParameter("password")
            val scheme = u.scheme
            val authority = u.encodedAuthority
            if (user != null && pass != null && scheme != null && authority != null) {
                return Uri.Builder()
                    .scheme(scheme)
                    .encodedAuthority(authority)
                    .path("/xmltv.php")
                    .appendQueryParameter("username", user)
                    .appendQueryParameter("password", pass)
                    .build()
                    .toString()
            }
        }
        val f = playlistFile(p.id)
        if (!f.exists()) return null
        val first = f.bufferedReader().use { it.readLine() }.orEmpty()
        val m = Regex("""(?:url-tvg|x-tvg-url)="([^"]+)"""", RegexOption.IGNORE_CASE).find(first) ?: return null
        return m.groupValues[1].split(",").first().trim().ifEmpty { null }
    }

    /** يمسح كاش الدليل (بعد تغيير الرابط) عشان التحميل الجاي يبدأ من جديد. */
    fun clearCache(id: String) {
        cacheFile(id).delete()
    }

    /** يحمّل الدليل (من الكاش لو لسه جديد). يرجّع true لو فيه دليل متاح دلوقتي. */
    suspend fun load(p: Playlist, force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val file = cacheFile(p.id)
            if (!EpgStore.isFor(p.id) && file.exists()) {
                EpgStore.set(p.id, readCache(file))
            }
            val fresh = file.exists() && System.currentTimeMillis() - file.lastModified() < REFRESH_MS
            if (fresh && !force) return@runCatching true

            val url = epgUrl(p) ?: return@runCatching EpgStore.isFor(p.id)
            val now = System.currentTimeMillis()
            val data = download(url, now - 3 * HOUR_MS, now + 36 * HOUR_MS)
            writeCache(file, data)
            EpgStore.set(p.id, data)
            true
        }.getOrElse { EpgStore.isFor(p.id) }
    }

    private fun download(url: String, from: Long, to: Long): EpgData {
        val request = Request.Builder().url(url).header("User-Agent", "SCPlayer/0.1 (Android)").build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code}")
            val raw = BufferedInputStream(resp.body!!.byteStream(), 64 * 1024)
            return EpgParser.parse(maybeGunzip(raw), from, to)
        }
    }

    private fun maybeGunzip(input: BufferedInputStream): InputStream {
        input.mark(2)
        val b0 = input.read()
        val b1 = input.read()
        input.reset()
        return if (b0 == 0x1f && b1 == 0x8b) GZIPInputStream(input, 64 * 1024) else input
    }

    private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')

    private fun writeCache(file: File, data: EpgData) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        try {
            tmp.bufferedWriter().use { w ->
                for ((name, id) in data.names) w.write("C\t${clean(name)}\t${clean(id)}\n")
                for ((id, list) in data.programmes) {
                    for (p in list) {
                        w.write("P\t${clean(id)}\t${p.start}\t${p.stop}\t${clean(p.title)}\t${clean(p.desc)}\n")
                    }
                }
            }
            file.delete()
            tmp.renameTo(file)
        } finally {
            tmp.delete()
        }
    }

    private fun readCache(file: File): EpgData {
        val progs = HashMap<String, MutableList<Programme>>()
        val names = HashMap<String, String>()
        file.bufferedReader().useLines { lines ->
            for (line in lines) {
                val parts = line.split('\t', limit = 6)
                when (parts[0]) {
                    "C" -> if (parts.size >= 3) names[parts[1]] = parts[2]
                    "P" -> if (parts.size >= 5) {
                        val start = parts[2].toLongOrNull() ?: continue
                        val stop = parts[3].toLongOrNull() ?: continue
                        progs.getOrPut(parts[1]) { ArrayList() }
                            .add(Programme(start, stop, parts[4], parts.getOrElse(5) { "" }))
                    }
                }
            }
        }
        val sorted = HashMap<String, List<Programme>>(progs.size)
        for ((k, v) in progs) sorted[k] = v.sortedBy { it.start }
        return EpgData(sorted, names)
    }

    private companion object {
        const val HOUR_MS = 3_600_000L
        const val REFRESH_MS = 6 * HOUR_MS
    }
}
