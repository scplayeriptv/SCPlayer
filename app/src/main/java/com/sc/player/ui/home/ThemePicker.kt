package com.sc.player.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.R
import com.sc.player.data.UserPrefs
import com.sc.player.ui.theme.AppTheme
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import com.sc.player.ui.theme.ThemeState

private fun AppTheme.nameRes() = when (this) {
    AppTheme.Violet -> R.string.theme_violet
    AppTheme.Ocean -> R.string.theme_ocean
    AppTheme.Emerald -> R.string.theme_emerald
    AppTheme.Sunset -> R.string.theme_sunset
    AppTheme.Rose -> R.string.theme_rose
    AppTheme.Amoled -> R.string.theme_amoled
}

/** صف اختيار الثيم: دواير ملوّنة، الاختيار بيتطبّق فورًا ويتحفظ. */
@Composable
internal fun ThemePicker(prefs: UserPrefs) {
    val current = ThemeState.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.set_theme), color = TextMain, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Text(stringResource(current.nameRes()), color = TextMuted, fontSize = 14.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AppTheme.values().forEach { t ->
                var focused by remember { mutableStateOf(false) }
                val sel = t == current
                Box(
                    Modifier
                        .size(38.dp)
                        .onFocusChanged { focused = it.isFocused }
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(t.grad1, t.grad2)))
                        .border(if (sel || focused) 3.dp else 1.dp, if (sel || focused) Color.White else Color(0x33FFFFFF), CircleShape)
                        .clickable(role = Role.RadioButton) {
                            ThemeState.current = t
                            prefs.setTheme(t.id)
                        },
                )
            }
        }
    }
}
