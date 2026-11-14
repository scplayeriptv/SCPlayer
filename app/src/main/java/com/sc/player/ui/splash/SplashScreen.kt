package com.sc.player.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.ui.theme.SCLogo
import com.sc.player.ui.theme.ScreenBackground
import com.sc.player.ui.theme.TextMuted
import com.sc.player.ui.theme.ThemeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val SPLASH_MS = 3200L
private const val WORD = "SC Player"

/** مقدمة التشغيل: اللوجو يظهر بنبضة، وحروف "SC Player" بتتجمّع من أماكن متفرّقة، وبعدها لمعة بتعدّي على الكلمة. */
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val logo = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    val shine = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { glow.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
        launch { logo.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
        launch {
            delay(1850)
            shine.animateTo(1f, tween(900, easing = LinearEasing))
        }
        launch {
            delay(2000)
            tagline.animateTo(1f, tween(700))
        }
        delay(SPLASH_MS)
        onFinish()
    }

    val theme = ThemeState.current
    ScreenBackground {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    // هالة ضوء ورا اللوجو
                    drawCircle(
                        Brush.radialGradient(
                            listOf(theme.primary.copy(alpha = 0.35f * glow.value), Color.Transparent),
                            center = Offset(size.width / 2, size.height * 0.42f),
                            radius = size.minDimension * 0.75f,
                        ),
                        radius = size.minDimension * 0.75f,
                        center = Offset(size.width / 2, size.height * 0.42f),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.graphicsLayer {
                        val s = 0.4f + 0.6f * logo.value
                        scaleX = s
                        scaleY = s
                        alpha = logo.value.coerceIn(0f, 1f)
                    },
                ) { SCLogo(104.dp) }
                Spacer(Modifier.height(22.dp))
                Row(
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val w = size.width
                            val x = -w * 0.3f + (w * 1.6f) * shine.value
                            if (shine.value in 0.001f..0.999f) {
                                drawRect(
                                    Brush.linearGradient(
                                        listOf(Color.Transparent, Color.White.copy(alpha = 0.85f), Color.Transparent),
                                        start = Offset(x - w * 0.12f, 0f),
                                        end = Offset(x + w * 0.12f, size.height * 0.4f),
                                    ),
                                    blendMode = BlendMode.SrcAtop,
                                )
                            }
                        },
                    horizontalArrangement = Arrangement.Center,
                ) {
                    WORD.forEachIndexed { i, ch ->
                        if (ch == ' ') {
                            Spacer(Modifier.width(16.dp))
                        } else {
                            Letter(ch, i, lerp(theme.light, theme.grad2, i / (WORD.length - 1f)))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "IPTV PLAYER",
                    color = TextMuted,
                    fontSize = 13.sp,
                    letterSpacing = 6.sp,
                    modifier = Modifier.graphicsLayer { alpha = tagline.value },
                )
            }
        }
    }
}

@Composable
private fun Letter(ch: Char, index: Int, color: Color) {
    val p = remember { Animatable(0f) }
    val r = remember { Random(index * 97 + 13) }
    val dx = remember { (r.nextFloat() * 2 - 1) * 280f }
    val dy = remember { (r.nextFloat() * 2 - 1) * 200f }
    val rot = remember { (r.nextFloat() * 2 - 1) * 80f }
    val density = LocalDensity.current.density
    LaunchedEffect(Unit) {
        delay(550L + index * 90L)
        p.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }
    Text(
        ch.toString(),
        color = color,
        fontSize = 56.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.graphicsLayer {
            val t = 1f - p.value
            translationX = dx * density * t
            translationY = dy * density * t
            rotationZ = rot * t
            val s = 1f + 1.6f * t
            scaleX = s
            scaleY = s
            alpha = (p.value * 2f).coerceIn(0f, 1f)
        },
    )
}
