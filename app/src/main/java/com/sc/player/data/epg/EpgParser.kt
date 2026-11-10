package com.sc.player.data.epg

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/** يقرا XMLTV بالتدفّق (streaming) ويحتفظ بالبرامج اللي في نافذة الوقت بس. */
object EpgParser {
    fun parse(input: InputStream, fromMs: Long, toMs: Long): EpgData {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        val progs = HashMap<String, MutableList<Programme>>()
        val names = HashMap<String, String>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "channel" -> readChannel(parser, names)
                    "programme" -> readProgramme(parser, progs, fromMs, toMs)
                }
            }
            event = parser.next()
        }
        val sorted = HashMap<String, List<Programme>>(progs.size)
        for ((k, v) in progs) sorted[k] = v.sortedBy { it.start }
        return EpgData(sorted, names)
    }

    private fun readChannel(p: XmlPullParser, names: HashMap<String, String>) {
        val id = p.getAttributeValue(null, "id")?.lowercase() ?: return
        while (true) {
            val ev = p.next()
            if (ev == XmlPullParser.END_DOCUMENT) return
            if (ev == XmlPullParser.END_TAG && p.name == "channel") return
            if (ev == XmlPullParser.START_TAG && p.name == "display-name") {
                val key = normalizeName(p.nextText())
                if (key.isNotEmpty() && key !in names) names[key] = id
            }
        }
    }

    private fun readProgramme(p: XmlPullParser, out: HashMap<String, MutableList<Programme>>, from: Long, to: Long) {
        val start = XmltvTime.parse(p.getAttributeValue(null, "start").orEmpty())
        val stop = XmltvTime.parse(p.getAttributeValue(null, "stop").orEmpty())
        val channel = p.getAttributeValue(null, "channel")?.lowercase()
        var title = ""
        var desc = ""
        while (true) {
            val ev = p.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.END_TAG && p.name == "programme") break
            if (ev == XmlPullParser.START_TAG) {
                when (p.name) {
                    "title" -> if (title.isEmpty()) title = p.nextText().trim()
                    "desc" -> if (desc.isEmpty()) desc = p.nextText().trim().take(300)
                }
            }
        }
        if (start == null || stop == null || channel == null || title.isEmpty()) return
        if (stop < from || start > to) return
        out.getOrPut(channel) { ArrayList() }.add(Programme(start, stop, title, desc))
    }
}
