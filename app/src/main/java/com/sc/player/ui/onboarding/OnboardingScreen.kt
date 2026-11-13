package com.sc.player.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.R
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.Ink
import com.sc.player.ui.theme.ScreenBackground
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import kotlinx.coroutines.launch
import kotlin.math.abs

private class Page(val emoji: String, val title: Int, val body: Int)

private val pages = listOf(
    Page("📺", R.string.onb1_title, R.string.onb1_body),
    Page("🎬", R.string.onb2_title, R.string.onb2_body),
    Page("🗓", R.string.onb3_title, R.string.onb3_body),
    Page("✨", R.string.onb4_title, R.string.onb4_body),
)

/** شاشة تعريف بتظهر أول مرة بس: 4 صفحات بحركة بسيطة. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val state = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = state.currentPage == pages.lastIndex

    BackHandler(enabled = state.currentPage > 0) {
        scope.launch { state.animateScrollToPage(state.currentPage - 1) }
    }

    ScreenBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!last) Pill(stringResource(R.string.onb_skip), false, onDone)
            }
            HorizontalPager(state, Modifier.weight(1f).fillMaxWidth()) { i ->
                val p = pages[i]
                val dist = abs((state.currentPage - i) + state.currentPageOffsetFraction).coerceIn(0f, 1f)
                Column(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 1f - dist * 0.8f
                            val s = 1f - dist * 0.15f
                            scaleX = s
                            scaleY = s
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val t = rememberInfiniteTransition(label = "float")
                    val dy by t.animateFloat(
                        0f, -14f,
                        infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                        label = "dy",
                    )
                    Box(
                        Modifier.size(120.dp).graphicsLayer { translationY = dy }.clip(CircleShape).background(Glass)
                            .border(1.dp, GlassBorder, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(p.emoji, fontSize = 56.sp) }
                    Text(
                        stringResource(p.title),
                        color = TextMain,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 28.dp),
                    )
                    Text(
                        stringResource(p.body),
                        color = TextMuted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp).widthIn(max = 460.dp),
                    )
                }
            }
            Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { i ->
                    val on = i == state.currentPage
                    Box(
                        Modifier.size(width = if (on) 22.dp else 8.dp, height = 8.dp).clip(CircleShape)
                            .background(if (on) Cyan else GlassBorder),
                    )
                }
            }
            Pill(
                stringResource(if (last) R.string.onb_start else R.string.onb_next),
                true,
            ) {
                if (last) onDone() else scope.launch { state.animateScrollToPage(state.currentPage + 1) }
            }
        }
    }
}

@Composable
private fun Pill(text: String, primary: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .then(if (primary) Modifier.background(BrandBrush) else Modifier.background(Glass))
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else GlassBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (primary) Ink else TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
