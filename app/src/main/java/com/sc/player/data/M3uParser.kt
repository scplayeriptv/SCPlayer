package com.sc.player.data

/** Parses extended M3U / M3U8 playlists into [Channel]s. Pure Kotlin so it can be unit tested. */
object M3uParser {
    private val attrRegex = Regex("""([\w-]+)="([^"]*)"""")

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

    fun parse(text: String): List<Channel> {
        val out = ArrayList<Channel>()
        var name: String? = null
        var group = ""
        var logo: String? = null
        var tvgId: String? = null

        for (raw in text.lineSequence()) {
            val line = raw.trim().removePrefix("\uFEFF")
            when {
                line.isEmpty() -> {}
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val attrs = attrRegex.findAll(line)
                        .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                    val comma = titleComma(line)
                    val title = if (comma >= 0) line.substring(comma + 1).trim() else ""
                    name = title.ifEmpty { attrs["tvg-name"].orEmpty() }
                    group = attrs["group-title"].orEmpty()
                    logo = attrs["tvg-logo"]?.ifEmpty { null }
                    tvgId = attrs["tvg-id"]?.ifEmpty { null }
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
