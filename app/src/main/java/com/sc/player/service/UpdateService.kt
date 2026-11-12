package com.sc.player.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.sc.player.data.update.UpdateManager

/** خدمة أمامية بتخلّي تحميل التحديث يكمّل في الخلفية. */
class UpdateService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifications.ensureChannels(this)
        val n = UpdateManager.progressNotification(this, -1)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(UpdateManager.ID_PROGRESS, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(UpdateManager.ID_PROGRESS, n)
            }
        } catch (e: Exception) {
            stopSelf()
        }
        return START_NOT_STICKY
    }
}
