package com.sc.player.data

import android.content.Context

/** بيحفظ آخر موضع مشاهدة لكل فيلم/حلقة (بالرابط) عشان نكمّل منه. بيحتفظ بآخر 150 عنصر بس. */
class PositionStore(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("sc_positions", Context.MODE_PRIVATE)

    /** الموضع اللي نكمّل منه بالمللي ثانية، أو null لو مفيش أو لو خلّص تقريبًا. */
    fun resumeAt(url: String): Long? {
        val parts = sp.getString(url, null)?.split(',') ?: return null
        val pos = parts.getOrNull(0)?.toLongOrNull() ?: return null
        return pos.takeIf { it >= MIN_RESUME_MS }
    }

    fun save(url: String, positionMs: Long, durationMs: Long) {
        val e = sp.edit()
        if (positionMs < MIN_RESUME_MS || durationMs - positionMs < END_MARGIN_MS) {
            e.remove(url)
        } else {
            e.putString(url, "$positionMs,$durationMs,${System.currentTimeMillis()}")
            val all = sp.all
            if (all.size >= MAX_ITEMS) {
                // نشيل الأقدم
                all.entries
                    .sortedBy { (it.value as? String)?.split(',')?.getOrNull(2)?.toLongOrNull() ?: 0L }
                    .take(all.size - MAX_ITEMS + 20)
                    .forEach { e.remove(it.key) }
            }
        }
        e.apply()
    }

    /** نسبة التقدّم 0..1 لو معروفة. */
    fun progress(url: String): Float? {
        val p = sp.getString(url, null)?.split(',') ?: return null
        val pos = p.getOrNull(0)?.toLongOrNull() ?: return null
        val dur = p.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0 } ?: return null
        return (pos.toFloat() / dur).coerceIn(0f, 1f)
    }

    fun clear() {
        sp.edit().clear().apply()
    }

    private companion object {
        const val MIN_RESUME_MS = 10_000L
        const val END_MARGIN_MS = 30_000L
        const val MAX_ITEMS = 150
    }
}
