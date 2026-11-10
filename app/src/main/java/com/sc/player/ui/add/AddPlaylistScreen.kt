package com.sc.player.ui.add

import com.sc.player.R
import androidx.compose.ui.res.stringResource
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import androidx.compose.ui.platform.LocalContext
import com.sc.player.ui.theme.SCLogo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.BrandChip
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.GradientButton
import com.sc.player.ui.theme.ScreenBackground
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

@Composable
fun AddPlaylistScreen(
    onDone: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    vm: AddPlaylistViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) { (state as? AddState.Done)?.let { onDone(it.id) } }

    ScreenBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(onBack)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                if (maxWidth >= 700.dp) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                    ) {
                        Brand(Modifier.weight(0.4f).align(Alignment.CenterVertically))
                        FormCard(state, vm, Modifier.weight(0.6f).fillMaxSize(), true)
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Brand(Modifier.fillMaxWidth())
                        FormCard(state, vm, Modifier.fillMaxWidth(), false)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(onBack: (() -> Unit)?) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Glass)
                    .border(1.dp, GlassBorder, CircleShape)
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (rtl) "→" else "←", color = TextMain, fontSize = 20.sp)
            }
        } else {
            SCLogo(36.dp)
        }
        Text(
            stringResource(R.string.add_title),
            color = TextMain,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Brand(modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ServerIllustration()
        Text(
            "SC Player",
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            style = TextStyle(brush = BrandBrush),
        )
        Text(
            stringResource(R.string.add_brand_text),
            color = TextMuted,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** رسم سيرفر بسيط: ثلاث وحدات مكدّسة وعلامة + بنفسجية. */
@Composable
private fun ServerIllustration() {
    Canvas(Modifier.size(150.dp)) {
        val w = size.width
        val h = size.height
        val unitH = h * 0.22f
        for (i in 0 until 3) {
            val top = h * 0.06f + i * (unitH + h * 0.05f)
            val tl = Offset(w * 0.08f, top)
            val sz = Size(w * 0.84f, unitH)
            drawRoundRect(Color(0xFF1B1240), tl, sz, CornerRadius(w * 0.06f))
            drawRoundRect(Color(0xFF7C3AED), tl, sz, CornerRadius(w * 0.06f), style = Stroke(width = w * 0.015f))
            drawCircle(Color(0xFF38BDF8), w * 0.025f, Offset(w * 0.18f, top + unitH / 2))
            drawCircle(Color(0xFF38BDF8), w * 0.025f, Offset(w * 0.26f, top + unitH / 2))
            drawLine(Color(0xFF7C3AED), Offset(w * 0.5f, top + unitH / 2), Offset(w * 0.8f, top + unitH / 2), strokeWidth = w * 0.015f)
        }
        val c = Offset(w * 0.8f, h * 0.84f)
        drawCircle(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF38BDF8)), start = Offset(c.x - w * 0.15f, c.y - w * 0.15f), end = Offset(c.x + w * 0.15f, c.y + w * 0.15f)), w * 0.15f, c)
        drawLine(Color.White, Offset(c.x - w * 0.07f, c.y), Offset(c.x + w * 0.07f, c.y), strokeWidth = w * 0.03f)
        drawLine(Color.White, Offset(c.x, c.y - w * 0.07f), Offset(c.x, c.y + w * 0.07f), strokeWidth = w * 0.03f)
    }
}

@Composable
private fun FormCard(state: AddState, vm: AddPlaylistViewModel, modifier: Modifier, scroll: Boolean) {
    val shape = RoundedCornerShape(32.dp)
    var tab by remember { mutableIntStateOf(0) }
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    val loading = state is AddState.Loading
    val ctx = LocalContext.current
    var qrError by remember { mutableStateOf<String?>(null) }
    val qrInvalid = stringResource(R.string.qr_invalid)
    val qrUnavailable = stringResource(R.string.qr_unavailable)

    val canSubmit = !loading && when (tab) {
        0 -> url.trim().startsWith("http", ignoreCase = true)
        1 -> server.isNotBlank() && user.isNotBlank() && pass.isNotBlank()
        else -> false
    }

    Column(
        modifier
            .background(Glass, shape)
            .border(1.dp, GlassBorder, shape)
            .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandChip(stringResource(R.string.tab_m3u), tab == 0, Modifier.weight(1f)) { tab = 0; vm.clearError() }
            BrandChip(stringResource(R.string.tab_xtream), tab == 1, Modifier.weight(1f)) { tab = 1; vm.clearError() }
            BrandChip(stringResource(R.string.tab_qr), tab == 2, Modifier.weight(1f)) { tab = 2; vm.clearError() }
        }

        when (tab) {
            0 -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Field(stringResource(R.string.field_url), url, Modifier.weight(2f), KeyboardType.Uri) { url = it }
                Field(stringResource(R.string.field_name), name, Modifier.weight(1f)) { name = it }
            }
            1 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Field(stringResource(R.string.field_server), server, Modifier.weight(2f), KeyboardType.Uri) { server = it }
                    Field(stringResource(R.string.field_name), name, Modifier.weight(1f)) { name = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Field(stringResource(R.string.field_user), user, Modifier.weight(1f)) { user = it }
                    Field(stringResource(R.string.field_pass), pass, Modifier.weight(1f), KeyboardType.Password, true) { pass = it }
                }
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientButton(stringResource(R.string.qr_scan), Modifier.fillMaxWidth()) {
                    qrError = null
                    val options = GmsBarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                    GmsBarcodeScanning.getClient(ctx, options).startScan()
                        .addOnSuccessListener { code ->
                            val v = code.rawValue?.trim().orEmpty()
                            if (v.startsWith("http", ignoreCase = true)) {
                                url = v
                                tab = 0
                            } else {
                                qrError = qrInvalid
                            }
                        }
                        .addOnFailureListener { qrError = qrUnavailable }
                }
                qrError?.let { Text(it, color = Color(0xFFFCA5A5), fontSize = 14.sp) }
            }
        }

        Text(stringResource(R.string.player_only_note), color = TextMuted, fontSize = 13.sp)

        (state as? AddState.Error)?.let {
            Text(it.message, color = Color(0xFFFCA5A5), fontSize = 14.sp)
        }

        Spacer(Modifier.height(4.dp))
        GradientButton(
            text = if (loading) stringResource(R.string.loading) else "✓  " + stringResource(R.string.add_and_watch),
            modifier = Modifier.fillMaxWidth(),
            enabled = canSubmit,
        ) {
            if (tab == 0) vm.addM3u(name, url) else vm.addXtream(name, server, user, pass)
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    modifier: Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Cyan,
            unfocusedBorderColor = GlassBorder,
            focusedTextColor = TextMain,
            unfocusedTextColor = TextMain,
            cursorColor = Cyan,
            focusedLabelColor = Cyan,
            unfocusedLabelColor = TextMuted,
            focusedContainerColor = Glass,
            unfocusedContainerColor = Glass,
        ),
    )
}
