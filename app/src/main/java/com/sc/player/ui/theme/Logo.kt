package com.sc.player.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** شعار SC Player: مثلث تشغيل بإطار متدرّج ومثلث أزرق داخله. */
@Composable
fun SCLogo(logoSize: Dp = 44.dp) {
    Canvas(Modifier.size(logoSize)) {
        val w = size.width
        val h = size.height
        val outer = Path().apply {
            moveTo(w * 0.20f, h * 0.10f)
            lineTo(w * 0.92f, h * 0.50f)
            lineTo(w * 0.20f, h * 0.90f)
            close()
        }
        drawPath(
            outer,
            Brush.linearGradient(
                listOf(Color(0xFF7C3AED), Color(0xFFC084FC), Color(0xFF38BDF8)),
                start = Offset(0f, 0f),
                end = Offset(w, h),
            ),
            style = Stroke(width = w * 0.11f, join = StrokeJoin.Round),
        )
        val inner = Path().apply {
            moveTo(w * 0.40f, h * 0.34f)
            lineTo(w * 0.70f, h * 0.50f)
            lineTo(w * 0.40f, h * 0.66f)
            close()
        }
        drawPath(inner, Color(0xFF38BDF8))
    }
}
