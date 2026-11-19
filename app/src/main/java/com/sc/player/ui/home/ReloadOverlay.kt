package com.sc.player.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.R
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.Ink
import com.sc.player.ui.theme.TextMuted

/** تقدّم تحديث المحتوى: 0 تنزيل القائمة، 1 لايف، 2 أفلام، 3 مسلسلات، 4 دليل البرامج، 5 خلص. */
object ReloadProgress {
    var stage by mutableIntStateOf(0)
    var counts by mutableStateOf(listOf(0, 0, 0))
}

/** شاشة التحديث: كل قسم بيشتغل بدوره (دايرة بتلف)، ولما يخلص بينوّر ويظهر عدده، وبعدين اللي بعده. */
@Composable
internal fun ReloadOverlay() {
    val stage = ReloadProgress.stage
    val counts = ReloadProgress.counts
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE6000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                if (stage == 0) stringResource(R.string.reload_stage_download) else stringResource(R.string.reloading),
                color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            )
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    StageCard(Icons.Filled.LiveTv, stringResource(R.string.nav_live_tv), 1, stage, counts[0])
                    StageCard(Icons.Filled.Movie, stringResource(R.string.nav_movies), 2, stage, counts[1])
                    StageCard(Icons.Filled.VideoLibrary, stringResource(R.string.nav_series), 3, stage, counts[2])
                    StageCard(Icons.Filled.CalendarMonth, stringResource(R.string.reload_guide), 4, stage, -1)
                }
            }
        }
    }
}

@Composable
private fun StageCard(icon: ImageVector, label: String, index: Int, stage: Int, count: Int) {
    val done = stage > index
    val active = stage == index
    val t = rememberInfiniteTransition(label = "reload")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "spin")
    val pulse by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")
    val shape = RoundedCornerShape(18.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(64.dp)
                .clip(shape)
                .background(if (done) BrandBrush else Brush.linearGradient(listOf(Color(0x22FFFFFF), Color(0x0DFFFFFF))))
                .border(if (active) 2.dp else 1.dp, if (done || active) Cyan else GlassBorder, shape)
                .alpha(if (done || active) 1f else 0.45f),
            contentAlignment = Alignment.Center,
        ) {
            if (active) {
                Box(
                    Modifier
                        .size(54.dp)
                        .rotate(spin)
                        .border(3.dp, Brush.sweepGradient(listOf(Color.Transparent, Cyan)), CircleShape),
                )
            }
            Icon(icon, null, tint = Ink, modifier = Modifier.size(28.dp).alpha(if (active) pulse else 1f))
            if (done) {
                Text("✓", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.TopEnd).padding(5.dp))
            }
        }
        Text(label, color = if (done || active) Ink else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(
            if (done && count >= 0) count.toString() else " ",
            color = Cyan, fontSize = 11.sp,
        )
    }
}
