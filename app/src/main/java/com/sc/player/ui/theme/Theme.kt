package com.sc.player.ui.theme

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** ثيمات التطبيق: الخلفية + لون التمييز. */
enum class AppTheme(
    val id: String,
    val bg: Color,
    val panel: Color,
    val primary: Color,
    val light: Color,
    val grad1: Color,
    val grad2: Color,
    val glow: Color,
    val bgStyle: Int = 0,
) {
    Violet("violet", Color(0xFF080512), Color(0xFF110A24), Color(0xFF8B5CF6), Color(0xFFC4B5FD), Color(0xFF7C3AED), Color(0xFFA855F7), Color(0xFF2563EB)),
    Ocean("ocean", Color(0xFF04101C), Color(0xFF08203A), Color(0xFF0EA5E9), Color(0xFF7DD3FC), Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF6366F1)),
    Emerald("emerald", Color(0xFF04120D), Color(0xFF072A1E), Color(0xFF10B981), Color(0xFF6EE7B7), Color(0xFF059669), Color(0xFF34D399), Color(0xFF0EA5E9)),
    Sunset("sunset", Color(0xFF140A04), Color(0xFF2A1508), Color(0xFFF97316), Color(0xFFFDBA74), Color(0xFFEA580C), Color(0xFFFB923C), Color(0xFFEC4899)),
    Rose("rose", Color(0xFF14040C), Color(0xFF2A0816), Color(0xFFEC4899), Color(0xFFF9A8D4), Color(0xFFDB2777), Color(0xFFF472B6), Color(0xFF8B5CF6)),
    Amoled("amoled", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFF8B5CF6), Color(0xFFC4B5FD), Color(0xFF7C3AED), Color(0xFFA855F7), Color(0xFF1E3A8A)),
    Crimson("crimson", Color(0xFF000000), Color(0xFF0C0C0C), Color(0xFFE11D2E), Color(0xFFFF6B6B), Color(0xFFB91C1C), Color(0xFFEF4444), Color(0xFF7F1D1D)),
    Aurora("aurora", Color(0xFF030B14), Color(0xFF07182A), Color(0xFF2DD4BF), Color(0xFF99F6E4), Color(0xFF0D9488), Color(0xFF8B5CF6), Color(0xFF22D3EE), 1),
    Starry("starry", Color(0xFF05060F), Color(0xFF0B0E22), Color(0xFF6366F1), Color(0xFFA5B4FC), Color(0xFF4F46E5), Color(0xFF818CF8), Color(0xFF312E81), 2),
    Neon("neon", Color(0xFF07040F), Color(0xFF120826), Color(0xFFD946EF), Color(0xFFF0ABFC), Color(0xFFC026D3), Color(0xFF22D3EE), Color(0xFF06B6D4), 3),
    Gold("gold", Color(0xFF0A0804), Color(0xFF1A1408), Color(0xFFF59E0B), Color(0xFFFDE68A), Color(0xFFB45309), Color(0xFFFBBF24), Color(0xFF92400E), 4),
    Forest("forest", Color(0xFF030A06), Color(0xFF0A1C12), Color(0xFF22C55E), Color(0xFFBBF7D0), Color(0xFF15803D), Color(0xFF4ADE80), Color(0xFF065F46), 5),
    Dusk("dusk", Color(0xFF0B0714), Color(0xFF1A1030), Color(0xFFFB7185), Color(0xFFFDA4AF), Color(0xFFE11D48), Color(0xFFF59E0B), Color(0xFF7C3AED), 6);

    companion object {
        fun byId(id: String?): AppTheme = values().firstOrNull { it.id == id } ?: Violet
    }
}

/** الثيم الحالي (state: أي شاشة بتقرأ الألوان بتتحدّث لوحدها لما يتغيّر). */
object ThemeState {
    var current by androidx.compose.runtime.mutableStateOf(AppTheme.Violet)
}

