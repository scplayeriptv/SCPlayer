package com.sc.player.ui.theme

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

val Bg = Color(0xFF070A12)
val TextMain = Color(0xFFEEF2FF)
val TextMuted = Color(0xFFA5B0C8)
val Cyan = Color(0xFF67E8F9)
val Violet = Color(0xFFA78BFA)
val Ink = Color(0xFF0B0F1A)
val Glass = Color(0x12FFFFFF)
val GlassBorder = Color(0x1FFFFFFF)
val BrandBrush = Brush.linearGradient(listOf(Cyan, Violet))

@Composable
fun SCPlayerTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Cyan,
                secondary = Violet,
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
                    Brush.radialGradient(listOf(Cyan.copy(alpha = 0.22f), Color.Transparent), center = bl, radius = r),
                    radius = r, center = bl,
                )
            },
    ) { content() }
}
