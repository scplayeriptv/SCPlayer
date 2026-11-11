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
 * المسلسلات مش داخلة: كل حلقة محتاجة طلب منفصل (get_series_info).
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
}

/** هل رد player_api.php معناه دخول ناجح؟ بيرمي InvalidCredentialsException لو لأ، أو HttpStatusException لو الرد مش لسيرفر Xtream. */
internal fun checkAuth(json: String) {
    val info = runCatching { JSONObject(json).optJSONObject("user_info") }.getOrNull()
        ?: throw HttpStatusException(404)
    if (info.optString("auth") != "1") throw InvalidCredentialsException()
}
