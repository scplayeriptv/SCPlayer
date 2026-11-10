package com.sc.player.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.SCLogo
import com.sc.player.ui.theme.ScreenBackground
import com.sc.player.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SPLASH_MS = 2800L

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val fade = remember { Animatable(0f) }
    val grow = remember { Animatable(0.8f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { fade.animateTo(1f, tween(900)) }
        launch { grow.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch { progress.animateTo(1f, tween(SPLASH_MS.toInt(), easing = LinearEasing)) }
        delay(SPLASH_MS)
        onFinish()
    }

    ScreenBackground {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.graphicsLayer {
                    alpha = fade.value
                    scaleX = grow.value
                    scaleY = grow.value
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SCLogo(120.dp)
                Spacer(Modifier.height(28.dp))
                Text("Welcome to", color = TextMuted, fontSize = 22.sp, letterSpacing = 2.sp)
                Text(
                    "SC Player",
                    fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = TextStyle(brush = BrandBrush),
                )
            }
            Spacer(Modifier.height(48.dp))
            Box(
                Modifier.width(160.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Glass),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress.value).background(BrandBrush))
            }
        }
    }
}
