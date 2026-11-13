package com.sc.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.data.Channel
import com.sc.player.ui.home.ChannelLogo
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.Ink
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import kotlinx.coroutines.delay

/** قائمة القنوات فوق الفيديو: اختيار قناة من غير ما تخرج من المشغّل. */
@Composable
internal fun ChannelListPanel(
    queue: List<Channel>,
    current: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (current - 3).coerceAtLeast(0))
    val currentFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(80)
        runCatching { currentFocus.requestFocus() }
    }
    LazyColumn(
        modifier
            .width(320.dp)
            .fillMaxHeight()
            .background(Color(0xEB0B0714))
            .padding(vertical = 8.dp),
        state = state,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(queue) { i, ch ->
            var focused by remember { mutableStateOf(false) }
            val shape = RoundedCornerShape(14.dp)
            val selected = i == current
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (selected) Modifier.focusRequester(currentFocus) else Modifier)
                    .onFocusChanged { focused = it.isFocused }
                    .clip(shape)
                    .background(
                        when {
                            focused -> Color(0x33A855F7)
                            selected -> Color(0x22FFFFFF)
                            else -> Color.Transparent
                        },
                    )
                    .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else if (selected) GlassBorder else Color.Transparent, shape)
                    .clickable(role = Role.Button) { onPick(i) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "${i + 1}",
                    color = if (selected) Cyan else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(30.dp),
                )
                ChannelLogo(ch, 34.dp)
                Text(
                    ch.name,
                    color = if (selected) Ink else TextMain,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** الرقم اللي المستخدم بيكتبه على الريموت (مثلاً 105). */
@Composable
internal fun NumberBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Ink,
        fontSize = 44.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(BrandBrush).padding(horizontal = 22.dp, vertical = 10.dp),
    )
}
