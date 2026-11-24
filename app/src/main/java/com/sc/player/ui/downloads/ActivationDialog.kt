package com.sc.player.ui.downloads

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.R
import com.sc.player.data.Premium
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

/** نافذة قفل ميزة التحميل/التسجيل: بتعرض كود الجهاز علشان يتفعّل. */
@Composable
fun ActivationDialog(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val code = remember { Premium.deviceCode(ctx) }
    LaunchedEffect(Unit) { Premium.refresh(ctx) }
    if (Premium.active) { onClose(); return }
    com.sc.player.ui.premium.ProSheet(onClose)
    if (true) return
    SheetDialog(stringResource(R.string.premium_title), onClose) {
        Text(stringResource(R.string.premium_desc), color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Text(
            code,
            color = TextMain,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x22FFFFFF))
                .padding(vertical = 14.dp),
        )
        Spacer(Modifier.height(10.dp))
        ChoiceCard(stringResource(R.string.premium_copy), null, true, Modifier.fillMaxWidth()) {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("code", code))
            Toast.makeText(ctx, R.string.premium_copied, Toast.LENGTH_SHORT).show()
        }
        Spacer(Modifier.height(8.dp))
        ChoiceCard(
            stringResource(if (Premium.checking) R.string.premium_checking else R.string.premium_recheck),
            null, false, Modifier.fillMaxWidth(),
        ) { Premium.refresh(ctx) }
        if (Premium.lastError) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.premium_offline), color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** نسخة Google Play: اختيار اشتراك سنوي أو شراء مرة واحدة. */
@Composable
private fun PlayPurchaseDialog(onClose: () -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { com.sc.player.data.PlayBilling.start(ctx) }
    val activity = remember {
        var c: Context? = ctx
        var a: android.app.Activity? = null
        while (c is android.content.ContextWrapper) { if (c is android.app.Activity) { a = c; break }; c = c.baseContext }
        a
    }
    val yearly = com.sc.player.data.PlayBilling.yearlyPrice
    val life = com.sc.player.data.PlayBilling.lifetimePrice
    SheetDialog(stringResource(R.string.premium_title), onClose) {
        Text(stringResource(R.string.premium_play_desc), color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        ChoiceCard(stringResource(R.string.premium_yearly), yearly ?: "…", true, Modifier.fillMaxWidth()) {
            activity?.let { com.sc.player.data.PlayBilling.buy(it, com.sc.player.data.PlayBilling.SKU_YEARLY) }
        }
        Spacer(Modifier.height(8.dp))
        ChoiceCard(stringResource(R.string.premium_lifetime), life ?: "…", false, Modifier.fillMaxWidth()) {
            activity?.let { com.sc.player.data.PlayBilling.buy(it, com.sc.player.data.PlayBilling.SKU_LIFETIME) }
        }
        Spacer(Modifier.height(8.dp))
        ChoiceCard(stringResource(R.string.premium_restore), null, false, Modifier.fillMaxWidth()) { com.sc.player.data.PlayBilling.refresh(ctx) }
        com.sc.player.data.PlayBilling.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.premium_play_terms), color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
