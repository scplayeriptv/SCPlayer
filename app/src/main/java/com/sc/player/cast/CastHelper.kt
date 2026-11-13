package com.sc.player.cast

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.framework.CastContext
import com.sc.player.R
import com.sc.player.data.Channel

/** Google Cast من المشغّل (للموبايل بس). لو Play Services مش موجودة، castContext = null والزر مش بيظهر. */
@OptIn(UnstableApi::class)
class CastHelper(ctx: Context) {
    val castContext: CastContext? = try {
        if (ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) null
        else CastContext.getSharedInstance(ctx.applicationContext)
    } catch (_: Throwable) {
        null
    }
    private val player: CastPlayer? = try {
        castContext?.let { CastPlayer(it) }
    } catch (_: Throwable) {
        null
    }

    var active by mutableStateOf(false)
        private set
    var deviceName by mutableStateOf("")
        private set
    var onStart: (() -> Unit)? = null
    var onEnd: (() -> Unit)? = null

    init {
        player?.setSessionAvailabilityListener(object : SessionAvailabilityListener {
            override fun onCastSessionAvailable() {
                active = true
                deviceName = castContext?.sessionManager?.currentCastSession?.castDevice?.friendlyName.orEmpty()
                onStart?.invoke()
            }

            override fun onCastSessionUnavailable() {
                active = false
                onEnd?.invoke()
            }
        })
        if (player?.isCastSessionAvailable == true) {
            active = true
            deviceName = castContext?.sessionManager?.currentCastSession?.castDevice?.friendlyName.orEmpty()
        }
    }

    val supported get() = player != null

    fun load(ch: Channel, positionMs: Long = 0L) {
        val p = player ?: return
        p.setMediaItem(item(ch), positionMs)
        p.prepare()
        p.playWhenReady = true
    }

    var paused by mutableStateOf(false)
        private set

    fun togglePause() {
        val p = player ?: return
        p.playWhenReady = !p.playWhenReady
        paused = !p.playWhenReady
    }

    fun stop() {
        castContext?.sessionManager?.endCurrentSession(true)
    }

    fun release() {
        player?.setSessionAvailabilityListener(null)
        stop()
        player?.release()
    }

    private fun item(ch: Channel): MediaItem {
        val live = "/live/" in ch.url
        // Chromecast مش بيشغّل TS خام → نجرّب HLS لروابط Xtream المباشرة
        val url = if (live && ch.url.endsWith(".ts")) ch.url.removeSuffix(".ts") + ".m3u8" else ch.url
        val path = url.substringBefore('?').lowercase()
        val mime = when {
            path.endsWith(".m3u8") || live -> MimeTypes.APPLICATION_M3U8
            path.endsWith(".mkv") -> "video/x-matroska"
            path.endsWith(".webm") -> MimeTypes.VIDEO_WEBM
            else -> MimeTypes.VIDEO_MP4
        }
        return MediaItem.Builder()
            .setUri(url)
            .setMediaId(ch.url)
            .setMimeType(mime)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(ch.name).build())
            .build()
    }
}

/** قائمة أجهزة Cast المتاحة (واجهة بتاعتنا عشان MediaRouteButton محتاج AppCompat). */
@Composable
fun CastDialog(cast: CastHelper, onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val router = remember { MediaRouter.getInstance(ctx) }
    val selector = cast.castContext?.mergedSelector
    var routes by remember { mutableStateOf(listOf<MediaRouter.RouteInfo>()) }

    DisposableEffect(selector) {
        if (selector == null) return@DisposableEffect onDispose { }
        fun refresh() {
            routes = router.routes.filter { !it.isDefault && it.isEnabled && it.matchesSelector(selector) }
        }
        val cb = object : MediaRouter.Callback() {
            override fun onRouteAdded(r: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
            override fun onRouteRemoved(r: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
            override fun onRouteChanged(r: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
        }
        router.addCallback(selector, cb, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
        refresh()
        onDispose { router.removeCallback(cb) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cast_title)) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (cast.active) {
                    Text(stringResource(R.string.cast_casting, cast.deviceName))
                    TextButton(onClick = { cast.stop(); onDismiss() }) { Text(stringResource(R.string.cast_stop)) }
                } else if (routes.isEmpty()) {
                    Text(stringResource(R.string.cast_none), fontSize = 14.sp)
                } else {
                    routes.forEach { r ->
                        Text(
                            r.name,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { router.selectRoute(r); onDismiss() }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}
