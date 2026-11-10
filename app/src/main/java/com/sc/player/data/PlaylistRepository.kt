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

    /**
     * ينزّل القائمة مباشرة في ملف (من غير ما تتحمّل كلها في الذاكرة)، ويحلّلها بالسطر،
     * وبعدين يستبدل الملف القديم. يرجّع القنوات.
     */
    private fun download(url: String, id: String): List<Channel> {
        val dest = listFile(id)
        val tmp = File(dest.parentFile, dest.name + ".tmp")
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SCPlayer/0.1 (Android)")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw HttpStatusException(resp.code)
                val body = resp.body ?: throw EmptyPlaylistException()
                body.byteStream().use { input -> tmp.outputStream().use { out -> input.copyTo(out) } }
            }
            val channels = parseFile(tmp)
            if (channels.isEmpty()) {
                // Xtream بيرجّع 200 وجسم فاضي لما اسم المستخدم أو كلمة المرور غلط
                if (url.contains("get.php", ignoreCase = true) && url.contains("username=", ignoreCase = true)) {
                    throw InvalidCredentialsException()
                }
                throw EmptyPlaylistException()
            }
            dest.delete()
            if (!tmp.renameTo(dest)) throw java.io.IOException("could not store playlist")
            ChannelCache.put(id, dest.lastModified(), channels)
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
    suspend fun refresh(id: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val p = playlists().firstOrNull { it.id == id } ?: throw IllegalArgumentException("missing")
            download(p.url, id).size
        }
    }

    fun delete(id: String) {
        listFile(id).delete()
        ChannelCache.remove(id)
        save(playlists().filter { it.id != id })
    }

    suspend fun channels(id: String): List<Channel> = withContext(Dispatchers.IO) {
        val f = listFile(id)
        if (!f.exists()) return@withContext emptyList()
        val stamp = f.lastModified()
        ChannelCache.get(id, stamp)
            ?: parseFile(f).also { ChannelCache.put(id, stamp, it) }
    }
}
