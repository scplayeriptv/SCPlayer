package com.sc.player.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sc.player.MainActivity
import com.sc.player.R

/** جسر بسيط بين الإشعار والمشغّل (زر Stop في الإشعار بيوقّف التشغيل). */
object PlaybackBridge {
    @Volatile
    var stop: (() -> Unit)? = null
}

/** خدمة أمامية بتخلّي العملية شغّالة والصوت مكمّل لما التطبيق يروح للخلفية. */
class PlaybackService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            PlaybackBridge.stop?.invoke()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        Notifications.ensureChannels(this)
        val name = intent?.getStringExtra(EXTRA_NAME).orEmpty()
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, PlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(this, Notifications.CH_PLAYBACK)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(name.ifBlank { getString(R.string.app_name) })
            .setContentText(getString(R.string.notif_playing))
            .setContentIntent(open)
            .addAction(0, getString(R.string.notif_stop), stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(ID, n)
            }
        } catch (e: Exception) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    companion object {
        private const val ID = 2001
        private const val ACTION_STOP = "com.sc.player.STOP_PLAYBACK"
        private const val EXTRA_NAME = "name"

        fun start(ctx: Context, channelName: String) {
            runCatching {
                ContextCompat.startForegroundService(
                    ctx, Intent(ctx, PlaybackService::class.java).putExtra(EXTRA_NAME, channelName),
                )
            }
        }

        fun stop(ctx: Context) {
            runCatching { ctx.stopService(Intent(ctx, PlaybackService::class.java)) }
        }
    }
}
