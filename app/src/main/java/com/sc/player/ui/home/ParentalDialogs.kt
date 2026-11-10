package com.sc.player.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.R
import com.sc.player.data.ParentalSession
import com.sc.player.data.UserPrefs
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

private enum class Stage { Verify, SetNew, Confirm, Manage }

/**
 * مسار القفل الأبوي: إنشاء PIN (لو مفيش) أو إدخاله، وبعدها شاشة إدارة التصنيفات المقفولة.
 * onChanged بتتنادى كل ما حاجة اتغيّرت (تصنيفات مقفولة / حالة الفتح).
 */
@Composable
fun ParentalFlow(
    prefs: UserPrefs,
    groups: List<String>,
    onChanged: () -> Unit,
    onClose: () -> Unit,
) {
    var stage by remember { mutableStateOf(if (prefs.hasPin()) Stage.Verify else Stage.SetNew) }
    var firstPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var locked by remember { mutableStateOf(prefs.lockedGroups()) }
    var unlocked by remember { mutableStateOf(ParentalSession.unlocked) }
    val wrongPin = stringResource(R.string.pin_wrong)
    val mismatch = stringResource(R.string.pin_mismatch)

    when (stage) {
        Stage.Verify -> PinDialog(stringResource(R.string.pin_enter_title), error, onClose) { pin ->
            if (prefs.checkPin(pin)) {
                error = null
                stage = Stage.Manage
            } else {
                error = wrongPin
            }
        }
        Stage.SetNew -> PinDialog(stringResource(R.string.pin_set_title), error, onClose) { pin ->
            firstPin = pin
            error = null
            stage = Stage.Confirm
        }
        Stage.Confirm -> PinDialog(stringResource(R.string.pin_confirm_title), error, onClose) { pin ->
            if (pin == firstPin) {
                prefs.setPin(pin)
                error = null
                stage = Stage.Manage
                onChanged()
            } else {
                error = mismatch
                stage = Stage.SetNew
            }
        }
        Stage.Manage -> AlertDialog(
            onDismissRequest = onClose,
            title = { Text(stringResource(R.string.parental_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        unlocked = !unlocked
                        ParentalSession.unlocked = unlocked
                        onChanged()
                    }) {
                        Text(stringResource(if (unlocked) R.string.parental_lock_now else R.string.parental_unlock))
                    }
                    Row {
                        TextButton(onClick = { error = null; stage = Stage.SetNew }) {
                            Text(stringResource(R.string.parental_change_pin))
                        }
                        TextButton(onClick = {
                            prefs.setPin(null)
                            ParentalSession.unlocked = false
                            onChanged()
                            onClose()
                        }) { Text(stringResource(R.string.parental_remove_pin)) }
                    }
                    Text(
                        stringResource(R.string.parental_locked_cats),
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(groups) { g ->
                            val on = g in locked
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        locked = if (on) locked - g else locked + g
                                        prefs.setLockedGroups(locked)
                                        onChanged()
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = on, onCheckedChange = null)
                                Text(g, color = TextMain, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.done)) } },
        )
    }
}

@Composable
private fun PinDialog(title: String, error: String?, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
                placeholder = { Text(stringResource(R.string.pin_hint)) },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
        },
        confirmButton = {
            TextButton(enabled = pin.length == 4, onClick = { onSubmit(pin) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
