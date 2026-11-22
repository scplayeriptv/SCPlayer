package com.sc.player.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sc.player.service.ScheduleReceiver

/** تسجيل مجدول لبرنامج: التطبيق بيبدأ التسجيل لوحده قبل البداية بدقيقة وينتهي بعد النهاية بـ 3 دقايق. */
data class ScheduledRec(val channelUrl: String, val channelName: String, val title: String, val start: Long, val stop: Long, val logo: String?) {
    val key: String get() = "$channelUrl@$start"
}

class ScheduleStore(context: Context) {
    private val app = context.applicationContext
    private val sp = app.getSharedPreferences("sc_schedules", Context.MODE_PRIVATE)

    fun list(): List<ScheduledRec> {
        val now = System.currentTimeMillis()
        return (sp.getString("items", "") ?: "").split('\n').mapNotNull { line ->
            val p = line.split('\t')
            val start = p.getOrNull(0)?.toLongOrNull()
            val stop = p.getOrNull(1)?.toLongOrNull()
            if (p.size < 6 || start == null || stop == null || stop + END_PAD_MS < now) null
            else ScheduledRec(p[2], p[3], p[4], start, stop, p[5].ifBlank { null })
        }.sortedBy { it.start }
    }

    fun has(url: String, start: Long): Boolean = list().any { it.channelUrl == url && it.start == start }

    fun add(r: ScheduledRec) {
        save(list().filter { it.key != r.key } + r)
        schedule(app, r)
    }

    fun remove(r: ScheduledRec) {
        save(list().filter { it.key != r.key })
        cancel(app, r)
    }

    fun removeKey(key: String) = list().firstOrNull { it.key == key }?.let { save(list().filter { x -> x.key != key }) }

    fun rescheduleAll() = list().forEach { schedule(app, it) }

    private fun save(items: List<ScheduledRec>) {
        sp.edit().putString(
            "items",
            items.joinToString("\n") { "${it.start}\t${it.stop}\t${c(it.channelUrl)}\t${c(it.channelName)}\t${c(it.title)}\t${c(it.logo ?: "")}" },
        ).apply()
    }

    private fun c(s: String) = s.replace('\t', ' ').replace('\n', ' ')

    companion object {
        const val LEAD_MS = 60_000L
        const val END_PAD_MS = 3 * 60_000L

        private fun pending(ctx: Context, r: ScheduledRec): PendingIntent =
            PendingIntent.getBroadcast(
                ctx, ("rec" + r.key).hashCode(),
                Intent(ctx, ScheduleReceiver::class.java)
                    .setAction("com.sc.player.SCHEDULED_REC")
                    .putExtra("url", r.channelUrl).putExtra("channel", r.channelName)
                    .putExtra("title", r.title).putExtra("start", r.start)
                    .putExtra("stop", r.stop).putExtra("logo", r.logo.orEmpty()),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        fun schedule(ctx: Context, r: ScheduledRec) {
            val am = ctx.getSystemService(AlarmManager::class.java) ?: return
            val at = (r.start - LEAD_MS).coerceAtLeast(System.currentTimeMillis() + 1_000)
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            runCatching {
                if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(ctx, r))
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(ctx, r))
            }.onFailure {
                runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(ctx, r)) }
            }
        }

        fun cancel(ctx: Context, r: ScheduledRec) {
            val am = ctx.getSystemService(AlarmManager::class.java) ?: return
            runCatching { am.cancel(pending(ctx, r)) }
        }
    }
}
