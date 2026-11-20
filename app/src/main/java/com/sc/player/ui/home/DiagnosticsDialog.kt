package com.sc.player.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.R
import com.sc.player.data.DiagStep
import com.sc.player.data.Diagnostics
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

/** شاشة تشخيص الاتصال: بتفحص خطوة خطوة وتقولك المشكلة من النت ولا من السيرفر ولا من الاشتراك. */
@Composable
internal fun DiagnosticsDialog(playlistId: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var steps by remember { mutableStateOf(List(Diagnostics.STEPS) { DiagStep(0) }) }
    var verdict by remember { mutableStateOf<Int?>(null) }
    var runKey by remember { mutableIntStateOf(0) }
    val names = listOf(
        R.string.diag_step_network, R.string.diag_step_internet, R.string.diag_step_dns,
        R.string.diag_step_server, R.string.diag_step_account, R.string.diag_step_stream,
    )
    LaunchedEffect(runKey) {
        steps = List(Diagnostics.STEPS) { DiagStep(0) }
        verdict = null
        verdict = Diagnostics.run(ctx, playlistId) { i, s ->
            steps = steps.toMutableList().also { it[i] = s }
        }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.diag_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                names.forEachIndexed { i, res ->
                    val s = steps[i]
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StateDot(s.state)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(res), color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            if (s.detail.isNotBlank()) Text(s.detail, color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
                verdict?.let { v ->
                    val bad = steps.any { it.state == 4 }
                    val warn = steps.any { it.state == 3 }
                    val tint = if (bad) Color(0xFFEF4444) else if (warn) Color(0xFFF59E0B) else Color(0xFF22C55E)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(tint.copy(alpha = 0.18f))
                            .padding(12.dp),
                    ) {
                        Text(stringResource(v), color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { runKey++ }, enabled = verdict != null) { Text(stringResource(R.string.diag_run)) } },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.diag_close)) } },
    )
}

@Composable
private fun StateDot(state: Int) {
    Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
        when (state) {
            1 -> CircularProgressIndicator(color = Cyan, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
            2 -> Dot(Color(0xFF22C55E), "✓")
            3 -> Dot(Color(0xFFF59E0B), "!")
            4 -> Dot(Color(0xFFEF4444), "✕")
            5 -> Dot(Color(0xFF6B7280), "–")
            else -> Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0x33FFFFFF)))
        }
    }
}

@Composable
private fun Dot(c: Color, glyph: String) {
    Box(Modifier.size(24.dp).clip(CircleShape).background(c), contentAlignment = Alignment.Center) {
        Text(glyph, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}
