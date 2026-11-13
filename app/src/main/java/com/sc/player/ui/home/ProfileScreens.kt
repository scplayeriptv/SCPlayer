package com.sc.player.ui.home

import com.sc.player.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.data.Profile
import com.sc.player.data.UserPrefs
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.Ink
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import com.sc.player.ui.theme.Violet

internal val Avatars = listOf("🙂", "😎", "🦊", "🐼", "🦁", "🐯", "🚀", "⚽", "🎮", "🌙", "👑", "🎬")

internal fun Profile.avatarEmoji(): String = Avatars[avatar.coerceIn(0, Avatars.lastIndex)]

@Composable
internal fun Profile.displayName(): String = name.ifBlank { stringResource(R.string.profile_default) }

@Composable
private fun AvatarCircle(p: Profile, size: Dp, focused: Boolean = false) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(BrandBrush)
            .border(if (focused) 4.dp else 2.dp, if (focused) Cyan else GlassBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(p.avatarEmoji(), fontSize = (size.value * 0.5f).sp)
    }
}

/** شاشة "مين بيتفرج؟". */
@Composable
fun ProfilePickerScreen(
    profiles: List<Profile>,
    activeId: String,
    onPick: (Profile) -> Unit,
    onCancel: (() -> Unit)?,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.profile_who),
            color = TextMain,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            modifier = Modifier.padding(top = 28.dp).weight(1f, fill = false),
            horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            items(profiles) { p ->
                var focused by remember { mutableStateOf(false) }
                Column(
                    Modifier
                        .onFocusChanged { focused = it.isFocused }
                        .graphicsLayer { scaleX = if (focused) 1.08f else 1f; scaleY = if (focused) 1.08f else 1f }
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button) { onPick(p) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AvatarCircle(p, 88.dp, focused || p.id == activeId)
                    Text(
                        p.displayName(),
                        color = TextMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (p.kids) {
                        Text(
                            stringResource(R.string.profile_kids_badge),
                            color = Ink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(BrandBrush).padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
        if (onCancel != null) {
            TextButton(onClick = onCancel, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

/** إضافة/تعديل بروفايل. initial == null = بروفايل جديد. */
@Composable
fun ProfileEditDialog(
    prefs: UserPrefs,
    initial: Profile?,
    isActive: Boolean,
    onChanged: () -> Unit,
    onClose: () -> Unit,
) {
    val isMain = initial?.id == UserPrefs.MAIN
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var avatar by remember { mutableStateOf(initial?.avatar ?: 0) }
    var kids by remember { mutableStateOf(initial?.kids ?: false) }
    val defaultName = stringResource(R.string.profile_default)

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(if (initial == null) R.string.profile_add else R.string.profile_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 16) name = it },
                    singleLine = true,
                    placeholder = { Text(if (isMain) defaultName else stringResource(R.string.profile_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(Avatars) { i, e ->
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (i == avatar) Violet else Glass)
                                .border(if (i == avatar) 2.dp else 1.dp, if (i == avatar) Cyan else GlassBorder, CircleShape)
                                .clickable(role = Role.RadioButton) { avatar = i },
                            contentAlignment = Alignment.Center,
                        ) { Text(e, fontSize = 22.sp) }
                    }
                }
                if (!isMain) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.profile_kids), color = TextMain, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.profile_kids_sub), color = TextMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = kids,
                            onCheckedChange = { kids = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Violet, checkedThumbColor = Ink),
                        )
                    }
                }
                if (initial != null && !isActive) {
                    TextButton(onClick = {
                        prefs.setActiveProfile(initial.id)
                        onChanged()
                        onClose()
                    }) { Text(stringResource(R.string.profile_use)) }
                }
                if (initial != null && !isMain) {
                    TextButton(onClick = {
                        prefs.deleteProfile(initial.id)
                        onChanged()
                        onClose()
                    }) { Text(stringResource(R.string.delete), color = androidx.compose.ui.graphics.Color(0xFFFCA5A5)) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isMain || name.isNotBlank(),
                onClick = {
                    val p = Profile(initial?.id ?: prefs.newProfileId(), name.trim(), kids, avatar)
                    prefs.saveProfile(p)
                    onChanged()
                    onClose()
                },
            ) { Text(stringResource(R.string.profile_save)) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } },
    )
}

/** شريحة صغيرة في الرئيسية: أيقونة البروفايل واسمه، وبتفتح شاشة التبديل. */
@Composable
internal fun ProfileChip(p: Profile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(Glass)
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else GlassBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(p.avatarEmoji(), fontSize = 16.sp)
        Text(p.displayName(), color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
