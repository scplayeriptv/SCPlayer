package com.sc.player.ui.downloads

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.sc.player.R
import com.sc.player.ui.theme.TextMuted

/** اختيار جودة التحميل: 0 = الأعلى، غير كده أقصى ارتفاع (720/480/360)، 1 = الأصغر. */
@Composable
fun QualityDialog(onPick: (Int) -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.dl_quality_title)) },
        text = {
            Column {
                listOf(
                    0 to stringResource(R.string.dl_q_best),
                    720 to "720p",
                    480 to "480p",
                    360 to "360p",
                    1 to stringResource(R.string.dl_q_small),
                ).forEach { (h, label) ->
                    TextButton(onClick = { onPick(h) }) { Text(label, fontSize = 16.sp) }
                }
                Text(stringResource(R.string.dl_q_hint), color = TextMuted, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } },
    )
}

/** اختيار مدة تسجيل البث الحالي (بالدقايق؛ 0 = لحد ما توقفه). */
@Composable
fun RecordDialog(onPick: (Int) -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.dl_rec_title)) },
        text = {
            Column {
                listOf(30 to R.string.dl_rec_30, 60 to R.string.dl_rec_60, 120 to R.string.dl_rec_120, 0 to R.string.dl_rec_open)
                    .forEach { (m, label) -> TextButton(onClick = { onPick(m) }) { Text(stringResource(label)) } }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } },
    )
}
