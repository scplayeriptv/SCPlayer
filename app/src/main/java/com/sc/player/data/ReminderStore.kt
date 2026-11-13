package com.sc.player.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sc.player.service.ReminderReceiver

/** تذكير ببرنامج: بيتبعت إشعار قبل ما يبدأ بدقيقتين. */
data class Reminder(val channelUrl: String, val channelName: String, val title: String, val start: Long) {
    val key: String get() = "$channelUrl@$start"
}

class ReminderStore(context: Context) {
    private val app = context.applicationContext
    private val sp = app.getSharedPreferences("sc_reminders", Context.MODE_PRIVATE)

    fun list(): List<Reminder> {
        val now = System.currentTimeMillis()
        return (sp.getString("items", "") ?: "").split('\n').mapNotNull { line ->
            val p = line.split('\t')
            val start = p.getOrNull(0)?.toLongOrNull()
            if (p.size < 4 || start == null || start < now - GRACE_MS) null else Reminder(p[1], p[2], p[3], start)
        }.sortedBy { it.start }
    }

    fun has(url: String, start: Long): Boolean = list().any { it.channelUrl == url && it.start == start }

    fun add(r: Reminder) {
        save(list().filter { it.key != r.key } + r)
        schedule(app, r)
    }

    fun remove(r: Reminder) {
        save(list().filter { it.key != r.key })
        cancel(app, r)
    }

    /** بعد إعادة تشغيل الجهاز الـ alarms بتضيع، فنرجّعها. */
    fun rescheduleAll() = list().forEach { schedule(app, it) }

    private fun save(items: List<Reminder>) {
        sp.edit().putString(
            "items",
            items.joinToString("\n") { "${it.start}\t${clean(it.channelUrl)}\t${clean(it.channelName)}\t${clean(it.title)}" },
        ).apply()
    }

    private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ')

    companion object {
        const val LEAD_MS = 2 * 60_000L
        private const val GRACE_MS = 10 * 60_000L

        private fun intent(ctx: Context, r: Reminder) = Intent(ctx, ReminderReceiver::class.java)
            .setAction("com.sc.player.REMINDER")
            .putExtra("title", r.title)
            .putExtra("channel", r.channelName)
            .putExtra("key", r.key)

        private fun pending(ctx: Context, r: Reminder): PendingIntent =
            PendingIntent.getBroadcast(
                ctx, r.key.hashCode(), intent(ctx, r),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        fun schedule(ctx: Context, r: Reminder) {
            val am = ctx.getSystemService(AlarmManager::class.java) ?: return
            val at = (r.start - LEAD_MS).coerceAtLeast(System.currentTimeMillis() + 1_000)
            // inexact عشان مفيش إذن SCHEDULE_EXACT_ALARM؛ ممكن يتأخر دقايق قليلة
            runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(ctx, r)) }
        }

        fun cancel(ctx: Context, r: Reminder) {
            val am = ctx.getSystemService(AlarmManager::class.java) ?: return
            runCatching { am.cancel(pending(ctx, r)) }
        }
    }
}