val Bg: Color get() = ThemeState.current.bg
val Panel: Color get() = ThemeState.current.panel
val TextMain = Color(0xFFF3EFFF)
val TextMuted = Color(0xFFB4A9D6)
/** لون التمييز (focus / عناصر نشطة). */
val Cyan: Color get() = ThemeState.current.light
val Violet: Color get() = ThemeState.current.primary
/** لون النص فوق الخلفية البنفسجية (أبيض). */
val Ink = Color(0xFFFFFFFF)
val Glass = Color(0x14FFFFFF)
val GlassBorder = Color(0x26FFFFFF)
val Blue: Color get() = ThemeState.current.glow
val BrandBrush: Brush get() = Brush.linearGradient(listOf(ThemeState.current.grad1, ThemeState.current.grad2))

@Composable
fun SCPlayerTheme(content: @Composable () -> Unit) {
    val rtl = LocalConfiguration.current.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL
    CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Violet,
                secondary = Cyan,
                background = Bg,
                surface = Bg,
                onBackground = TextMain,
                onSurface = TextMain,
            ),
            content = content,
        )
    }
}

@Composable
fun ScreenBackground(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .drawBehind {
                if (ThemeState.current == AppTheme.Crimson) {
                    redWaves()
                    return@drawBehind
                }
                when (ThemeState.current.bgStyle) {
                    1 -> { aurora(); return@drawBehind }
                    2 -> { stars(); return@drawBehind }
                    3 -> { neonGrid(); return@drawBehind }
                    4 -> { goldRays(); return@drawBehind }
                    5 -> { forestHills(); return@drawBehind }
                    6 -> { duskSun(); return@drawBehind }
                }
                val r = size.width * 0.5f
                val tr = Offset(size.width * 0.85f, 0f)
                val bl = Offset(0f, size.height)
                drawCircle(
                    Brush.radialGradient(listOf(Violet.copy(alpha = 0.35f), Color.Transparent), center = tr, radius = r),
                    radius = r, center = tr,
                )
                drawCircle(
                    Brush.radialGradient(listOf(Blue.copy(alpha = 0.25f), Color.Transparent), center = bl, radius = r),
                    radius = r, center = bl,
                )
            },
    ) { content() }
}

/** موجات حمراء سفلية (ثيم Crimson). */
private fun DrawScope.redWaves() {
    val w = size.width
    val h = size.height
    fun wave(y0: Float, amp: Float, a: Float, shift: Float) {
        val path = Path().apply {
            moveTo(0f, h)
            lineTo(0f, y0)
            cubicTo(w * 0.25f, y0 - amp + shift, w * 0.5f, y0 + amp, w * 0.75f, y0 - amp * 0.4f)
            cubicTo(w * 0.85f, y0 - amp * 0.8f, w * 0.95f, y0 - amp * 0.9f, w, y0 - amp * 1.4f)
            lineTo(w, h)
            close()
        }
        drawPath(
            path,
            Brush.verticalGradient(
                listOf(Color(0xFFFF1E2D).copy(alpha = a), Color(0xFF3B0004).copy(alpha = a * 0.5f), Color.Transparent),
                startY = y0 - amp * 1.5f,
                endY = h,
            ),
        )
    }
    wave(h * 0.80f, h * 0.10f, 0.55f, 0f)
    wave(h * 0.88f, h * 0.07f, 0.45f, h * 0.03f)
}

private fun DrawScope.band(y0: Float, amp: Float, thick: Float, c: Color, a: Float) {
    val w = size.width
    val path = Path().apply {
        moveTo(0f, y0)
        cubicTo(w * 0.3f, y0 - amp, w * 0.6f, y0 + amp, w, y0 - amp * 0.5f)
        lineTo(w, y0 - amp * 0.5f + thick)
        cubicTo(w * 0.6f, y0 + amp + thick, w * 0.3f, y0 - amp + thick, 0f, y0 + thick)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(Color.Transparent, c.copy(alpha = a), Color.Transparent), startY = y0 - amp, endY = y0 + thick + amp))
}

/** شفق: شرائط ضوء ملوّنة. */
private fun DrawScope.aurora() {
    val h = size.height
    band(h * 0.15f, h * 0.18f, h * 0.35f, Color(0xFF2DD4BF), 0.35f)
    band(h * 0.35f, h * 0.15f, h * 0.30f, Color(0xFF8B5CF6), 0.30f)
    band(h * 0.60f, h * 0.12f, h * 0.30f, Color(0xFF22D3EE), 0.20f)
}

