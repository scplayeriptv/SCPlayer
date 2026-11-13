package com.sc.player.data

import com.sc.player.R
import androidx.annotation.StringRes

enum class Kind(@StringRes val titleRes: Int) {
    Live(R.string.nav_live_tv),
    Movies(R.string.nav_movies),
    Series(R.string.nav_series),
    Radio(R.string.nav_radio),
}

private val radioWords = Regex("radio|إذاعة|اذاعة|راديو", RegexOption.IGNORE_CASE)
private val movieWords = Regex("movie|film|vod|cinema|أفلام|افلام|فيلم", RegexOption.IGNORE_CASE)
private val seriesWords = Regex("series|season|مسلسل", RegexOption.IGNORE_CASE)

// رابط بث مباشر بنمط Xtream القديم: http://host:port/user/pass/12345 (من غير /live/ ولا امتداد)
private val bareXtreamLive = Regex("^https?://[^/]+/[^/]+/[^/]+/\\d+$", RegexOption.IGNORE_CASE)

/**
 * تصنيف العنصر: روابط Xtream فيها /movie/ و /series/ هي الأقوى. الراديو بيتعرف من اسم المجموعة.
 * رابط البث المباشر (/live/ أو .ts أو .m3u8 أو user/pass/id) بيفضل Live حتى لو اسم المجموعة فيه "Cinema"
 * أو "Movies". غير كده بنخمّن من اسم المجموعة، وأي حاجة مش مصنّفة بتتعامل كقناة مباشرة.
 */
fun Channel.kind(): Kind {
    val u = url.lowercase()
    val liveUrl = "/live/" in u || u.substringBefore('?').let { it.endsWith(".ts") || it.endsWith(".m3u8") } ||
        bareXtreamLive.matches(url)
    return when {
        "/movie/" in u -> Kind.Movies
        "/series/" in u || "/seriesinfo/" in u -> Kind.Series
        radioWords.containsMatchIn(group) -> Kind.Radio
        liveUrl -> Kind.Live
        movieWords.containsMatchIn(group) -> Kind.Movies
        seriesWords.containsMatchIn(group) -> Kind.Series
        else -> Kind.Live
    }
}
