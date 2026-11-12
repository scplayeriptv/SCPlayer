package com.sc.player.ui.player

import com.sc.player.R
import androidx.compose.ui.res.stringResource
import com.sc.player.ui.theme.BackButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
        val hw = com.sc.player.data.UserPrefs(ctx).hardwareDecoding()
        val selector = androidx.media3.exoplayer.mediacodec.MediaCodecSelector { mime, secure, tunneling ->
            val all = androidx.media3.exoplayer.mediacodec.MediaCodecUtil.getDecoderInfos(mime, secure, tunneling)
            if (hw) all else all.sortedBy { if (it.softwareOnly) 0 else 1 }
        }
        val renderers = androidx.media3.exoplayer.DefaultRenderersFactory(ctx)
            .setMediaCodecSelector(selector)
            .setEnableDecoderFallback(true)
        ExoPlayer.Builder(ctx, renderers)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
            .setMediaSourceFactory(DefaultMediaSourceFactory(ctx).setDataSourceFactory(http))
            .build()
    }
    val prefs = remember { com.sc.player.data.UserPrefs(ctx) }
    var aspect by remember { mutableIntStateOf(prefs.aspectMode()) }
    var showOptions by remember { mutableStateOf(false) }
    var tracks by remember { mutableStateOf(androidx.media3.common.Tracks.EMPTY) }
    val subtitleView = remember {
        androidx.media3.ui.SubtitleView(ctx).apply {
            setUserDefaultStyle()
            setUserDefaultTextSize()
        }
    }

    val inPip by PipState.inPip
    DisposableEffect(Unit) {
        PipState.playerActive = true
        PipState.autoEnter = prefs.pip()
        onDispose {
            PipState.playerActive = false
            PipState.inPip.value = false
        }
    }
    LaunchedEffect(ratio) { PipState.ratio = ratio }

    val positions = remember { com.sc.player.data.PositionStore(ctx) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isVod by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }

    // حفظ موضع المشاهدة للأفلام/الحلقات (مش للبث المباشر)
    fun saveNow() {
        val id = player.currentMediaItem?.mediaId?.takeIf { it.isNotEmpty() } ?: return
        val d = player.duration
        if (player.isCurrentMediaItemLive || d == androidx.media3.common.C.TIME_UNSET || d < 60_000L) return
        positions.save(id, player.currentPosition, d)
    }
    DisposableEffect(Unit) { onDispose { saveNow(); player.release() } }
    LaunchedEffect(player) {
        while (true) {
            delay(500)
            val d = player.duration
            duration = if (d == androidx.media3.common.C.TIME_UNSET) 0L else d
            isVod = !player.isCurrentMediaItemLive && duration >= 60_000L
            if (!dragging) position = player.currentPosition
        }
    }
    LaunchedEffect(player) {
        while (true) {
            delay(10_000)
            if (player.isPlaying) saveNow()
        }
    }

    // تشغيل في الخلفية: خدمة أمامية بإشعار (قابل للإيقاف من الإعدادات)
    val bgPlay = remember { com.sc.player.data.UserPrefs(ctx).backgroundPlay() }
    DisposableEffect(player) {
        com.sc.player.service.PlaybackBridge.stop = { player.pause() }
        onDispose {
            com.sc.player.service.PlaybackBridge.stop = null
            com.sc.player.service.PlaybackService.stop(ctx)
        }
    }
    LaunchedEffect(channel.name) {
        if (bgPlay) com.sc.player.service.PlaybackService.start(ctx, channel.name)
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) error = null
            }

            override fun onTracksChanged(t: androidx.media3.common.Tracks) {
                tracks = t
            }

            override fun onCues(cueGroup: androidx.media3.common.text.CueGroup) {
                subtitleView.setCues(cueGroup.cues)
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
                        MediaItem.Builder()
                            .setUri(uri)
                            .setMediaId(player.currentMediaItem?.mediaId ?: uri.toString())
                            .setMimeType(MimeTypes.APPLICATION_M3U8)
                            .build(),
                        player.currentPosition,
                    )
                    player.prepare()
                    player.playWhenReady = true
                } else {
                    error = ctx.getString(R.string.player_error)
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(index) {
        saveNow() // يحفظ موضع العنصر السابق قبل ما نبدّل
        triedHls = false
        error = null
        buffering = true
        position = 0L
        duration = 0L
        isVod = false
        val item = MediaItem.Builder().setUri(channel.url).setMediaId(channel.url).build()
        val resume = positions.resumeAt(channel.url)
        if (resume != null) player.setMediaItem(item, resume) else player.setMediaItem(item)
        player.prepare()
        player.playWhenReady = true
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        saveNow()
        if (!bgPlay) player.pause()
    }

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
    fun seekBy(ms: Long) {
        if (!isVod) return
        player.seekTo((player.currentPosition + ms).coerceIn(0L, duration))
        position = player.currentPosition
        poke()
    }
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
                    Key.Menu -> { showOptions = true; true }
                    Key.MediaPlayPause -> { toggle(); true }
                    Key.DirectionRight, Key.MediaFastForward -> if (isVod) { seekBy(10_000); true } else false
                    Key.DirectionLeft, Key.MediaRewind -> if (isVod) { seekBy(-10_000); true } else false
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
        BoxWithConstraints(Modifier.fillMaxSize().clipToBounds(), contentAlignment = Alignment.Center) {
            val mw = maxWidth
            val mh = maxHeight
            val boxRatio = mw / mh
            // 0 = Fit (الصورة كاملة)، 1 = Stretch (تملأ الشاشة)، 2 = Zoom (تقص الأطراف)
            val (w, h) = when (aspect) {
                1 -> mw to mh
                2 -> if (boxRatio > ratio) mw to mw / ratio else mh * ratio to mh
                else -> if (boxRatio > ratio) mh * ratio to mh else mw to mw / ratio
            }
            AndroidView(
                factory = { c -> SurfaceView(c).also { player.setVideoSurfaceView(it) } },
                modifier = Modifier.requiredSize(w, h),
            )
            AndroidView(factory = { subtitleView }, modifier = Modifier.fillMaxSize())
        }

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
                    PillButton(stringResource(R.string.retry), primary = true) {
                        error = null
                        buffering = true
                        player.prepare()
                        player.playWhenReady = true
                    }
                    PillButton(stringResource(R.string.next_channel)) { next() }
                }
            }
        }

        AnimatedVisibility(visible = controls && !inPip, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    BackButton(onBack)
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
                            Text(stringResource(R.string.now_prefix, it.title), color = Cyan, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        onNext?.let {
                            Text(stringResource(R.string.next_prefix, it.title), color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Text("${index + 1} / ${queue.size}", color = TextMuted, fontSize = 13.sp)
                    if (PipState.supported(ctx)) {
                        RoundButton("🖼", stringResource(R.string.player_pip)) { ctx.findActivity()?.let { PipState.enter(it) } }
                    }
                    RoundButton("⚙", stringResource(R.string.player_options)) { showOptions = true; poke() }
                }
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (isVod) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(clock(position), color = TextMuted, fontSize = 12.sp)
                            Slider(
                                value = position.toFloat().coerceIn(0f, duration.toFloat()),
                                onValueChange = { dragging = true; position = it.toLong(); poke() },
                                onValueChangeFinished = { player.seekTo(position); dragging = false },
                                valueRange = 0f..duration.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Cyan,
                                    activeTrackColor = Cyan,
                                    inactiveTrackColor = GlassBorder,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            Text(clock(duration), color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RoundButton("⏮", stringResource(R.string.previous)) { prev() }
                        if (isVod) RoundButton("⏪", stringResource(R.string.player_back10)) { seekBy(-10_000) }
                        RoundButton(
                            if (playing) "⏸" else "▶",
                            if (playing) stringResource(R.string.pause) else stringResource(R.string.resume),
                            big = true,
                        ) { toggle() }
                        if (isVod) RoundButton("⏩", stringResource(R.string.player_fwd10)) { seekBy(10_000) }
                        RoundButton("⏭", stringResource(R.string.next)) { next() }
                    }
                }
            }
        }
    }

    if (showOptions && !inPip) {
        PlayerOptionsDialog(
            player = player,
            tracks = tracks,
            aspect = aspect,
            onAspect = { aspect = it; prefs.setAspectMode(it) },
            onDismiss = { showOptions = false },
        )
    }
}

private fun clock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
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

/** زر تحكم دائري (السابق / تشغيل-إيقاف / التالي). */
@Composable
private fun RoundButton(glyph: String, description: String, big: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(if (big) 72.dp else 52.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(CircleShape)
            .background(if (big) BrandBrush else Brush.linearGradient(listOf(Glass, Glass)))
            .border(if (focused) 3.dp else 1.dp, if (focused) Cyan else GlassBorder, CircleShape)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = Ink, fontSize = if (big) 30.sp else 22.sp)
    }
}