/** سماء بنجوم (مواضع ثابتة). */
private fun DrawScope.stars() {
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFF4F46E5).copy(alpha = 0.30f), Color.Transparent), center = Offset(size.width * 0.8f, size.height * 0.1f), radius = size.width * 0.5f),
        radius = size.width * 0.5f, center = Offset(size.width * 0.8f, size.height * 0.1f),
    )
    val r = kotlin.random.Random(7)
    repeat(90) {
        val x = r.nextFloat() * size.width
        val y = r.nextFloat() * size.height
        val big = r.nextFloat() < 0.15f
        drawCircle(Color.White.copy(alpha = 0.25f + r.nextFloat() * 0.6f), radius = if (big) 2.6f else 1.3f, center = Offset(x, y))
    }
}

/** شبكة نيون بمنظور في الأسفل. */
private fun DrawScope.neonGrid() {
    val w = size.width
    val h = size.height
    val horizon = h * 0.62f
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFD946EF).copy(alpha = 0.35f), Color.Transparent), center = Offset(w / 2, horizon), radius = w * 0.45f),
        radius = w * 0.45f, center = Offset(w / 2, horizon),
    )
    val line = Color(0xFFD946EF).copy(alpha = 0.35f)
    for (i in 0..8) {
        val t = i / 8f
        val y = horizon + (h - horizon) * t * t
        drawLine(line, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
    }
    for (i in -10..10) {
        drawLine(line, Offset(w / 2, horizon), Offset(w / 2 + i * w * 0.14f, h), strokeWidth = 1.5f)
    }
}

/** أشعة ذهبية قطرية. */
private fun DrawScope.goldRays() {
    val w = size.width
    val h = size.height
    for (i in 0..3) {
        val x = w * (0.1f + i * 0.28f)
        val path = Path().apply {
            moveTo(x, 0f); lineTo(x + w * 0.12f, 0f); lineTo(x - w * 0.25f, h); lineTo(x - w * 0.37f, h); close()
        }
        drawPath(path, Brush.verticalGradient(listOf(Color(0xFFF59E0B).copy(alpha = 0.16f), Color.Transparent)))
    }
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFF59E0B).copy(alpha = 0.25f), Color.Transparent), center = Offset(w * 0.9f, 0f), radius = w * 0.4f),
        radius = w * 0.4f, center = Offset(w * 0.9f, 0f),
    )
}

/** تلال خضراء متدرّجة. */
private fun DrawScope.forestHills() {
    val w = size.width
    val h = size.height
    fun hill(y0: Float, amp: Float, a: Float) {
        val p = Path().apply {
            moveTo(0f, h); lineTo(0f, y0)
            cubicTo(w * 0.3f, y0 - amp, w * 0.55f, y0 + amp, w, y0 - amp * 0.6f)
            lineTo(w, h); close()
        }
        drawPath(p, Brush.verticalGradient(listOf(Color(0xFF22C55E).copy(alpha = a), Color.Transparent), startY = y0 - amp, endY = h))
    }
    hill(h * 0.70f, h * 0.12f, 0.30f)
    hill(h * 0.82f, h * 0.09f, 0.32f)
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFF4ADE80).copy(alpha = 0.18f), Color.Transparent), center = Offset(w * 0.15f, 0f), radius = w * 0.45f),
        radius = w * 0.45f, center = Offset(w * 0.15f, 0f),
    )
}

/** غروب: شمس كبيرة وتدرّج دافئ. */
private fun DrawScope.duskSun() {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF7C3AED).copy(alpha = 0.22f), Color(0xFFE11D48).copy(alpha = 0.28f))))
    val c = Offset(w * 0.5f, h * 0.88f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFF59E0B).copy(alpha = 0.55f), Color(0xFFE11D48).copy(alpha = 0.25f), Color.Transparent), center = c, radius = w * 0.35f), radius = w * 0.35f, center = c)
}
