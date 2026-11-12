package com.sc.player.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.compose.runtime.mutableStateOf

/** حالة صورة داخل صورة (PiP): MainActivity بتحدّثها والمشغّل بيقراها. */
object PipState {
    @Volatile var playerActive = false
    @Volatile var autoEnter = true
    @Volatile var ratio = 16f / 9f
    val inPip = mutableStateOf(false)

    fun supported(ctx: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    fun enter(activity: Activity): Boolean {
        if (!supported(activity)) return false
        return try {
            val r = ratio.coerceIn(0.5f, 2.3f)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational((r * 1000).toInt(), 1000))
                .build()
            activity.enterPictureInPictureMode(params)
        } catch (e: Exception) {
            false
        }
    }
}
