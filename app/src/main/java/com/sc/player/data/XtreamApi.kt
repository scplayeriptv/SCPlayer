package com.sc.player.data

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

class NotXtreamPanelException : Exception("not an Xtream panel")

/** رابط get.php بتاع Xtream (فيه username و password). */
fun isXtreamUrl(url: String): Boolean =
    url.contains("get.php", ignoreCase = true) && url.contains("username=", ignoreCase = true)

/** السيرفرات اللي نجرّبها: اللي اتكتب، وبعده نفس العنوان بالبروتوكول التاني (http ⇄ https) لو مفيش بورت. */
internal fun candidateBases(url: String): List<String> {
    val u = Uri.parse(url)
    val scheme = u.scheme ?: return emptyList()
    val authority = u.encodedAuthority ?: return emptyList()
    val first = "$scheme://$authority"
    val hasPort = authority.substringAfterLast(']').contains(':')
    if (hasPort) return listOf(first)
    val alt = if (scheme.equals("https", true)) "http://$authority" else "https://$authority"
    return listOf(first, alt)
}

private fun clean(s: String) = s.replace('"', '\'').replace('\n', ' ').replace('\r', ' ').trim()

private fun categoryMap(arr: JSONArray?): Map<String, String> {
    if (arr == null) return emptyMap()
    val m = HashMap<String, String>(arr.length())
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        m[o.optString("category_id")] = o.optString("category_name")
    }
    return m
}

/**
 * يحوّل ردود player_api.php (قنوات مباشرة + أفلام) لنص M3U عادي نقدر نحلّله بنفس الـ parser.
 * المسلسلات بتدخل كعنصر واحد لكل مسلسل برابط .../seriesinfo/user/pass/<id>؛ الحلقات بتتجاب وقت الفتح (get_series_info).
 */
internal fun xtreamToM3u(
    out: Appendable,
    base: String,
    user: String,
    pass: String,
    liveCategories: JSONArray?,
    liveStreams: JSONArray?,
    vodCategories: JSONArray?,
    vodStreams: JSONArray?,
    seriesCategories: JSONArray? = null,
    series: JSONArray? = null,
) {
    val u = Uri.encode(user)
    val p = Uri.encode(pass)
    out.append("#EXTM3U\n")

    fun write(streams: JSONArray?, cats: Map<String, String>, path: String, defaultExt: String?) {
        if (streams == null) return
        for (i in 0 until streams.length()) {
            val o = streams.optJSONObject(i) ?: continue
            val id = o.optString("stream_id")
            val name = clean(o.optString("name"))
            if (id.isEmpty() || name.isEmpty()) continue
            val group = clean(cats[o.optString("category_id")] ?: "")
            val logo = clean(o.optString("stream_icon").takeIf { it != "null" } ?: "")
            val epgId = clean(o.optString("epg_channel_id").takeIf { it != "null" } ?: "")
            val ext = defaultExt?.let { o.optString("container_extension").ifEmpty { it } } ?: "ts"
            out.append("#EXTINF:-1 tvg-id=\"").append(epgId)
                .append("\" tvg-logo=\"").append(logo)
                .append("\" group-title=\"").append(group)
                .append("\",").append(name).append('\n')
                .append(base).append('/').append(path).append('/').append(u).append('/').append(p)
                .append('/').append(id).append('.').append(ext).append('\n')
        }
    }

    write(liveStreams, categoryMap(liveCategories), "live", null)
    write(vodStreams, categoryMap(vodCategories), "movie", "mp4")

    writeSeriesEntries(out, base, user, pass, seriesCategories, series)
}

/** عنصر واحد لكل مسلسل برابط .../seriesinfo/user/pass/<id> (الحلقات بتتجاب وقت الفتح). */
internal fun writeSeriesEntries(
    out: Appendable,
    base: String,
    user: String,
    pass: String,
    seriesCategories: JSONArray?,
    series: JSONArray?,
) {
    if (series == null) return
    val u = Uri.encode(user)
    val p = Uri.encode(pass)
    val cats = categoryMap(seriesCategories)
    for (i in 0 until series.length()) {
        val o = series.optJSONObject(i) ?: continue
        val id = o.optString("series_id")
        val name = clean(o.optString("name"))
        if (id.isEmpty() || name.isEmpty()) continue
        val group = clean(cats[o.optString("category_id")] ?: "")
        val cover = clean(o.optString("cover").takeIf { it != "null" } ?: "")
        out.append("#EXTINF:-1 tvg-logo=\"").append(cover)
            .append("\" group-title=\"").append(group)
            .append("\",").append(name).append('\n')
            .append(base).append("/seriesinfo/").append(u).append('/').append(p)
            .append('/').append(id).append('\n')
    }
}

/** هل رد player_api.php معناه دخول ناجح؟ بيرمي InvalidCredentialsException لو لأ، أو HttpStatusException لو الرد مش لسيرفر Xtream. */
internal fun checkAuth(json: String) {
    val info = runCatching { JSONObject(json).optJSONObject("user_info") }.getOrNull()
        ?: throw HttpStatusException(404)
    if (info.optString("auth") != "1") throw InvalidCredentialsException()
}
