package com.sc.player.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.tv.TvContract
import android.os.Build
import com.sc.player.MainActivity

/**
 * صف "Continue Watching" في الشاشة الرئيسية لـ Android TV (Watch Next).
 * بيشتغل على Android 8+ وعلى أجهزة TV بس، وأي فشل بيتتجاهل (best-effort).
 */
object WatchNext {
    const val EXTRA_URL = "wn_url"
    const val EXTRA_NAME = "wn_name"
    const val EXTRA_LOGO = "wn_logo"

    private var lastUrl: String? = null
    private var lastAt = 0L

    private fun supported(ctx: Context) =
        Build.VERSION.SDK_INT >= 26 && ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

    private fun ids(ctx: Context) = ctx.applicationContext.getSharedPreferences("sc_watchnext", Context.MODE_PRIVATE)

    /** يضيف/يحدّث العنصر لو لسه في نص المشاهدة، ويشيله لو خلّص أو لسه في أوله. */
    fun update(ctx: Context, ch: Channel, positionMs: Long, durationMs: Long) {
        if (!supported(ctx)) return
        val unfinished = positionMs >= 10_000L && durationMs - positionMs >= 30_000L
        if (!unfinished) {
            remove(ctx, ch.url)
            return
        }
        val now = System.currentTimeMillis()
        if (lastUrl == ch.url && now - lastAt < 30_000L) return
        lastUrl = ch.url
        lastAt = now
        try {
            val app = ctx.applicationContext
            val intent = Intent(app, MainActivity::class.java)
                .putExtra(EXTRA_URL, ch.url)
                .putExtra(EXTRA_NAME, ch.name)
                .putExtra(EXTRA_LOGO, ch.logo)
            val cv = ContentValues().apply {
                put(TvContract.WatchNextPrograms.COLUMN_TYPE, TvContract.WatchNextPrograms.TYPE_MOVIE)
                put(TvContract.WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE, TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                put(TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS, now)
                put(TvContract.WatchNextPrograms.COLUMN_TITLE, ch.name)
                put(TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS, durationMs.toInt())
                put(TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS, positionMs.toInt())
                put(TvContract.WatchNextPrograms.COLUMN_INTENT_URI, intent.toUri(Intent.URI_INTENT_SCHEME))
                put(TvContract.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID, ch.url.hashCode().toString())
                if (!ch.logo.isNullOrBlank()) put(TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI, ch.logo)
            }
            val key = ch.url.hashCode().toString()
            val existing = ids(app).getLong(key, -1L)
            if (existing >= 0) {
                val n = app.contentResolver.update(ContentUris.withAppendedId(TvContract.WatchNextPrograms.CONTENT_URI, existing), cv, null, null)
                if (n > 0) return
            }
            val uri = app.contentResolver.insert(TvContract.WatchNextPrograms.CONTENT_URI, cv) ?: return
            ids(app).edit().putLong(key, ContentUris.parseId(uri)).apply()
        } catch (_: Exception) {
            // مفيش provider أو مفيش صلاحية — نتجاهل
        }
    }

    fun remove(ctx: Context, url: String) {
        if (!supported(ctx)) return
        try {
            val app = ctx.applicationContext
            val key = url.hashCode().toString()
            val id = ids(app).getLong(key, -1L)
            if (id < 0) return
            app.contentResolver.delete(ContentUris.withAppendedId(TvContract.WatchNextPrograms.CONTENT_URI, id), null, null)
            ids(app).edit().remove(key).apply()
        } catch (_: Exception) {
        }
    }
}
