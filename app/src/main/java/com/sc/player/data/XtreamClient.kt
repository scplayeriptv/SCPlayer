package com.sc.player.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

/** عنصر مسلسل في القائمة (مش حلقة قابلة للتشغيل): بيتحوّل لشاشة المواسم والحلقات. */
fun isSeriesEntry(url: String): Boolean = "/seriesinfo/" in url

internal class SeriesRef(val base: String, val user: String, val pass: String, val id: String)

private val seriesRefRx = Regex("^(.*)/seriesinfo/([^/]+)/([^/]+)/(\\d+)$")

internal fun parseSeriesRef(url: String): SeriesRef? {
    val m = seriesRefRx.find(url) ?: return null
    fun dec(s: String) = runCatching { URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)
    return SeriesRef(m.groupValues[1], dec(m.groupValues[2]), dec(m.groupValues[3]), m.groupValues[4])
}

class Episode(val season: Int, val number: Int, val title: String, val url: String, val image: String?, val duration: String?)

/** بيانات العرض لفيلم أو مسلسل (كلها اختيارية). */
class MediaInfo(
    val plot: String = "",
    val poster: String? = null,
    val backdrop: String? = null,
    val year: String = "",
    val rating: String = "",
    val genre: String = "",
    val cast: String = "",
    val director: String = "",
    val duration: String = "",
    val trailer: String = "",
)

class SeriesDetails(val plot: String, val cover: String?, val seasons: Map<Int, List<Episode>>, val meta: MediaInfo = MediaInfo())

private val vodRefRx = Regex("^(.*)/movie/([^/]+)/([^/]+)/(\\d+)\\.[^/.]*$")

internal fun parseVodRef(url: String): SeriesRef? {
    val m = vodRefRx.find(url) ?: return null
    fun dec(s: String) = runCatching { URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)
    return SeriesRef(m.groupValues[1], dec(m.groupValues[2]), dec(m.groupValues[3]), m.groupValues[4])
}

private fun firstUrl(o: JSONObject, key: String): String? {
    val a = o.optJSONArray(key)
    val v = if (a != null && a.length() > 0) a.optString(0) else o.optString(key)
    return v.takeIf { it.isNotBlank() && it != "null" && it.startsWith("http") }
}

/** يقرأ بلوك "info" من get_vod_info أو get_series_info. */
internal fun parseMediaInfo(info: JSONObject?): MediaInfo {
    if (info == null) return MediaInfo()
    val rating = info.str("rating").ifBlank {
        info.str("rating_5based").toDoubleOrNull()?.let { "%.1f".format(it * 2) }.orEmpty()
    }.let { r -> r.toDoubleOrNull()?.takeIf { it > 0 }?.let { "%.1f".format(it) }.orEmpty() }
    val date = info.str("releasedate").ifBlank { info.str("releaseDate") }.ifBlank { info.str("year") }
    return MediaInfo(
        plot = info.str("plot").ifBlank { info.str("description") },
        poster = firstUrl(info, "movie_image") ?: firstUrl(info, "cover"),
        backdrop = firstUrl(info, "backdrop_path"),
        year = Regex("(19|20)\\d{2}").find(date)?.value.orEmpty(),
        rating = rating,
        genre = info.str("genre"),
        cast = info.str("cast").ifBlank { info.str("actors") },
        director = info.str("director"),
        duration = info.str("duration"),
        trailer = info.str("youtube_trailer"),
    )
}

class AccountInfo(
    val status: String,
    /** وقت الانتهاء بالثواني (unix) أو null = غير محدود. */
    val expires: Long?,
    val trial: Boolean,
    val activeConnections: Int?,
    val maxConnections: Int?,
)

private fun JSONObject.str(key: String): String =
    optString(key).takeIf { it != "null" }.orEmpty()

