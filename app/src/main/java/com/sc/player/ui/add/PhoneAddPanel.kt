package com.sc.player.ui.add

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.sc.player.R
import com.sc.player.data.PhoneAddServer
import com.sc.player.data.PhonePayload
import com.sc.player.data.findLocalIp
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

private sealed interface PhoneInfo {
    data class Ready(val url: String) : PhoneInfo
    data object NoWifi : PhoneInfo
    data object Failed : PhoneInfo
}

/** تاب "من الموبايل": QR + عنوان، وبيستقبل القائمة من صفحة الفورم على الموبايل. */
@Composable
internal fun PhoneAddPanel(onPayload: (PhonePayload) -> Unit) {
    val ctx = LocalContext.current
    val latest by rememberUpdatedState(onPayload)
    var info by remember { mutableStateOf<PhoneInfo?>(null) }

    DisposableEffect(Unit) {
        val ip = findLocalIp()
        val token = (1..8).map { "abcdefghjkmnpqrstuvwxyz23456789".random() }.joinToString("")
        val main = Handler(Looper.getMainLooper())
        val srv = PhoneAddServer(ctx.applicationContext, token) { p -> main.post { latest(p) } }
        val port = if (ip != null) srv.start() else null
        info = when {
            ip == null -> PhoneInfo.NoWifi
            port == null -> PhoneInfo.Failed
            else -> PhoneInfo.Ready("http://$ip:$port/$token")
        }
        onDispose { srv.stop() }
    }

    when (val i = info) {
        is PhoneInfo.Ready -> BoxWithConstraints {
            val steps: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.phone_step1), color = TextMain, fontSize = 14.sp)
                    Text(stringResource(R.string.phone_step2), color = TextMain, fontSize = 14.sp)
                    Text(i.url, color = Cyan, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.phone_step3), color = TextMuted, fontSize = 14.sp)
                }
            }
            if (maxWidth > 520.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    QrImage(i.url, 180.dp)
                    steps(Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    QrImage(i.url, 200.dp)
                    steps(Modifier)
                }
            }
        }
        PhoneInfo.NoWifi -> Text(stringResource(R.string.phone_no_wifi), color = Color(0xFFFCA5A5), fontSize = 14.sp)
        PhoneInfo.Failed -> Text(stringResource(R.string.phone_failed), color = Color(0xFFFCA5A5), fontSize = 14.sp)
        null -> {}
    }
}

@Composable
private fun QrImage(text: String, size: Dp) {
    val bmp = remember(text) { makeQr(text) }
    androidx.compose.foundation.layout.Box(
        Modifier.size(size).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(10.dp),
    ) {
        Image(bitmap = bmp, contentDescription = null, modifier = Modifier.fillMaxSize(), filterQuality = FilterQuality.None)
    }
}

private fun makeQr(text: String): ImageBitmap {
    val m = QRCodeWriter().encode(
        text, BarcodeFormat.QR_CODE, 0, 0,
        mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.CHARACTER_SET to "UTF-8"),
    )
    val w = m.width
    val h = m.height
    val px = IntArray(w * h) { if (m[it % w, it / w]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bmp.setPixels(px, 0, w, 0, 0, w, h)
    return bmp.asImageBitmap()
}
