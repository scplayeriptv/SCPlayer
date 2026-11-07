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

    /** Downloads, validates and stores a playlist. */
    suspend fun add(name: String, url: String): Result<Playlist> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw HttpStatusException(resp.code)
                val body = resp.body?.string().orEmpty()
                if (M3uParser.parse(body).isEmpty()) throw EmptyPlaylistException()
                val playlist = Playlist(UUID.randomUUID().toString(), name, url)
                listFile(playlist.id).writeText(body)
                save(playlists() + playlist)
                playlist
            }
        }
    }

    suspend fun channels(id: String): List<Channel> = withContext(Dispatchers.IO) {
        val f = listFile(id)
        if (f.exists()) M3uParser.parse(f.readText()) else emptyList()
    }
}
