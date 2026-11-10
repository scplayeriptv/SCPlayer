package com.sc.player.data

enum class Kind(val title: String) {
    Live("Live TV"),
    Movies("Movies"),
    Series("Series"),
    Radio("Radio"),
}

private val radioWords = Regex("radio|إذاعة|اذاعة|راديو", RegexOption.IGNORE_CASE)
private val movieWords = Regex("movie|film|vod|cinema|أفلام|افلام|فيلم", RegexOption.IGNORE_CASE)
private val seriesWords = Regex("series|season|مسلسل", RegexOption.IGNORE_CASE)

/**
 * تصنيف العنصر: روابط Xtream فيها /movie/ و /series/، وغير كده بنخمّن من اسم المجموعة.
 * أي حاجة مش مصنّفة بتتعامل كقناة مباشرة.
 */
fun Channel.kind(): Kind {
    val u = url.lowercase()
    return when {
        "/movie/" in u -> Kind.Movies
        "/series/" in u -> Kind.Series
        radioWords.containsMatchIn(group) -> Kind.Radio
        movieWords.containsMatchIn(group) -> Kind.Movies
        seriesWords.containsMatchIn(group) -> Kind.Series
        else -> Kind.Live
    }
}
