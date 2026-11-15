package com.sc.player.data

import android.content.Context
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/**
 * بيتتبّع أول مرة ظهر فيها كل عنصر (بهاش الرابط) عشان تصنيف "أُضيف حديثًا".
 * ملف ثنائي صغير لكل نوع: (hash:Int, firstSeen:Long). أول تحميل أو تغيير القائمة كلها = baseline (من غير "جديد").
 */
class SeenStore(context: Context) {
    private val dir = context.applicationContext.filesDir

    /** العناصر الجديدة (آخر 14 يوم)، الأحدث الأول؛ ولو مفيش جديد بنرجّع آخر عناصر القائمة. */
    fun recent(kind: Kind, items: List<Channel>): List<Channel> {
        if (items.isEmpty()) return emptyList()
        val file = File(dir, "seen_${kind.name}.bin")
        val now = System.currentTimeMillis()
        val prev = HashMap<Int, Long>()
        val hadFile = file.exists()
        if (hadFile) runCatching {
            DataInputStream(file.inputStream().buffered()).use { inp ->
                val n = inp.readInt()
                repeat(n) { prev[inp.readInt()] = inp.readLong() }
            }
        }
        val fresh = items.count { it.url.hashCode() !in prev }
        val massChange = !hadFile || prev.isEmpty() || fresh > items.size * 0.4
        val next = HashMap<Int, Long>(items.size * 2)
        for (it in items) {
            val h = it.url.hashCode()
            next[h] = prev[h] ?: if (massChange) 0L else now
        }
        runCatching {
            DataOutputStream(file.outputStream().buffered()).use { out ->
                out.writeInt(next.size)
                next.forEach { (h, t) -> out.writeInt(h); out.writeLong(t) }
            }
        }
        val cutoff = now - WINDOW_MS
        val recent = items.filter { (next[it.url.hashCode()] ?: 0L) > cutoff }
            .sortedByDescending { next[it.url.hashCode()] ?: 0L }
            .take(MAX)
        return if (recent.isNotEmpty()) recent else items.takeLast(FALLBACK).reversed()
    }

    private companion object {
        const val WINDOW_MS = 14L * 24 * 3600 * 1000
        const val MAX = 200
        const val FALLBACK = 30
    }
}
