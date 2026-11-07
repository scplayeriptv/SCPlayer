package com.sc.player.ui.add

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
fun AddPlaylistScreen(onDone: (String) -> Unit, vm: AddPlaylistViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) { (state as? AddState.Done)?.let { onDone(it.id) } }

    ScreenBackground {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth >= 700.dp) {
                Row(Modifier.fillMaxSize().padding(48.dp), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
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

@Composable
private fun Brand(modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "SC Player",
            fontSize = 52.sp,
            fontWeight = FontWeight.ExtraBold,
            style = TextStyle(brush = BrandBrush),
        )
        Text(
            "مشغّل IPTV سريع وأنيق. أضف قائمتك الخاصة وابدأ المشاهدة في أقل من دقيقة.",
            color = Color(0xFFC7D2FE),
            fontSize = 18.sp,
            lineHeight = 30.sp,
        )
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
        Text("أضف قائمتك", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextMain)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandChip("رابط M3U", tab == 0, Modifier.weight(1f)) { tab = 0; vm.clearError() }
            BrandChip("Xtream Codes", tab == 1, Modifier.weight(1f)) { tab = 1; vm.clearError() }
            BrandChip("مسح QR", tab == 2, Modifier.weight(1f)) { tab = 2; vm.clearError() }
        }

        when (tab) {
            0 -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Field("رابط القائمة", url, Modifier.weight(2f), KeyboardType.Uri) { url = it }
                Field("اسم القائمة", name, Modifier.weight(1f)) { name = it }
            }
            1 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Field("السيرفر", server, Modifier.weight(2f), KeyboardType.Uri) { server = it }
                    Field("اسم القائمة", name, Modifier.weight(1f)) { name = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Field("اسم المستخدم", user, Modifier.weight(1f)) { user = it }
                    Field("كلمة المرور", pass, Modifier.weight(1f), KeyboardType.Password, true) { pass = it }
                }
            }
            else -> Text("المسح بالـ QR هيتضاف في جزء لاحق.", color = TextMuted, fontSize = 15.sp)
        }

        Text("التطبيق مشغّل فقط ولا يحتوي على أي قنوات.", color = TextMuted, fontSize = 13.sp)

        (state as? AddState.Error)?.let {
            Text(it.message, color = Color(0xFFFCA5A5), fontSize = 14.sp)
        }

        Spacer(Modifier.height(4.dp))
        GradientButton(
            text = if (loading) "جاري التحميل..." else "إضافة وبدء المشاهدة",
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
        ),
    )
}
