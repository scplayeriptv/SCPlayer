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
) {
    Violet("violet", Color(0xFF080512), Color(0xFF110A24), Color(0xFF8B5CF6), Color(0xFFC4B5FD), Color(0xFF7C3AED), Color(0xFFA855F7), Color(0xFF2563EB)),
    Ocean("ocean", Color(0xFF04101C), Color(0xFF08203A), Color(0xFF0EA5E9), Color(0xFF7DD3FC), Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF6366F1)),
    Emerald("emerald", Color(0xFF04120D), Color(0xFF072A1E), Color(0xFF10B981), Color(0xFF6EE7B7), Color(0xFF059669), Color(0xFF34D399), Color(0xFF0EA5E9)),
    Sunset("sunset", Color(0xFF140A04), Color(0xFF2A1508), Color(0xFFF97316), Color(0xFFFDBA74), Color(0xFFEA580C), Color(0xFFFB923C), Color(0xFFEC4899)),
    Rose("rose", Color(0xFF14040C), Color(0xFF2A0816), Color(0xFFEC4899), Color(0xFFF9A8D4), Color(0xFFDB2777), Color(0xFFF472B6), Color(0xFF8B5CF6)),
    Amoled("amoled", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFF8B5CF6), Color(0xFFC4B5FD), Color(0xFF7C3AED), Color(0xFFA855F7), Color(0xFF1E3A8A));

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
