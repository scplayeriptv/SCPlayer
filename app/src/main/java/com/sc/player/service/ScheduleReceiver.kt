package com.sc.player.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sc.player.data.Downloads
import com.sc.player.data.ScheduleStore

/** بيستقبل منبّه التسجيل المجدول ويبدأ تسجيل القناة لحد نهاية البرنامج. */
class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.getStringExtra("url").orEmpty()
        val channel = intent.getStringExtra("channel").orEmpty()
        val title = intent.getStringExtra("title").orEmpty()
        val start = intent.getLongExtra("start", 0L)
        val stop = intent.getLongExtra("stop", 0L)
        val logo = intent.getStringExtra("logo")?.ifBlank { null }
        if (url.isBlank() || stop <= 0L) return
        val endAt = stop + ScheduleStore.END_PAD_MS
        val minutes = Math.ceil((endAt - System.currentTimeMillis()) / 60_000.0).toInt()
        if (minutes >= 1) {
            Downloads.enqueue(context, "$channel - $title", url, logo, kind = 1, limitMin = minutes)
        }
        ScheduleStore(context).removeKey("$url@$start")
    }
}
