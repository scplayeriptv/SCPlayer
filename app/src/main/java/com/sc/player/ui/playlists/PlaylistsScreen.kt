package com.sc.player.ui.playlists

import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.data.Playlist
import com.sc.player.data.PlaylistRepository
import com.sc.player.data.friendlyError
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.Ink
import com.sc.player.ui.theme.ScreenBackground
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import kotlinx.coroutines.launch

private val Danger = Color(0xFFFCA5A5)

@Composable
fun PlaylistsScreen(
    activeId: String?,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    onEmpty: () -> Unit,
) {
    val ctx = LocalContext.current
    val repo = remember { PlaylistRepository(ctx.applicationContext) }
    val scope = rememberCoroutineScope()
    var lists by remember { mutableStateOf(repo.playlists()) }
    var busy by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<Playlist?>(null) }

    ScreenBackground {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionPill("رجوع", onClick = onBack)
                Text(
                    "قوائمك",
                    color = TextMain,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                ActionPill("+ قائمة جديدة", primary = true, onClick = onAdd)
            }
            message?.let { Text(it, color = Cyan, fontSize = 14.sp) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                items(lists, key = { it.id }) { p ->
                    PlaylistCard(
                        playlist = p,
                        active = p.id == activeId,
                        refreshing = busy == p.id,
                        idle = busy == null,
                        onOpen = { onOpen(p.id) },
                        onRefresh = {
                            busy = p.id
                            message = null
                            scope.launch {
                                repo.refresh(p.id).fold(
                                    onSuccess = { message = "اتحدّثت «${p.name}»: $it قناة." },
                                    onFailure = { message = "فشل التحديث: ${friendlyError(it)}" },
                                )
                                busy = null
                            }
                        },
                        onDelete = { confirmDelete = p },
                    )
                }
            }
        }
    }

    confirmDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("حذف القائمة؟") },
            text = { Text("هتتحذف «${p.name}» من التطبيق. تقدر تضيفها تاني بنفس الرابط.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    repo.delete(p.id)
                    lists = repo.playlists()
                    if (lists.isEmpty()) onEmpty() else if (p.id == activeId) onOpen(lists.first().id)
                }) { Text("حذف", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("إلغاء") }
            },
        )
    }
}

private fun hostOf(url: String): String =
    runCatching { Uri.parse(url).host }.getOrNull() ?: "رابط مخصص"

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    active: Boolean,
    refreshing: Boolean,
    idle: Boolean,
    onOpen: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Glass)
            .border(1.dp, if (active) Cyan else GlassBorder, shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                playlist.name,
                color = TextMain,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (active) Text("الحالية", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(hostOf(playlist.url), color = TextMuted, fontSize = 13.sp, maxLines = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionPill(if (active) "فتح (الحالية)" else "فتح", primary = true, onClick = onOpen)
            ActionPill(if (refreshing) "جاري التحديث..." else "تحديث", enabled = idle, onClick = onRefresh)
            ActionPill("حذف", danger = true, enabled = idle, onClick = onDelete)
        }
    }
}

@Composable
private fun ActionPill(
    text: String,
    primary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .onFocusChanged { focused = it.isFocused }
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(if (primary) BrandBrush else Brush.linearGradient(listOf(Glass, Glass)))
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else GlassBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (primary) Ink else if (danger) Danger else TextMain,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}
