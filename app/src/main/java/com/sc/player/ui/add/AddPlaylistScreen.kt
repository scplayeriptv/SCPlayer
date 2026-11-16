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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.ui.unit.Dp
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
                // الفورم بعرض الشاشة كلها؛ حقلين جنب بعض لو العرض يسمح
                FormCard(state, vm, Modifier.fillMaxSize(), maxWidth >= 600.dp)
            }
        }
    }
}

@Composable
private fun TopBar(onBack: (() -> Unit)?) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
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
        }
        SCLogo(36.dp)
        Column(Modifier.weight(1f)) {
            Text(
                "SC Player",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(brush = BrandBrush),
            )
            Text(stringResource(R.string.add_title), color = TextMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Brand(modifier: Modifier, compact: Boolean, onList: (() -> Unit)? = null) {
    if (compact) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ServerIllustration(84.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "SC Player",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = TextStyle(brush = BrandBrush),
                )
                Text(
                    stringResource(R.string.add_brand_text),
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            }
        }
    } else {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ServerIllustration(150.dp)
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
            if (onList != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Glass)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button, onClick = onList)
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                ) {
                    Text("📋  " + stringResource(R.string.add_list_playlists), color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** رسم سيرفر بسيط: ثلاث وحدات مكدّسة وعلامة + بنفسجية. */
@Composable
private fun ServerIllustration(illustrationSize: Dp) {
    Canvas(Modifier.size(illustrationSize)) {
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
    val shape = RoundedCornerShape(24.dp)
    var tab by remember { mutableIntStateOf(1) }
    var showPass by remember { mutableStateOf(false) }
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

    val narrow = !scroll
    Column(
        modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .background(Glass, shape)
            .border(1.dp, GlassBorder, shape)
            .verticalScroll(rememberScrollState())
            .padding(if (narrow) 18.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(if (narrow) 14.dp else 12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandChip(stringResource(R.string.tab_m3u), tab == 0, Modifier.weight(1f)) { tab = 0; vm.clearError() }
            BrandChip(stringResource(R.string.tab_xtream), tab == 1, Modifier.weight(1f)) { tab = 1; vm.clearError() }
            BrandChip(stringResource(R.string.tab_qr), tab == 2, Modifier.weight(1f)) { tab = 2; vm.clearError() }
            BrandChip(stringResource(R.string.tab_phone), tab == 3, Modifier.weight(1f)) { tab = 3; vm.clearError() }
        }

        when (tab) {
            0 -> if (narrow) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(stringResource(R.string.field_url), url, Modifier.fillMaxWidth(), KeyboardType.Uri) { url = it }
                    Field(stringResource(R.string.field_name), name, Modifier.fillMaxWidth()) { name = it }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Field(stringResource(R.string.field_url), url, Modifier.weight(2f), KeyboardType.Uri) { url = it }
                    Field(stringResource(R.string.field_name), name, Modifier.weight(1f)) { name = it }
                }
            }
            1 -> {
                val nameF: @Composable (Modifier) -> Unit = { m -> Field(stringResource(R.string.field_name), name, m) { name = it } }
                val userF: @Composable (Modifier) -> Unit = { m -> Field(stringResource(R.string.field_user), user, m) { user = it } }
                val passF: @Composable (Modifier) -> Unit = { m ->
                    Field(
                        stringResource(R.string.field_pass), pass, m, KeyboardType.Password,
                        secret = !showPass,
                        trailing = {
                            Text(
                                if (showPass) "🙈" else "👁",
                                fontSize = 20.sp,
                                modifier = Modifier.clip(CircleShape).clickable(role = Role.Button) { showPass = !showPass }.padding(8.dp),
                            )
                        },
                    ) { pass = it }
                }
                val serverF: @Composable (Modifier) -> Unit = { m ->
                    Field(
                        stringResource(R.string.field_server), server, m, KeyboardType.Uri,
                        ime = ImeAction.Done,
                        onDone = { if (canSubmit) vm.addXtream(name, server, user, pass) },
                    ) { server = it }
                }
                if (scroll) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            nameF(Modifier.weight(1f)); userF(Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            passF(Modifier.weight(1f)); serverF(Modifier.weight(1f))
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        nameF(Modifier.fillMaxWidth()); userF(Modifier.fillMaxWidth())
                        passF(Modifier.fillMaxWidth()); serverF(Modifier.fillMaxWidth())
                    }
                }
            }
            3 -> PhoneAddPanel { p ->
                if (p.type == "m3u") vm.addM3u(p.name, p.url) else vm.addXtream(p.name, p.server, p.user, p.pass)
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
    ime: ImeAction = ImeAction.Next,
    trailing: (@Composable () -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ime),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        trailingIcon = trailing,
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
