package com.sc.player.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.SurfaceView
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException
import com.sc.player.data.PlayQueue
import com.sc.player.data.epg.EpgStore
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.Ink
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import kotlinx.coroutines.delay

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(startIndex: Int, onBack: () -> Unit) {
    val queue = PlayQueue.list
    if (queue.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val ctx = LocalContext.current
    val view = LocalView.current
    var index by rememberSaveable { mutableIntStateOf(startIndex.coerceIn(0, queue.lastIndex)) }
    val channel = queue[index.coerceIn(0, queue.lastIndex)]

    var buffering by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(true) }
    var controls by remember { mutableStateOf(true) }
    var touch by remember { mutableIntStateOf(0) }
    var triedHls by remember { mutableStateOf(false) }
    var ratio by remember { mutableFloatStateOf(16f / 9f) }

    val player = remember {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("SCPlayer/0.1 (Android)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
        ExoPlayer.Builder(ctx)
            .setMediaSourceFactory(DefaultMediaSourceFactory(ctx).setDataSourceFactory(http))
            .build()
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) error = null
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.height > 0 && size.width > 0) {
                    ratio = size.width * size.pixelWidthHeightRatio / size.height
                }
            }

            override fun onPlayerError(e: PlaybackException) {
                val unknownFormat = e.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                    e.cause is UnrecognizedInputFormatException
                val uri = player.currentMediaItem?.localConfiguration?.uri
                if (unknownFormat && !triedHls && uri != null) {
                    triedHls = true
                    player.setMediaItem(
                        MediaItem.Builder().setUri(uri).setMimeType(MimeTypes.APPLICATION_M3U8).build(),
                    )
                    player.prepare()
                    player.playWhenReady = true
                } else {
                    error = "تعذّر تشغيل القناة. راجع الرابط أو الإنترنت."
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(index) {
        triedHls = false
        error = null
        buffering = true
        player.setMediaItem(MediaItem.fromUri(channel.url))
        player.prepare()
        player.playWhenReady = true
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player.pause() }

    // ملء الشاشة + بالعرض + الشاشة تفضل منوّرة
    DisposableEffect(Unit) {
        val activity = ctx.findActivity()
        val window = activity?.window
        val oldOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val bars = window?.let { WindowCompat.getInsetsController(it, view) }
        bars?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        bars?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            bars?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = oldOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // إخفاء الأزرار تلقائيًا بعد 4 ثواني
    LaunchedEffect(controls, playing, touch, index) {
        if (controls && playing && error == null) {
            delay(4000)
            controls = false
        }
    }

    fun poke() { controls = true; touch++ }
    fun next() { index = (index + 1) % queue.size; poke() }
    fun prev() { index = (index - 1 + queue.size) % queue.size; poke() }
    fun toggle() {
        if (player.isPlaying) player.pause() else player.play()
        poke()
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyUp) {
                    false
                } else when (ev.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (controls) toggle() else poke()
                        true
                    }
                    Key.MediaPlayPause -> { toggle(); true }
                    Key.DirectionUp, Key.PageUp -> { next(); true }
                    Key.DirectionDown, Key.PageDown -> { prev(); true }
                    else -> false
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    controls = !controls
                    touch++
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { c -> SurfaceView(c).also { player.setVideoSurfaceView(it) } },
            modifier = Modifier.aspectRatio(ratio),
        )

        if (buffering && error == null) {
            CircularProgressIndicator(color = Cyan)
        }

        error?.let { msg ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(msg, color = TextMain, fontSize = 16.sp, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton("إعادة المحاولة", primary = true) {
                        error = null
                        buffering = true
                        player.prepare()
                        player.playWhenReady = true
                    }
                    PillButton("القناة التالية") { next() }
                }
            }
        }

        AnimatedVisibility(visible = controls, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PillButton("رجوع") { onBack() }
                    Column(Modifier.weight(1f)) {
                        Text(
                            channel.name,
                            color = TextMain,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(channel.group, color = TextMuted, fontSize = 12.sp, maxLines = 1)
                        val nowMs = remember(index) { System.currentTimeMillis() }
                        val onNow = remember(index) { EpgStore.now(channel, nowMs) }
                        val onNext = remember(index) { EpgStore.next(channel, nowMs) }
                        onNow?.let {
                            Text("الآن: ${it.title}", color = Cyan, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        onNext?.let {
                            Text("بعده: ${it.title}", color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Text("${index + 1} / ${queue.size}", color = TextMuted, fontSize = 13.sp)
                }
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PillButton("السابقة") { prev() }
                    PillButton(if (playing) "إيقاف مؤقت" else "تشغيل", primary = true) { toggle() }
                    PillButton("التالية") { next() }
                }
            }
        }
    }
}

@Composable
private fun PillButton(text: String, primary: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (primary) BrandBrush else Brush.linearGradient(listOf(Glass, Glass)))
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (primary) Ink else TextMain,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}
