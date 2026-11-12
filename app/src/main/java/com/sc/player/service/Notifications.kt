package com.sc.player.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object Notifications {
    const val CH_PLAYBACK = "playback"
    const val CH_UPDATE = "update"
    const val CH_UPDATE_READY = "update_ready"

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CH_PLAYBACK, "Playback", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_UPDATE, "Update download", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_UPDATE_READY, "Update ready", NotificationManager.IMPORTANCE_DEFAULT))
    }
}
