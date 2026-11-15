package com.sc.player.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class HttpStatusException(val code: Int) : Exception("HTTP $code")
class EmptyPlaylistException : Exception("no channels")
class InvalidCredentialsException : Exception("bad credentials")

/** Builds the standard Xtream Codes M3U URL from server + credentials. */
fun xtreamUrl(server: String, user: String, pass: String): String {
    var s = server.trim().trimEnd('/')
    if (!s.startsWith("http://", true) && !s.startsWith("https://", true)) s = "http://$s"
    return "$s/get.php?username=${Uri.encode(user.trim())}&password=${Uri.encode(pass.trim())}&type=m3u_plus&output=ts"
}

class PlaylistRepository(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val metaFile get() = File(context.filesDir, "playlists.json")
    private fun listFile(id: String) = File(context.filesDir, "playlist_$id.m3u")

    fun playlists(): List<Playlist> {
        if (!metaFile.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(metaFile.readText())
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Playlist(o.getString("id"), o.getString("name"), o.getString("url"))
            }
        }.getOrDefault(emptyList())
    }

    private fun save(list: List<Playlist>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("url", it.url))
        }
        metaFile.writeText(arr.toString())
    }

    private fun parseFile(f: File): List<Channel> = f.bufferedReader().useLines { M3uParser.parse(it) }

    // كاش ثنائي بعد الـ parse: فتح التطبيق بيقرا الملف ده بدل ما يحلّل الـ M3U من الأول (والتصنيف محفوظ فيه)
    private fun binFile(id: String) = File(context.filesDir, "playlist_$id.bin")

    private fun readBin(f: File, stamp: Long): List<Channel>? {
        if (!f.exists()) return null
        return runCatching {
            java.io.DataInputStream(f.inputStream().buffered(65536)).use { inp ->
                if (inp.readInt() != BIN_VERSION || inp.readLong() != stamp) return null
                val n = inp.readInt()
                val kinds = Kind.values()
                val out = ArrayList<Channel>(n)
                repeat(n) {
                    val name = inp.readUTF()
                    val url = inp.readUTF()
                    val group = inp.readUTF()
                    val logo = if (inp.readBoolean()) inp.readUTF() else null
                    val tvg = if (inp.readBoolean()) inp.readUTF() else null
                    val kd = inp.readByte().toInt()
                    val c = Channel(name, url, group, logo, tvg)
                    if (kd in kinds.indices) c.kindCache = kinds[kd]
                    out.add(c)
                }
                out as List<Channel>
            }
        }.getOrNull()
    }

    private fun writeBin(f: File, stamp: Long, list: List<Channel>) {
        val tmp = File(f.parentFile, f.name + ".tmp")
        val ok = runCatching {
            java.io.DataOutputStream(tmp.outputStream().buffered(65536)).use { out ->
                out.writeInt(BIN_VERSION)
                out.writeLong(stamp)
                out.writeInt(list.size)
                for (c in list) {
                    out.writeUTF(c.name)
                    out.writeUTF(c.url)
                    out.writeUTF(c.group)
                    out.writeBoolean(c.logo != null)
                    if (c.logo != null) out.writeUTF(c.logo)
                    out.writeBoolean(c.tvgId != null)
                    if (c.tvgId != null) out.writeUTF(c.tvgId)
                    out.writeByte(c.kind().ordinal)
                }
            }
        }.isSuccess
        f.delete()
        if (!ok || !tmp.renameTo(f)) tmp.delete()
    }

    /**
     * ينزّل القائمة مباشرة في ملف (من غير ما تتحمّل كلها في الذاكرة)، ويحلّلها بالسطر،
     * وبعدين يستبدل الملف القديم. يرجّع القنوات.
     */
    private fun download(url: String, id: String): List<Channel> {
        val dest = listFile(id)
        val tmp = File(dest.parentFile, dest.name + ".tmp")
        try {
            var channels: List<Channel> = emptyList()
            var failure: Exception? = null
            try {
                fetchTo(url, tmp)
                channels = parseFile(tmp)
            } catch (e: HttpStatusException) {
                failure = e
            }
            // get.php عادةً مبيرجّعش المسلسلات: نكمّلها من player_api.php (best effort)
            if (channels.isNotEmpty() && isXtreamUrl(url) && channels.none { it.kind() == Kind.Series }) {
                if (runCatching { addSeriesFromApi(url, tmp) }.getOrDefault(false)) {
                    channels = runCatching { parseFile(tmp) }.getOrDefault(channels)
                }
            }
            if (channels.isEmpty()) {
                if (isXtreamUrl(url)) {
                    // get.php مقفول أو بيرجّع 404/فاضي: نجرّب player_api.php (بيفرّق بين باسورد غلط وسيرفر مش Xtream)
                    buildFromApi(url, tmp)
                    channels = parseFile(tmp)
                } else {
                    throw failure ?: EmptyPlaylistException()
                }
            }
            if (channels.isEmpty()) throw EmptyPlaylistException()
            dest.delete()
            if (!tmp.renameTo(dest)) throw java.io.IOException("could not store playlist")
            ChannelCache.put(id, dest.lastModified(), channels)
            writeBin(binFile(id), dest.lastModified(), channels)
            return channels
        } finally {
            tmp.delete()
        }
    }

    /** Downloads, validates and stores a playlist. */
    suspend fun add(name: String, url: String): Result<Playlist> = withContext(Dispatchers.IO) {
        runCatching {
            val playlist = Playlist(UUID.randomUUID().toString(), name, url)
            download(url, playlist.id)
            save(playlists() + playlist)
            playlist
        }
    }

    /** يعيد تحميل قائمة موجودة من رابطها. يرجّع عدد القنوات الجديد. */

    private val userAgents = listOf("SCPlayer/0.1 (Android)", "VLC/3.0.18 LibVLC/3.0.18")

    /** GET بنص كامل؛ لو السيرفر رفض (403/404) بنعيد بـ User-Agent تاني. */
    private fun httpGet(url: String, sink: (okhttp3.ResponseBody) -> Unit) {
        var last: HttpStatusException? = null
        for (ua in userAgents) {
            val request = Request.Builder().url(url).header("User-Agent", ua).build()
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    sink(resp.body ?: throw EmptyPlaylistException())
                    return
                }
                last = HttpStatusException(resp.code)
                if (resp.code != 403 && resp.code != 404) throw last!!
            }
        }
        throw last ?: HttpStatusException(404)
    }

    private fun fetchTo(url: String, tmp: File) {
        httpGet(url) { body -> body.byteStream().use { input -> tmp.outputStream().use { out -> input.copyTo(out) } } }
    }

    private fun apiGet(base: String, user: String, pass: String, action: String?): String {
        val url = "$base/player_api.php?username=${Uri.encode(user)}&password=${Uri.encode(pass)}" +
            (action?.let { "&action=$it" } ?: "")
        var text = ""
        httpGet(url) { text = it.string() }
        return text
    }

    private fun optionalArray(base: String, user: String, pass: String, action: String): JSONArray? =
        runCatching { JSONArray(apiGet(base, user, pass, action)) }.getOrNull()

    /** يضيف عناصر المسلسلات لآخر ملف M3U نازل من get.php. بيرجّع true لو اتضاف حاجة. */
    private fun addSeriesFromApi(url: String, tmp: File): Boolean {
        val u = Uri.parse(url)
        val user = u.getQueryParameter("username") ?: return false
        val pass = u.getQueryParameter("password") ?: return false
        for (base in candidateBases(url)) {
            val series = optionalArray(base, user, pass, "get_series") ?: continue
            if (series.length() == 0) return false
            val cats = optionalArray(base, user, pass, "get_series_categories")
            java.io.FileWriter(tmp, true).buffered().use {
                it.append('\n')
                writeSeriesEntries(it, base, user, pass, cats, series)
            }
            return true
        }
        return false
    }

    /** يبني ملف M3U من player_api.php لو get.php مش شغّال. */
    private fun buildFromApi(url: String, tmp: File) {
        val u = Uri.parse(url)
        val user = u.getQueryParameter("username") ?: throw InvalidCredentialsException()
        val pass = u.getQueryParameter("password") ?: throw InvalidCredentialsException()
        var firstError: Exception? = null
        for (base in candidateBases(url)) {
            try {
                checkAuth(apiGet(base, user, pass, null))
                val liveCats = optionalArray(base, user, pass, "get_live_categories")
                val live = optionalArray(base, user, pass, "get_live_streams")
                val vodCats = optionalArray(base, user, pass, "get_vod_categories")
                val vod = optionalArray(base, user, pass, "get_vod_streams")
                val seriesCats = optionalArray(base, user, pass, "get_series_categories")
                val series = optionalArray(base, user, pass, "get_series")
                tmp.bufferedWriter().use { w ->
                    xtreamToM3u(w, base, user, pass, liveCats, live, vodCats, vod, seriesCats, series)
                }
                return
            } catch (e: InvalidCredentialsException) {
                throw e
            } catch (e: HttpStatusException) {
                if (firstError == null) firstError = e
            } catch (e: java.io.IOException) {
                if (firstError == null) firstError = e
            }
        }
        // مشكلة شبكة حقيقية (DNS/مهلة) نظهرها زي ما هي، وغير كده نقول إن العنوان مش سيرفر Xtream
        val err = firstError
        if (err is java.net.UnknownHostException || err is java.net.SocketTimeoutException) throw err
        throw NotXtreamPanelException()
    }

    suspend fun refresh(id: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val p = playlists().firstOrNull { it.id == id } ?: throw IllegalArgumentException("missing")
            download(p.url, id).size
        }
    }

    fun delete(id: String) {
        listFile(id).delete()
        binFile(id).delete()
        ChannelCache.remove(id)
        save(playlists().filter { it.id != id })
    }

    suspend fun channels(id: String): List<Channel> = withContext(Dispatchers.IO) {
        val f = listFile(id)
        if (!f.exists()) return@withContext emptyList()
        val stamp = f.lastModified()
        ChannelCache.get(id, stamp)
            ?: (readBin(binFile(id), stamp)
                ?: parseFile(f).also { list ->
                    // نسخّن كاش التصنيف هنا (خيط الخلفية) بدل أول رسم للواجهة
                    for (c in list) c.kind()
                    writeBin(binFile(id), stamp, list)
                }
                ).also { ChannelCache.put(id, stamp, it) }
    }

    private companion object {
        const val BIN_VERSION = 1
    }
}
