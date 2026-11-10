package com.sc.player.data.epg

import com.sc.player.data.Channel

class Programme(val start: Long, val stop: Long, val title: String, val desc: String)

class EpgData(
    /** معرّف القناة في XMLTV (lowercase) ← برامجها مرتّبة بالوقت. */
    val programmes: Map<String, List<Programme>>,
    /** اسم القناة بعد التنضيف ← معرّفها في XMLTV (لو القناة من غير tvg-id). */
    val names: Map<String, String>,
)

/** حروف وأرقام بس وبحروف صغيرة، عشان مطابقة الأسماء. */
fun normalizeName(s: String): String = buildString {
    for (c in s) if (c.isLetterOrDigit()) append(c.lowercaseChar())
}

/** دليل البرامج للقائمة المفتوحة حاليًا (في الذاكرة). */
object EpgStore {
    @Volatile private var owner: String? = null
    @Volatile private var data: EpgData? = null

    fun set(playlistId: String, d: EpgData) {
        data = d
        owner = playlistId
    }

    fun isFor(playlistId: String): Boolean = owner == playlistId && data != null

    fun hasData(): Boolean = data != null

    fun forChannel(ch: Channel): List<Programme> {
        val d = data ?: return emptyList()
        val byId = ch.tvgId?.lowercase()?.let { d.programmes[it] }
        if (byId != null) return byId
        val id = d.names[normalizeName(ch.name)] ?: return emptyList()
        return d.programmes[id].orEmpty()
    }

    fun now(ch: Channel, t: Long): Programme? = forChannel(ch).firstOrNull { t >= it.start && t < it.stop }

    fun next(ch: Channel, t: Long): Programme? = forChannel(ch).firstOrNull { it.start >= t }
}
