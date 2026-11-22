package com.sc.player.ui.downloads

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sc.player.R
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.Panel
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import com.sc.player.ui.theme.Violet

/** بطاقة اختيار واحدة (بتدعم الريموت: حد مضيء عند الفوكس). */
@Composable
internal fun ChoiceCard(
    title: String,
    sub: String?,
    highlight: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val bg = if (highlight) Modifier.background(Brush.horizontalGradient(listOf(Violet, Cyan.copy(alpha = 0.85f))), shape)
    else Modifier.background(Color(0x1AFFFFFF), shape)
    Box(
        modifier
            .clip(shape)
            .then(bg)
            .border(
                if (focused) BorderStroke(2.dp, Color.White) else BorderStroke(1.dp, if (highlight) Color.Transparent else GlassBorder),
                shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (sub != null) Text(sub, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun SheetDialog(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(16.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth(0.92f)
                .heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp - 56).dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Panel)
                .border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(title, color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            content()
            Spacer(Modifier.height(12.dp))
            ChoiceCard(stringResource(R.string.cancel), null, false, Modifier.fillMaxWidth(), onClose)
        }
    }
}

/** اختيار جودة التحميل: 0 = الأعلى، غير كده أقصى ارتفاع (720/480/360)، 1 = الأصغر. */
@Composable
fun QualityDialog(onPick: (Int) -> Unit, onClose: () -> Unit) {
    if (!com.sc.player.data.Premium.active) { ActivationDialog(onClose); return }
    SheetDialog(stringResource(R.string.dl_quality_title), onClose) {
        ChoiceCard(stringResource(R.string.dl_q_best), "MAX", true, Modifier.fillMaxWidth()) { onPick(0) }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChoiceCard("720p", "HD", false, Modifier.weight(1f)) { onPick(720) }
            ChoiceCard("480p", "SD", false, Modifier.weight(1f)) { onPick(480) }
            ChoiceCard("360p", "LOW", false, Modifier.weight(1f)) { onPick(360) }
        }
        Spacer(Modifier.height(10.dp))
        ChoiceCard(stringResource(R.string.dl_q_small), "MIN", false, Modifier.fillMaxWidth()) { onPick(1) }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.dl_q_hint), color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/** اختيار مدة تسجيل البث الحالي (بالدقايق؛ 0 = لحد ما توقفه). */
@Composable
fun RecordDialog(onPick: (Int) -> Unit, onClose: () -> Unit) {
    if (!com.sc.player.data.Premium.active) { ActivationDialog(onClose); return }
    SheetDialog(stringResource(R.string.dl_rec_title), onClose) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChoiceCard(stringResource(R.string.dl_rec_30), null, false, Modifier.weight(1f)) { onPick(30) }
            ChoiceCard(stringResource(R.string.dl_rec_60), null, false, Modifier.weight(1f)) { onPick(60) }
            ChoiceCard(stringResource(R.string.dl_rec_120), null, false, Modifier.weight(1f)) { onPick(120) }
        }
        Spacer(Modifier.height(10.dp))
        ChoiceCard(stringResource(R.string.dl_rec_open), null, true, Modifier.fillMaxWidth()) { onPick(0) }
    }
}
