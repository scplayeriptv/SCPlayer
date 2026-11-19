package com.sc.player.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
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
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sc.player.R
import com.sc.player.data.MpvEngine
import com.sc.player.data.PlayQueue
import com.sc.player.data.PositionStore
import com.sc.player.data.UserPrefs
import com.sc.player.ui.theme.BackButton
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import kotlinx.coroutines.delay

private tailrec fun Context.activityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activityOrNull()
    else -> null
}

private fun fmt(sec: Double): String {
    val s = sec.toLong().coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val r = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, r) else "%02d:%02d".format(m, r)
}

/**
 * مشغّل mpv البديل (اختياري من الإعدادات). لو mpv مش متاح أو فشل في التهيئة بيستدعي onFail
 * وPlayerScreen يكمل بـ ExoPlayer. تحكم أساسي: تشغيل/إيقاف، سابق/تالي، تقديم/تأخير للأفلام.
 */
@Composable
fun MpvPlayerScreen(startIndex: Int, onBack: () -> Unit, onFail: () -> Unit) {
    val queue = PlayQueue.list
    if (queue.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val ctx = LocalContext.current
    val view = LocalView.current
    val positions = remember { PositionStore(ctx) }
    val engine = remember { MpvEngine(ctx) }
    var index by rememberSaveable { mutableIntStateOf(startIndex.coerceIn(0, queue.lastIndex)) }
    val channel = queue[index.coerceIn(0, queue.lastIndex)]

    var started by remember { mutableStateOf<Boolean?>(null) }
    var surfaceReady by remember { mutableStateOf(false) }
    var holder by remember { mutableStateOf<SurfaceHolder?>(null) }
    var controls by remember { mutableStateOf(true) }
    var touch by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var pos by remember { mutableFloatStateOf(0f) }
    var dur by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var loadedAt by remember { mutableStateOf(0L) }
    var attempt by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    val prefs = remember { UserPrefs(ctx) }
    var aspect by remember { mutableIntStateOf(prefs.aspectMode().coerceIn(0, 8)) }
    val isVod = dur >= 60f // البث المباشر بيرجّع مدة صغيرة (نافذة HLS) — مش فيلم

    fun saveNow() {
        if (started != true || dur < 60f) return
        positions.save(channel.url, (pos * 1000).toLong(), (dur * 1000).toLong())
    }

    // ملء الشاشة + بالعرض + الشاشة منوّرة
    DisposableEffect(Unit) {
        val activity = ctx.activityOrNull()
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

    DisposableEffect(Unit) {
        val ok = engine.start(UserPrefs(ctx).hardwareDecoding())
        started = ok
        if (ok) holder?.let { engine.attach(it.surface) }
        onDispose {
            saveNow()
            engine.detach()
            engine.destroy()
        }
    }
    LaunchedEffect(started) {
        if (started == false) {
            Toast.makeText(ctx, ctx.getString(R.string.mpv_unavailable), Toast.LENGTH_LONG).show()
            onFail()
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }

    // تحميل الملف لما mpv والسطح يجهزوا، وعند تغيير القناة
    LaunchedEffect(index, started, surfaceReady) {
        if (started == true && surfaceReady) {
            saveNow()
            error = false
            buffering = true
            pos = 0f
            dur = 0f
            attempt = 0
            val resume = positions.resumeAt(channel.url)
            engine.setString("start", if (resume != null) (resume / 1000).toString() else "0")
            engine.setBool("pause", false)
            engine.load(channel.url)
            loadedAt = System.currentTimeMillis()
        }
    }

    // سحب الحالة كل نص ثانية
    LaunchedEffect(started) {
        if (started != true) return@LaunchedEffect
        while (true) {
            delay(500)
            if (!surfaceReady) continue
            val d = engine.double("duration")
            dur = (d ?: 0.0).toFloat()
            val p = engine.double("time-pos")
            if (p != null && !dragging) pos = p.toFloat()
            paused = engine.bool("pause") ?: false
            buffering = (engine.bool("paused-for-cache") ?: false) || (p == null && !error)
            val idle = engine.bool("idle-active") ?: false
            val sinceLoad = System.currentTimeMillis() - loadedAt
            if (idle && p == null && sinceLoad > 4000 && loadedAt > 0) {
                if (attempt < 3) {
                    attempt++
                    engine.load(channel.url)
                    loadedAt = System.currentTimeMillis()
                } else {
                    error = true
                    buffering = false
                }
            }
            if (engine.bool("eof-reached") == true && dur >= 60f && index < queue.lastIndex) index++
            if (dur > 0f && (System.currentTimeMillis() / 1000) % 15L == 0L) saveNow()
        }
    }

    LaunchedEffect(controls, touch, paused) {
        if (controls && !paused && !error) {
            delay(4000)
            controls = false
        }
    }

    fun applyAspect(mode: Int) {
        if (started != true) return
        engine.setString("keepaspect", "yes")
        engine.setString("panscan", "0")
        engine.setString("video-zoom", "0")
        engine.setString("video-aspect-override", "-1")
        when (mode) {
            1 -> engine.setString("keepaspect", "no")
            2 -> engine.setString("panscan", "1.0")
            3 -> engine.setString("video-aspect-override", "16:9")
            4 -> engine.setString("video-aspect-override", "4:3")
            5 -> engine.setString("video-aspect-override", "21:9")
            6 -> engine.setString("video-zoom", "0.3219")
            7 -> engine.setString("video-zoom", "0.585")
            8 -> engine.setString("video-zoom", "0.807")
        }
    }
    LaunchedEffect(aspect, started, surfaceReady) { if (surfaceReady) applyAspect(aspect) }
    val aspectLabels = listOf(
        stringResource(R.string.aspect_fit), stringResource(R.string.aspect_stretch), stringResource(R.string.aspect_zoom),
        "16:9", "4:3", "21:9", "125%", "150%", "175%",
    )

    fun next() { if (index < queue.lastIndex) index++ }
    fun prev() { if (index > 0) index-- }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                touch++
                when (e.key) {
                    Key.DirectionCenter, Key.Enter, Key.MediaPlayPause, Key.Spacebar -> {
                        if (controls || e.key == Key.MediaPlayPause) {
                            engine.setBool("pause", !paused)
                        } else {
                            controls = true
                        }
                        true
                    }
                    Key.MediaPlay -> { engine.setBool("pause", false); true }
                    Key.MediaPause -> { engine.setBool("pause", true); true }
                    Key.DirectionRight -> { if (isVod) engine.seekRel(10.0) else controls = true; true }
                    Key.DirectionLeft -> { if (isVod) engine.seekRel(-10.0) else controls = true; true }
                    Key.DirectionUp, Key.MediaNext, Key.ChannelUp -> { next(); true }
                    Key.DirectionDown, Key.MediaPrevious, Key.ChannelDown -> { prev(); true }
                    else -> false
                }
            }
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                controls = !controls
                touch++
            },
    ) {
        AndroidView(
            factory = { c ->
                SurfaceView(c).also { sv ->
                    sv.holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(h: SurfaceHolder) {
                            holder = h
                            if (engine.ready) {
                                engine.attach(h.surface)
                                engine.setString("vo", "gpu")
                            }
                            surfaceReady = true
                        }

                        override fun surfaceChanged(h: SurfaceHolder, format: Int, w: Int, hh: Int) {
                            if (engine.ready) engine.setString("android-surface-size", "${w}x$hh")
                        }

                        override fun surfaceDestroyed(h: SurfaceHolder) {
                            surfaceReady = false
                            holder = null
                            if (engine.ready) {
                                engine.setString("vo", "null")
                                engine.detach()
                            }
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (buffering && !error) {
            CircularProgressIndicator(color = Cyan, modifier = Modifier.align(Alignment.Center).size(40.dp))
        }
        if (error) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.player_error), color = TextMain, fontSize = 15.sp)
                MpvPill(stringResource(R.string.engine_exo)) { onFail() }
            }
        }

        if (controls) {
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BackButton(onBack)
                Text(channel.name, color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                MpvPill("⛶ " + aspectLabels[aspect.coerceIn(0, 8)]) {
                    aspect = (aspect + 1) % 9
                    prefs.setAspectMode(aspect)
                    touch++
                }
                MpvPill(stringResource(R.string.engine_exo)) { onFail() }
                Text("mpv", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isVod) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(fmt(pos.toDouble()), color = TextMuted, fontSize = 12.sp)
                        Slider(
                            value = pos.coerceIn(0f, dur),
                            onValueChange = { pos = it; dragging = true; touch++ },
                            onValueChangeFinished = { engine.seekAbs(pos.toDouble()); dragging = false },
                            valueRange = 0f..dur,
                            modifier = Modifier.weight(1f),
                        )
                        Text(fmt(dur.toDouble()), color = TextMuted, fontSize = 12.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    MpvPill("⏮") { prev() }
                    Box(Modifier.size(12.dp))
                    MpvPill("⏪ 10") { engine.seekRel(-10.0); touch++ }
                    Box(Modifier.size(12.dp))
                    MpvPill(if (paused) "▶" else "⏸") { engine.setBool("pause", !paused) }
                    Box(Modifier.size(12.dp))
                    MpvPill("10 ⏩") { engine.seekRel(10.0); touch++ }
                    Box(Modifier.size(12.dp))
                    MpvPill("⏭") { next() }
                }
            }
            }
        }
    }
}

@Composable
private fun MpvPill(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = TextMain,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(CircleShape)
            .background(Glass)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    )
}
