package com.sc.player.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sc.player.MainActivity
import com.sc.player.R
import com.sc.player.data.ReminderStore

/** بيستقبل منبّه التذكير ويعرض إشعار "البرنامج هيبدأ". */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title").orEmpty()
        val channel = intent.getStringExtra("channel").orEmpty()
        val key = intent.getStringExtra("key").orEmpty()
        Notifications.ensureChannels(context)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, Notifications.CH_REMINDER)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.notif_reminder_title))
            .setContentText(context.getString(R.string.notif_reminder_text, title, channel))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notif_reminder_text, title, channel)))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(key.hashCode(), n)
        } catch (e: SecurityException) {
            // مفيش إذن إشعارات
        }
    }
}

/** بعد إعادة التشغيل بنرجّع كل التذكيرات المحفوظة. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderStore(context).rescheduleAll()
        }
    }
}
