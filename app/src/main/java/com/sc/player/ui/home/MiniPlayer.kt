package com.sc.player.ui.home

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.sc.player.ui.theme.Cyan
import kotlinx.coroutines.delay

/**
 * مشغّل معاينة صغير جوه قائمة Live: بيشغّل القناة المحددة بعد نص ثانية تقريبًا من آخر تغيير،
 * وبيتقفل ويتحرر لما الشاشة تتساب. الضغط عليه بينفّذ onTap (فتح ملء الشاشة).
 */
@OptIn(UnstableApi::class)
@Composable
internal fun MiniPlayer(url: String?, modifier: Modifier = Modifier, onTap: () -> Unit) {
    val ctx = LocalContext.current
    var ratio by remember { mutableFloatStateOf(16f / 9f) }
    var buffering by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val triedHls = remember { booleanArrayOf(false) }
    val retries = remember { intArrayOf(0) }
    val player = remember {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("SCPlayer/0.1 (Android)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)
        ExoPlayer.Builder(ctx)
            .setMediaSourceFactory(DefaultMediaSourceFactory(ctx).setDataSourceFactory(http))
            .build()
    }
    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    ratio = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) failed = false
            }

            override fun onPlayerError(error: PlaybackException) {
                val uri = player.currentMediaItem?.localConfiguration?.uri
                val unknownFormat = error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                    error.cause is androidx.media3.exoplayer.source.UnrecognizedInputFormatException
                if (unknownFormat && !triedHls[0] && uri != null) {
                    // نفس أسلوب المشغّل الكامل: ملف بدون امتداد واضح → جرّب كـ HLS
                    triedHls[0] = true
                    player.setMediaItem(MediaItem.Builder().setUri(uri).setMimeType(MimeTypes.APPLICATION_M3U8).build())
                    player.prepare()
                    player.playWhenReady = true
                } else if (retries[0] < 2 && uri != null) {
                    retries[0]++
                    player.setMediaItem(MediaItem.fromUri(uri))
                    player.prepare()
                    player.playWhenReady = true
                } else {
                    failed = true
                    buffering = false
                }
            }
        }
        player.addListener(l)
        onDispose {
            player.removeListener(l)
            player.release()
        }
    }
    LaunchedEffect(url) {
        failed = false
        triedHls[0] = false
        retries[0] = 0
        if (url == null) {
            player.stop()
            return@LaunchedEffect
        }
        delay(600)
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { if (url != null && !failed) player.play() }

    BoxWithConstraints(
        modifier.background(Color.Black).clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        val boxRatio = maxWidth / maxHeight
        val w = if (boxRatio > ratio) maxHeight * ratio else maxWidth
        val h = if (boxRatio > ratio) maxHeight else maxWidth / ratio
        AndroidView(
            factory = { c -> SurfaceView(c).also { player.setVideoSurfaceView(it) } },
            modifier = Modifier.size(w, h),
        )
        if (buffering) CircularProgressIndicator(color = Cyan, modifier = Modifier.size(28.dp))
        if (failed) Text("⚠", color = Color(0xFFFFB74D), fontSize = 28.sp)
    }
}
