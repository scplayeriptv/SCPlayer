package com.sc.player.data

data class Channel(
    val name: String,
    val url: String,
    val group: String,
    val logo: String?,
    val tvgId: String?,
) {
    // كاش داخلي عشان التصنيف والربط بالدليل ما يتحسبوش من الأول مع كل صف/إعادة رسم (قوايم بعشرات الآلاف)
    @Volatile internal var kindCache: Kind? = null
    @Volatile internal var normName: String? = null
    @Volatile internal var epgOwner: Any? = null
    @Volatile internal var epgList: List<com.sc.player.data.epg.Programme>? = null
}

data class Playlist(
    val id: String,
    val name: String,
    val url: String,
)
