package com.sc.player.ui.premium

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sc.player.BuildConfig
import com.sc.player.R
import com.sc.player.data.PlayBilling
import com.sc.player.data.Premium
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

private val Gold1 = Color(0xFFFFD36B)
private val Gold2 = Color(0xFFFF9F1C)

/** أيقونة Pro بجنب الشعار: تاج ذهبي. بتفتح شاشة الاشتراك. */
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    var show by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Gold1, Gold2)))
            .clickable { show = true }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(Icons.Filled.WorkspacePremium, null, tint = Color(0xFF3A2400), modifier = Modifier.size(18.dp))
        Text("PRO", color = Color(0xFF3A2400), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        if (Premium.active) Icon(Icons.Filled.Check, null, tint = Color(0xFF3A2400), modifier = Modifier.size(14.dp))
    }
    if (show) ProSheet { show = false }
}

@Composable
fun ProSheet(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val play = BuildConfig.PLAY
    LaunchedEffect(Unit) { if (play) PlayBilling.start(ctx) else Premium.refresh(ctx) }
    var plan by remember { mutableStateOf(PlayBilling.SKU_YEARLY) }
    val activity = remember {
        var c: Context? = ctx
        var a: android.app.Activity? = null
        while (c is android.content.ContextWrapper) { if (c is android.app.Activity) { a = c; break }; c = c.baseContext }
        a
    }
    val shape = RoundedCornerShape(28.dp)
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(12.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth(0.94f)
                .heightIn(max = (LocalConfiguration.current.screenHeightDp - 40).dp)
                .clip(shape)
                .background(Color(0xFF1B1B1E))
                .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
                .verticalScroll(rememberScrollState()),
        ) {
            // ===== الهيدر =====
            Box(
                Modifier.fillMaxWidth().height(120.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF3A2400), Color(0xFF7A3E00), Color(0xFF1B1B1E)))),
            ) {
                Box(
                    Modifier.padding(12.dp).size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                        .clickable(onClick = onClose).align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.WorkspacePremium, null, tint = Gold1, modifier = Modifier.size(22.dp))
                        Text("Pro", color = Gold1, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(stringResource(R.string.pro_headline), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // ===== المميزات =====
                listOf(R.string.pro_f1, R.string.pro_f2, R.string.pro_f3, R.string.pro_f4, R.string.pro_f5).forEach {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Check, null, tint = Color(0xFF4FA3FF), modifier = Modifier.size(18.dp))
                        Text(stringResource(it), color = TextMain, fontSize = 15.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))

                if (Premium.active) {
                    Text(
                        stringResource(R.string.pro_active), color = Gold1, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                } else if (play) {
                    PlanCard(
                        title = stringResource(R.string.premium_yearly),
                        price = PlayBilling.yearlyPrice ?: "…",
                        tag = stringResource(R.string.pro_best),
                        selected = plan == PlayBilling.SKU_YEARLY,
                    ) { plan = PlayBilling.SKU_YEARLY }
                    PlanCard(
                        title = stringResource(R.string.premium_lifetime),
                        price = PlayBilling.lifetimePrice ?: "…",
                        tag = null,
                        selected = plan == PlayBilling.SKU_LIFETIME,
                    ) { plan = PlayBilling.SKU_LIFETIME }
                    Spacer(Modifier.height(2.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(Gold2, Color(0xFFE0501A))))
                            .clickable { activity?.let { PlayBilling.buy(it, plan) } }
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.pro_subscribe), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) }
                    PlayBilling.error?.let {
                        Text(it, color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    Text(
                        stringResource(if (plan == PlayBilling.SKU_YEARLY) R.string.pro_auto else R.string.pro_once),
                        color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(R.string.premium_restore), color = Gold2, fontSize = 13.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clickable { PlayBilling.refresh(ctx) }.padding(6.dp),
                    )
                } else {
                    // نسخة GitHub: التفعيل بكود الجهاز
                    val code = remember { Premium.deviceCode(ctx) }
                    Text(stringResource(R.string.premium_desc), color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text(
                        code, color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x22FFFFFF)).padding(vertical = 14.dp),
                    )
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(Gold2, Color(0xFFE0501A))))
                            .clickable {
                                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("code", code))
                                Toast.makeText(ctx, R.string.premium_copied, Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.premium_copy), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold) }
                    Text(
                        stringResource(if (Premium.checking) R.string.premium_checking else R.string.premium_recheck),
                        color = Gold2, fontSize = 13.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clickable { Premium.refresh(ctx) }.padding(6.dp),
                    )
                    if (Premium.lastError) {
                        Text(stringResource(R.string.premium_offline), color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(title: String, price: String, tag: String?, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val border = if (selected) BorderStroke(2.dp, Gold2) else BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    Box(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) Color(0xFF3A2414) else Color(0xFF242427))
            .border(border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        if (tag != null) {
            Text(tag, color = Gold2, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopEnd))
        }
        Row(Modifier.fillMaxWidth().padding(top = if (tag != null) 8.dp else 0.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(price, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
