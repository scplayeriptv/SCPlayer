package com.sc.player.data.epg

/** تحويل وقت XMLTV (مثلاً "20261110120000 +0200") لـ epoch milliseconds. من غير SimpleDateFormat عشان السرعة. */
object XmltvTime {
    fun parse(s: String): Long? {
        val t = s.trim()
        if (t.length < 14) return null
        fun num(a: Int, b: Int): Int? = t.substring(a, b).toIntOrNull()
        val y = num(0, 4) ?: return null
        val mo = num(4, 6) ?: return null
        val d = num(6, 8) ?: return null
        val h = num(8, 10) ?: return null
        val mi = num(10, 12) ?: return null
        val se = num(12, 14) ?: return null
        var offsetMin = 0
        val rest = t.substring(14).trim()
        if (rest.length >= 5 && (rest[0] == '+' || rest[0] == '-')) {
            val oh = rest.substring(1, 3).toIntOrNull() ?: return null
            val om = rest.substring(3, 5).toIntOrNull() ?: return null
            offsetMin = (oh * 60 + om) * (if (rest[0] == '-') -1 else 1)
        }
        val days = daysFromCivil(y, mo, d)
        val seconds = ((days * 24L + h) * 60L + mi - offsetMin) * 60L + se
        return seconds * 1000L
    }

    private fun daysFromCivil(y0: Int, m: Int, d: Int): Long {
        val y = if (m <= 2) y0 - 1 else y0
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468L
    }
}
