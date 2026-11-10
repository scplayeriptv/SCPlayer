package com.sc.player.ui.theme

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

val Bg = Color(0xFF080512)
val Panel = Color(0xFF110A24)
val TextMain = Color(0xFFF3EFFF)
val TextMuted = Color(0xFFB4A9D6)
/** لون التمييز (focus / عناصر نشطة): بنفسجي فاتح. */
val Cyan = Color(0xFFC4B5FD)
val Violet = Color(0xFF8B5CF6)
/** لون النص فوق الخلفية البنفسجية (أبيض). */
val Ink = Color(0xFFFFFFFF)
val Glass = Color(0x14FFFFFF)
val GlassBorder = Color(0x26FFFFFF)
val Blue = Color(0xFF2563EB)
val BrandBrush = Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFA855F7)))

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