/** يحوّل رد get_series_info لمواسم وحلقات بروابط تشغيل جاهزة. */
internal fun parseSeriesInfo(json: String, ref: SeriesRef, fallbackCover: String?): SeriesDetails {
    val root = JSONObject(json)
    val info = root.optJSONObject("info")
    val cover = info?.str("cover")?.ifBlank { null } ?: fallbackCover
    val plot = info?.str("plot").orEmpty()
    val u = Uri.encode(ref.user)
    val p = Uri.encode(ref.pass)

    val seasons = sortedMapOf<Int, MutableList<Episode>>()
    fun addAll(arr: JSONArray?, defaultSeason: Int) {
        if (arr == null) return
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            val id = e.str("id")
            if (id.isEmpty()) continue
            val season = e.optInt("season", defaultSeason).takeIf { it > 0 } ?: defaultSeason
            val ext = e.str("container_extension").ifEmpty { "mp4" }
            val ei = e.optJSONObject("info")
            seasons.getOrPut(season) { mutableListOf() }.add(
                Episode(
                    season = season,
                    number = e.optInt("episode_num", i + 1),
                    title = e.str("title").ifBlank { "Episode ${i + 1}" },
                    url = "${ref.base}/series/$u/$p/$id.$ext",
                    image = ei?.str("movie_image")?.ifBlank { null },
                    duration = ei?.str("duration")?.ifBlank { null },
                ),
            )
        }
    }

    when (val eps = root.opt("episodes")) {
        is JSONObject -> eps.keys().forEach { k -> addAll(eps.optJSONArray(k), k.toIntOrNull() ?: 1) }
        is JSONArray -> for (i in 0 until eps.length()) addAll(eps.optJSONArray(i), i + 1)
        else -> Unit
    }
    seasons.values.forEach { l -> l.sortBy { it.number } }
    return SeriesDetails(plot, cover, seasons, parseMediaInfo(info))
}

/** طلبات Xtream اللي بتحصل وقت الاستخدام (تفاصيل مسلسل، بيانات الحساب). */
class XtreamClient(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()
    private val userAgents = listOf("SCPlayer/0.1 (Android)", "VLC/3.0.18 LibVLC/3.0.18")

    private fun get(url: String): String {
        var last: Exception? = null
        for (ua in userAgents) {
            val req = Request.Builder().url(url).header("User-Agent", ua).build()
            try {
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) return resp.body?.string().orEmpty()
                    last = HttpStatusException(resp.code)
                    if (resp.code != 403 && resp.code != 404) throw last!!
                }
            } catch (e: HttpStatusException) {
                last = e
            }
        }
        throw last ?: HttpStatusException(404)
    }

    private fun api(base: String, user: String, pass: String, extra: String = ""): String =
        get("$base/player_api.php?username=${Uri.encode(user)}&password=${Uri.encode(pass)}$extra")

    suspend fun seriesInfo(entry: Channel): Result<SeriesDetails> = withContext(Dispatchers.IO) {
        runCatching {
            val ref = parseSeriesRef(entry.url) ?: throw IllegalArgumentException("not a series entry")
            val json = api(ref.base, ref.user, ref.pass, "&action=get_series_info&series_id=${ref.id}")
            parseSeriesInfo(json, ref, entry.logo)
        }
    }

    suspend fun vodInfo(entry: Channel): Result<MediaInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val ref = parseVodRef(entry.url) ?: throw IllegalArgumentException("not an xtream movie")
            val json = api(ref.base, ref.user, ref.pass, "&action=get_vod_info&vod_id=${ref.id}")
            parseMediaInfo(JSONObject(json).optJSONObject("info"))
        }
    }

    /** بيانات الحساب من player_api.php (للقوائم المضافة بـ Xtream بس). */
    suspend fun accountInfo(playlistUrl: String): Result<AccountInfo> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isXtreamUrl(playlistUrl)) throw NotXtreamPanelException()
            val u = Uri.parse(playlistUrl)
            val user = u.getQueryParameter("username") ?: throw InvalidCredentialsException()
            val pass = u.getQueryParameter("password") ?: throw InvalidCredentialsException()
            var err: Exception? = null
            for (base in candidateBases(playlistUrl)) {
                try {
                    val info = JSONObject(api(base, user, pass)).optJSONObject("user_info")
                        ?: throw HttpStatusException(404)
                    if (info.optString("auth") != "1") throw InvalidCredentialsException()
                    val exp = info.str("exp_date").toLongOrNull()?.takeIf { it > 0 }
                    return@runCatching AccountInfo(
                        status = info.str("status"),
                        expires = exp,
                        trial = info.str("is_trial") == "1",
                        activeConnections = info.str("active_cons").toIntOrNull(),
                        maxConnections = info.str("max_connections").toIntOrNull(),
                    )
                } catch (e: InvalidCredentialsException) {
                    throw e
                } catch (e: Exception) {
                    if (err == null) err = e
                }
            }
            throw err ?: NotXtreamPanelException()
        }
    }
}
