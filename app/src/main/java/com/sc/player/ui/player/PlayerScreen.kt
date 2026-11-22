package com.sc.player.ui.player

import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.sc.player.data.kind
import com.sc.player.data.epg.EpgStore
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.Ink
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import kotlinx.coroutines.delay

private const val MAX_RECONNECT = 5

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
    // محرّك mpv الاختياري (من الإعدادات)؛ لو فشل بنكمل بـ ExoPlayer
    var mpvFailed by rememberSaveable { mutableStateOf(false) }
    if (!mpvFailed && com.sc.player.data.UserPrefs(ctx).playerEngine() == 1) {
        MpvPlayerScreen(startIndex, onBack, onFail = { mpvFailed = true })
        return
    }
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
    var showList by remember { mutableStateOf(false) }
    var numberBuffer by remember { mutableStateOf("") }
    var lastIndex by remember { mutableIntStateOf(-1) }
    var attempt by remember { mutableIntStateOf(0) }
    var retryKey by remember { mutableIntStateOf(0) }
    var replay by remember { mutableStateOf<com.sc.player.data.CatchUpItem?>(null) }
    var showCatchUp by remember { mutableStateOf(false) }
    var showRecord by remember { mutableStateOf(false) }
    var showLock by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf("") }

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
        val lc = when (com.sc.player.data.UserPrefs(ctx).bufferMode()) {
            0 -> androidx.media3.exoplayer.DefaultLoadControl.Builder().setBufferDurationsMs(3_000, 12_000, 500, 1_500)
            2 -> androidx.media3.exoplayer.DefaultLoadControl.Builder().setBufferDurationsMs(30_000, 120_000, 2_500, 5_000)
            else -> androidx.media3.exoplayer.DefaultLoadControl.Builder().setBufferDurationsMs(15_000, 50_000, 1_000, 2_500)
        }.setPrioritizeTimeOverSizeThresholds(true).build()
        ExoPlayer.Builder(ctx, renderers)
            .setLoadControl(lc)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
            .setMediaSourceFactory(DefaultMediaSourceFactory(ctx, fastExtractors()).setDataSourceFactory(http))
            .build()
    }
    val prefs = remember { com.sc.player.data.UserPrefs(ctx) }
    val autoReconnect = remember { prefs.autoReconnect() }
    val showStats = remember { prefs.showStats() }
    var aspect by remember { mutableIntStateOf(prefs.aspectMode()) }
    var showOptions by remember { mutableStateOf(false) }
    var zoomToast by remember { mutableIntStateOf(0) }
    val zoomLabels = listOf(
        stringResource(R.string.aspect_fit), stringResource(R.string.aspect_stretch), stringResource(R.string.aspect_zoom),
        "16:9", "4:3", "21:9", "125%", "150%", "175%",
    )
    LaunchedEffect(zoomToast) {
        if (zoomToast > 0) { kotlinx.coroutines.delay(1300); zoomToast = 0 }
    }
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

    var showCast by remember { mutableStateOf(false) }
    val cast = remember { com.sc.player.cast.CastHelper(ctx) }
    val castChannel by androidx.compose.runtime.rememberUpdatedState(channel)
    val castVod by androidx.compose.runtime.rememberUpdatedState(isVod)
    DisposableEffect(cast) {
        fun handoff() {
            val pos = player.currentPosition
            player.pause()
            cast.load(castChannel, if (castVod) pos else 0L)
        }
        cast.onStart = { handoff() }
        cast.onEnd = { player.playWhenReady = true }
        if (cast.active) handoff()
        onDispose {
            cast.onStart = null
            cast.onEnd = null
            cast.release()
        }
    }
    var showSleep by remember { mutableStateOf(false) }
    var sleepAt by remember { mutableStateOf(0L) }
    var sleepLeft by remember { mutableStateOf(0) }
    LaunchedEffect(sleepAt) {
        if (sleepAt <= 0L) {
            sleepLeft = 0
            return@LaunchedEffect
        }
        while (true) {
            val left = sleepAt - android.os.SystemClock.elapsedRealtime()
            if (left <= 0L) {
                player.pause()
                cast.stop()
                onBack()
                break
            }
            sleepLeft = ((left + 59_999L) / 60_000L).toInt()
            delay(minOf(left, 15_000L))
        }
    }

    // حفظ موضع المشاهدة للأفلام/الحلقات (مش للبث المباشر)
    fun saveNow() {
        val id = player.currentMediaItem?.mediaId?.takeIf { it.isNotEmpty() } ?: return
        val d = player.duration
        if (player.isCurrentMediaItemLive || d == androidx.media3.common.C.TIME_UNSET || d < 60_000L) return
        positions.save(id, player.currentPosition, d)
        queue.firstOrNull { it.url == id }?.let { com.sc.player.data.WatchNext.update(ctx, it, player.currentPosition, d) }
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
                // بث انقطع (TS من غير مدة معروفة بينتهي لما الاتصال يقفل) → إعادة اتصال تلقائي
                if (state == Player.STATE_ENDED && autoReconnect && attempt < MAX_RECONNECT &&
                    (player.isCurrentMediaItemLive || player.duration == androidx.media3.common.C.TIME_UNSET)
                ) {
                    attempt++
                    retryKey++
                }
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
                } else if (autoReconnect && attempt < MAX_RECONNECT) {
                    attempt++
                    retryKey++
                } else {
                    error = ctx.getString(R.string.player_error)
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // إعادة اتصال تلقائي مع مهلة بتزيد (2ث، 4ث، ...)
    LaunchedEffect(retryKey) {
        if (retryKey > 0) {
            buffering = true
            delay(attempt * 2000L)
            if (player.isCurrentMediaItemLive || player.duration == androidx.media3.common.C.TIME_UNSET) {
                player.seekToDefaultPosition()
            }
            player.prepare()
            player.playWhenReady = true
        }
    }
    // لو اشتغل 20 ثانية متواصل نصفّر العدّاد
    LaunchedEffect(playing) {
        if (playing) {
            delay(20_000)
            attempt = 0
        }
    }
    // إحصائيات التشغيل (اختياري من الإعدادات)
    LaunchedEffect(player, showStats) {
        if (!showStats) return@LaunchedEffect
        val meter = androidx.media3.exoplayer.upstream.DefaultBandwidthMeter.getSingletonInstance(ctx)
        while (true) {
            val vf = player.videoFormat
            val af = player.audioFormat
            val dropped = player.videoDecoderCounters?.let { it.ensureUpdated(); it.droppedBufferCount } ?: 0
            val ahead = ((player.bufferedPosition - player.currentPosition).coerceAtLeast(0)) / 1000.0
            stats = buildString {
                append("Video: ").append(vf?.let { "${it.width}x${it.height} ${it.sampleMimeType?.substringAfter('/') ?: ""}" } ?: "-")
                vf?.bitrate?.takeIf { it > 0 }?.let { append(" · ${it / 1000} kbps") }
                append("\nAudio: ").append(af?.sampleMimeType?.substringAfter('/') ?: "-")
                append("\nBuffer: ").append("%.1f s".format(ahead))
                append("\nNetwork: ").append("%.1f Mbps".format(meter.bitrateEstimate / 1_000_000.0))
                append("\nDropped frames: ").append(dropped)
            }
            delay(1000)
        }
    }

    LaunchedEffect(index) {
        replay = null
        attempt = 0
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
        if (cast.active) {
            player.playWhenReady = false
            cast.load(channel)
        }
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
    fun switchTo(i: Int) {
        if (i != index) {
            lastIndex = index
            index = i
        }
        poke()
    }
    fun next() = switchTo((index + 1) % queue.size)
    fun prev() = switchTo((index - 1 + queue.size) % queue.size)
    fun playReplay(it: com.sc.player.data.CatchUpItem) {
        val u = it.url ?: return
        replay = it
        showCatchUp = false
        attempt = 0
        error = null
        buffering = true
        position = 0L
        duration = 0L
        isVod = false
        player.setMediaItem(MediaItem.Builder().setUri(u).setMediaId(u).build())
        player.prepare()
        player.playWhenReady = true
    }

    fun backToLive() {
        replay = null
        attempt = 0
        error = null
        buffering = true
        player.setMediaItem(MediaItem.Builder().setUri(channel.url).setMediaId(channel.url).build())
        player.prepare()
        player.playWhenReady = true
    }

    fun recall() { if (lastIndex in queue.indices) switchTo(lastIndex) }
    fun commitNumber() {
        val n = numberBuffer.toIntOrNull()
        numberBuffer = ""
        if (n != null && n in 1..queue.size) switchTo(n - 1)
    }
    // الرقم بيتنفّذ تلقائي بعد ثانية ونص من آخر رقم
    LaunchedEffect(numberBuffer) {
        if (numberBuffer.isNotEmpty()) {
            delay(1500)
            commitNumber()
        }
    }
    androidx.activity.compose.BackHandler(enabled = showList) { showList = false }
    fun seekBy(ms: Long) {
        if (!player.isCurrentMediaItemSeekable) { poke(); return }
        val d = player.duration
        val target = player.currentPosition + ms
        player.seekTo(if (d > 0) target.coerceIn(0L, d) else target.coerceAtLeast(0L))
        position = player.currentPosition
        poke()
    }
    fun toggle() {
        if (player.isPlaying) player.pause() else player.play()
        poke()
    }

    // تخطي مقدمة المسلسل: الزر بيظهر في أول 5 دقايق من الحلقة
    val introSec = remember { prefs.introSkip() }
    var introDone by remember(index) { mutableStateOf(false) }
    val introVisible = introSec > 0 && !introDone && isVod && position in 3_000L..300_000L &&
        channel.kind() == com.sc.player.data.Kind.Series && !cast.active
    fun skipIntro() {
        player.seekTo((player.currentPosition + introSec * 1000L).coerceIn(0L, duration))
        position = player.currentPosition
        introDone = true
        poke()
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(showList) { if (!showList) runCatching { focus.requestFocus() } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { ev ->
                if (showList || ev.type != KeyEventType.KeyUp) {
                    false
                } else when (ev.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (numberBuffer.isNotEmpty()) commitNumber()
                        else if (introVisible && !controls) skipIntro()
                        else if (controls) toggle() else poke()
                        true
                    }
                    Key.Menu -> { showOptions = true; true }
                    Key.MediaPlayPause -> { toggle(); true }
                    Key.DirectionRight, Key.MediaFastForward -> if (isVod) { seekBy(10_000); true } else false
                    Key.DirectionLeft -> if (isVod) { seekBy(-10_000); true } else { showList = true; true }
                    Key.MediaRewind -> if (isVod) { seekBy(-10_000); true } else false
                    Key.LastChannel -> { recall(); true }
                    Key.ChannelUp -> { next(); true }
                    Key.ChannelDown -> { prev(); true }
                    Key.DirectionUp, Key.PageUp -> { next(); true }
                    Key.DirectionDown, Key.PageDown -> { prev(); true }
                    else -> {
                        val d = digitOf(ev.key)
                        if (d != null) {
                            if (numberBuffer.length < 4) numberBuffer += d
                            true
                        } else false
                    }
                }
            }
            .pointerInput(isVod, queue.size) {
                // سحب لفوق/تحت = قناة تالية/سابقة (للبث المباشر بس)
                if (isVod) return@pointerInput
                var acc = 0f
                detectVerticalDragGestures(
                    onDragStart = { acc = 0f },
                    onDragEnd = {
                        if (!showList) {
                            if (acc < -140f) next() else if (acc > 140f) prev()
                        }
                        acc = 0f
                    },
                    onDragCancel = { acc = 0f },
                ) { _, dy -> acc += dy }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    if (showList) {
                        showList = false
                    } else {
                        controls = !controls
                        touch++
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().clipToBounds(), contentAlignment = Alignment.Center) {
            val mw = maxWidth
            val mh = maxHeight
            val boxRatio = mw / mh
            // 0 = Fit، 1 = Stretch، 2 = Zoom (تقص الأطراف)، 3/4/5 = نسبة 16:9 / 4:3 / 21:9، 6/7/8 = تكبير 125% / 150% / 175%
            fun forced(r: Float) = if (boxRatio > r) mh * r to mh else mw to mw / r
            val fit = forced(ratio)
            val (w, h) = when (aspect) {
                1 -> mw to mh
                2 -> if (boxRatio > ratio) mw to mw / ratio else mh * ratio to mh
                3 -> forced(16f / 9f)
                4 -> forced(4f / 3f)
                5 -> forced(21f / 9f)
                6 -> fit.first * 1.25f to fit.second * 1.25f
                7 -> fit.first * 1.5f to fit.second * 1.5f
                8 -> fit.first * 1.75f to fit.second * 1.75f
                else -> fit
            }
            AndroidView(
                factory = { c -> SurfaceView(c).also { player.setVideoSurfaceView(it) } },
                modifier = Modifier.requiredSize(w, h),
            )
            AndroidView(factory = { subtitleView }, modifier = Modifier.fillMaxSize())
        }

        if ((buffering || attempt > 0) && error == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(color = Cyan)
                if (attempt > 0) {
                    Text(stringResource(R.string.player_reconnecting, attempt, MAX_RECONNECT), color = TextMain, fontSize = 13.sp)
                }
            }
        }

        if (showStats && stats.isNotEmpty() && !inPip) {
            Text(
                stats,
                color = TextMain,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 76.dp, end = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x99000000))
                    .padding(8.dp),
            )
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
                        if (replay != null) Text("⏪ " + replay!!.title, color = Cyan, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        else Text(channel.group, color = TextMuted, fontSize = 12.sp, maxLines = 1)
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
                        RoundButton(androidx.compose.material.icons.Icons.Filled.PictureInPictureAlt, stringResource(R.string.player_pip)) { ctx.findActivity()?.let { PipState.enter(it) } }
                    }
                    if (sleepLeft > 0) {
                        Text("⏲ ${sleepLeft}m", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    if (channel.kind() == com.sc.player.data.Kind.Live && com.sc.player.data.parseLiveRef(channel.url) != null) {
                        if (replay != null) RoundButton(androidx.compose.material.icons.Icons.Filled.FiberManualRecord, stringResource(R.string.catchup_live), tint = Color(0xFFEF5350)) { backToLive(); poke() }
                        RoundButton(androidx.compose.material.icons.Icons.Filled.History, stringResource(R.string.catchup_title)) { showCatchUp = true; poke() }
                        RoundButton(if (com.sc.player.data.Premium.active) androidx.compose.material.icons.Icons.Filled.FiberManualRecord else androidx.compose.material.icons.Icons.Filled.Lock, stringResource(R.string.dl_record), tint = if (com.sc.player.data.Premium.active) Color(0xFFEF5350) else Color.White) { showRecord = true; poke() }
                    }
                    RoundButton(androidx.compose.material.icons.Icons.Filled.Timer, stringResource(R.string.sleep_title)) { showSleep = true; poke() }
                    if (cast.supported) {
                        RoundButton(androidx.compose.material.icons.Icons.Filled.Cast, stringResource(R.string.cast_title)) { showCast = true; poke() }
                    }
                    if (queue.size > 1) {
                        RoundButton(androidx.compose.material.icons.Icons.Filled.Menu, stringResource(R.string.player_channels)) { showList = true; poke() }
                    }
                    RoundButton(androidx.compose.material.icons.Icons.Filled.Settings, stringResource(R.string.player_options)) { showOptions = true; poke() }
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
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr,
                        ) {
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
                    }
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr,
                    ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (lastIndex in queue.indices) {
                            RoundButton(androidx.compose.material.icons.Icons.Filled.SwapHoriz, stringResource(R.string.player_last)) { recall() }
                        }
                        RoundButton(androidx.compose.material.icons.Icons.Filled.SkipPrevious, stringResource(R.string.previous)) { prev() }
                        RoundButton(androidx.compose.material.icons.Icons.Filled.Replay10, stringResource(R.string.player_back10)) { seekBy(-10_000) }
                        RoundButton(
                            if (playing) androidx.compose.material.icons.Icons.Filled.Pause else androidx.compose.material.icons.Icons.Filled.PlayArrow,
                            if (playing) stringResource(R.string.pause) else stringResource(R.string.resume),
                            big = true,
                        ) { toggle() }
                        RoundButton(androidx.compose.material.icons.Icons.Filled.Forward10, stringResource(R.string.player_fwd10)) { seekBy(10_000) }
                        RoundButton(androidx.compose.material.icons.Icons.Filled.SkipNext, stringResource(R.string.next)) { next() }
                        ZoomButton(zoomLabels[aspect.coerceIn(0, 8)]) {
                            aspect = (aspect + 1) % 9
                            prefs.setAspectMode(aspect)
                            zoomToast++
                            poke()
                        }
                    }
                    }
                }
            }
        }

        if (zoomToast > 0 && !inPip) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "⛶  " + zoomLabels[aspect.coerceIn(0, 8)],
                    color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xCC000000), RoundedCornerShape(24.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                )
            }
        }

        if (cast.active && !inPip) {
            Column(
                Modifier.background(Color(0xCC000000), RoundedCornerShape(16.dp)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.cast_casting, cast.deviceName), color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton(stringResource(if (cast.paused) R.string.resume else R.string.pause), primary = true) { cast.togglePause() }
                    PillButton(stringResource(R.string.cast_stop)) { cast.stop() }
                }
            }
        }

        if (introVisible && !inPip) {
            Box(Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = 120.dp)) {
                PillButton(stringResource(R.string.player_skip_intro, introSec), primary = true) { skipIntro() }
            }
        }
        if (numberBuffer.isNotEmpty() && !inPip) {
            NumberBadge(numberBuffer, Modifier.align(Alignment.TopEnd).padding(28.dp))
        }
        if (showList && !inPip) {
            ChannelListPanel(
                queue = queue,
                current = index,
                onPick = { switchTo(it); showList = false },
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
    }

    if (showCatchUp && !inPip) {
        CatchUpDialog(
            channel,
            onPlay = { playReplay(it) },
            onSave = {
                val u = it.url
                if (u != null && !com.sc.player.data.Premium.active) {
                    showLock = true
                } else if (u != null) {
                    com.sc.player.data.Downloads.enqueue(ctx, channel.name + " - " + it.title, u, channel.logo, 2)
                    android.widget.Toast.makeText(ctx, R.string.dl_added, android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onClose = { showCatchUp = false },
        )
    }

    if (showRecord && !inPip) {
        com.sc.player.ui.downloads.RecordDialog(
            onPick = { m ->
                com.sc.player.data.Downloads.enqueue(ctx, channel.name, channel.url, channel.logo, 1, m)
                android.widget.Toast.makeText(ctx, R.string.dl_added, android.widget.Toast.LENGTH_SHORT).show()
                showRecord = false
            },
            onClose = { showRecord = false },
        )
    }
    if (showLock && !inPip) com.sc.player.ui.downloads.ActivationDialog { showLock = false }

    if (showSleep && !inPip) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSleep = false },
            title = { Text(stringResource(R.string.sleep_title)) },
            text = {
                Column {
                    listOf(0, 15, 30, 45, 60, 90).forEach { m ->
                        androidx.compose.material3.TextButton(onClick = {
                            sleepAt = if (m == 0) 0L else android.os.SystemClock.elapsedRealtime() + m * 60_000L
                            showSleep = false
                        }) {
                            Text(if (m == 0) stringResource(R.string.sleep_off) else stringResource(R.string.sleep_min, m))
                        }
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { showSleep = false }) { Text(stringResource(R.string.done)) } },
        )
    }

    if (showCast && !inPip) {
        com.sc.player.cast.CastDialog(cast) { showCast = false }
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

private fun digitOf(k: Key): Int? = when (k) {
    Key.Zero, Key.NumPad0 -> 0
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
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
private fun RoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    big: Boolean = false,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(if (focused) 1.08f else 1f, androidx.compose.animation.core.tween(140), label = "btnScale")
    // لون محايد (زجاج معتم + أبيض) بيمشي مع كل الثيمات وكل خلفيات الفيديو ومريح للعين
    val fill = when {
        big && focused -> Color(0x66FFFFFF)
        big -> Color(0x40FFFFFF)
        focused -> Color(0x55FFFFFF)
        else -> Color(0x66000000)
    }
    val line = if (focused) Color.White else if (big) Color(0xB3FFFFFF) else Color(0x40FFFFFF)
    Box(
        Modifier
            .size(if (big) 72.dp else 52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .clip(CircleShape)
            .background(fill)
            .border(if (focused) 3.dp else if (big) 2.dp else 1.dp, line, CircleShape)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(icon, null, tint = tint.copy(alpha = if (tint == Color.White) 0.95f else 1f), modifier = Modifier.size(if (big) 38.dp else 26.dp))
    }
}

@Composable
private fun ZoomButton(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(26.dp)
    val accent = Color.White
    Row(
        Modifier
            .height(52.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) Color(0x55FFFFFF) else Color(0x66000000))
            .border(if (focused) 3.dp else 1.dp, if (focused) Color.White else Color(0x40FFFFFF), shape)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
            val w = size.width
            val l = w * 0.36f
            val st = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.6f * density, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            val m = 1.5f * density
            fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                drawLine(accent, androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Offset(x + dx * l, y), st.width, st.cap)
                drawLine(accent, androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Offset(x, y + dy * l), st.width, st.cap)
            }
            corner(m, m, 1f, 1f); corner(w - m, m, -1f, 1f); corner(m, w - m, 1f, -1f); corner(w - m, w - m, -1f, -1f)
        }
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** TS أسرع في البداية: بيقبل keyframes من غير IDR (شائع في بث IPTV) فبيقلّل الشاشة السودا. */
@OptIn(UnstableApi::class)
internal fun fastExtractors(): androidx.media3.extractor.ExtractorsFactory =
    androidx.media3.extractor.DefaultExtractorsFactory()
        .setTsExtractorFlags(
            androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS,
        )
        .setConstantBitrateSeekingEnabled(true)
