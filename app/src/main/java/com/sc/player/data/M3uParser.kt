package com.sc.player.data

/** Parses extended M3U / M3U8 playlists into [Channel]s. Pure Kotlin so it can be unit tested. */
object M3uParser {
    /** Index of the first comma that is not inside quotes (it separates attributes from the title). */
    private fun titleComma(line: String): Int {
        var inQuotes = false
        for (i in line.indices) {
            when (line[i]) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) return i
            }
        }
        return -1
    }

    /** قيمة خاصية key="..." من سطر EXTINF (من غير regex عشان الملفات الكبيرة). */
    private fun attr(line: String, key: String): String? {
        var from = 0
        while (true) {
            val i = line.indexOf(key, from, ignoreCase = true)
            if (i < 0) return null
            val end = i + key.length
            val boundaryOk = i == 0 || !(line[i - 1].isLetterOrDigit() || line[i - 1] == '_' || line[i - 1] == '-')
            if (boundaryOk && end + 1 < line.length && line[end] == '=' && line[end + 1] == '"') {
                val close = line.indexOf('"', end + 2)
                return if (close < 0) null else line.substring(end + 2, close)
            }
            from = end
        }
    }

    fun parse(text: String): List<Channel> = parse(text.lineSequence())

    /** يحلّل بالسطر (مناسب للملفات الكبيرة من غير ما تتحمّل كلها في الذاكرة). */
    fun parse(lines: Sequence<String>): List<Channel> {
        val out = ArrayList<Channel>()
        var name: String? = null
        var group = ""
        var logo: String? = null
        var tvgId: String? = null

        for (raw in lines) {
            val line = raw.trim().removePrefix("\uFEFF")
            when {
                line.isEmpty() -> {}
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val comma = titleComma(line)
                    val title = if (comma >= 0) line.substring(comma + 1).trim() else ""
                    name = title.ifEmpty { attr(line, "tvg-name").orEmpty() }
                    group = attr(line, "group-title").orEmpty()
                    logo = attr(line, "tvg-logo")?.ifEmpty { null }
                    tvgId = attr(line, "tvg-id")?.ifEmpty { null }
                }
                line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                    if (group.isEmpty()) group = line.substringAfter(':').trim()
                }
                line.startsWith("#") -> {}
                else -> {
                    val n = name
                    if (n != null) {
                        out.add(Channel(n.ifEmpty { line }, line, group.ifEmpty { "أخرى" }, logo, tvgId))
                    }
                    name = null
                    group = ""
                    logo = null
                    tvgId = null
                }
            }
        }
        return out
    }
}
