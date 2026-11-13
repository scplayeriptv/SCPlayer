package com.sc.player.ui.home

import com.sc.player.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.data.Channel
import com.sc.player.data.Kind
import com.sc.player.data.epg.EpgStore
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.GradientButton
import com.sc.player.ui.theme.Ink
import com.sc.player.ui.theme.Panel
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val FAV_KEY = "__fav__"

private fun hm(t: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(t))

/** قائمة القنوات (أو الأفلام/المسلسلات/الراديو): تصنيفات + قائمة + لوحة تفاصيل على الشاشات الكبيرة. */
@Composable
fun ContentScreen(
    kind: Kind,
    items: List<Channel>,
    favs: Set<String>,
    epgTick: Int,
    wide: Boolean,
    onToggleFav: (String) -> Unit,
    onPlay: (List<Channel>, Int) -> Unit,
    onGuide: (List<Channel>) -> Unit,
) {
    val huge = LocalConfiguration.current.screenWidthDp >= 1100
    var selected by rememberSaveable(kind.name) { mutableStateOf<String?>(null) }
    var query by rememberSaveable(kind.name) { mutableStateOf("") }
    var highlighted by remember { mutableStateOf<Channel?>(null) }

    val groups = remember(items) { items.groupingBy { it.group }.eachCount().entries.toList() }
    val favCount = remember(items, favs) { items.count { it.url in favs } }

    // البحث بيستنى 250ms بعد آخر حرف، والفلترة بتتم في الخلفية عشان القوائم الكبيرة
    var debounced by remember { mutableStateOf(query) }
    LaunchedEffect(query) {
        if (query != debounced) {
            delay(250)
            debounced = query
        }
    }
    val filtered by produceState(items, items, selected, debounced, favs) {
        val q = debounced.trim()
        val sel = selected
        val fav = favs
        value = withContext(Dispatchers.Default) {
            items.filter {
                val inCategory = when (sel) {
                    null -> true
                    FAV_KEY -> it.url in fav
                    else -> it.group == sel
                }
                inCategory && (q.isEmpty() || it.name.contains(q, ignoreCase = true))
            }
        }
    }
    val live = kind == Kind.Live
    val posters = kind == Kind.Movies || kind == Kind.Series
    val allLabel = if (live) stringResource(R.string.all_channels) else stringResource(R.string.all_items)
    val favLabel = stringResource(R.string.favorites)
    val kindTitle = stringResource(kind.titleRes)

    Column(Modifier.fillMaxSize().padding(top = 16.dp)) {
        Row(
            Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenTitle(kindTitle)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint), color = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextMain,
                    unfocusedTextColor = TextMain,
                    cursorColor = Cyan,
                    focusedContainerColor = Glass,
                    unfocusedContainerColor = Glass,
                ),
            )
            if (live) CategoryChip(stringResource(R.string.guide), false) { onGuide(filtered) }
        }

        if (!wide) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 12.dp),
            ) {
                item { CategoryChip("$allLabel (${items.size})", selected == null) { selected = null } }
                item { CategoryChip("$favLabel ($favCount)", selected == FAV_KEY) { selected = FAV_KEY } }
                items(groups) { g ->
                    CategoryChip("${g.key} (${g.value})", selected == g.key) { selected = g.key }
                }
            }
        } else {
            Box(Modifier.padding(top = 12.dp))
        }

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (wide) {
                LazyColumn(
                    Modifier.width(220.dp).fillMaxHeight().padding(start = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item { CategoryRow(allLabel, items.size, selected == null) { selected = null } }
                    item { CategoryRow(favLabel, favCount, selected == FAV_KEY) { selected = FAV_KEY } }
                    items(groups) { g -> CategoryRow(g.key, g.value, selected == g.key) { selected = g.key } }
                }
            }

            if (filtered.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(
                        when {
                            items.isEmpty() -> stringResource(R.string.empty_none, kindTitle)
                            selected == FAV_KEY -> stringResource(R.string.empty_favs)
                            else -> stringResource(R.string.empty_nomatch)
                        },
                        color = TextMuted,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            } else if (posters) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (wide) 140.dp else 110.dp),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(start = if (wide) 0.dp else 24.dp, end = 24.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    gridItemsIndexed(filtered) { i, ch ->
                        PosterCard(
                            ch = ch,
                            fav = ch.url in favs,
                            onFocus = { highlighted = ch },
                            onToggleFav = { onToggleFav(ch.url) },
                        ) { onPlay(filtered, i) }
                    }
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(
                        start = if (wide) 0.dp else 24.dp,
                        end = if (huge) 0.dp else 24.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(filtered) { i, ch ->
                        ChannelRow(
                            ch = ch,
                            now = epgNow(ch, epgTick),
                            fav = ch.url in favs,
                            live = live,
                            onFocus = { highlighted = ch },
                            onToggleFav = { onToggleFav(ch.url) },
                        ) { onPlay(filtered, i) }
                    }
                }
            }

            if (huge && !posters) {
                val shown = highlighted?.takeIf { h -> filtered.any { it === h } } ?: filtered.firstOrNull()
                Box(Modifier.width(320.dp).fillMaxHeight().padding(end = 24.dp, bottom = 24.dp)) {
                    if (shown != null) {
                        DetailsPanel(
                            ch = shown,
                            live = live,
                            fav = shown.url in favs,
                            epgTick = epgTick,
                            onToggleFav = { onToggleFav(shown.url) },
                            onPlay = {
                                val i = filtered.indexOfFirst { it === shown }
                                if (i >= 0) onPlay(filtered, i)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (selected) BrandBrush else androidx.compose.ui.graphics.SolidColor(Glass), shape)
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else GlassBorder, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (selected) Ink else TextMain,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(count.toString(), color = if (selected) Ink else TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun DetailsPanel(
    ch: Channel,
    live: Boolean,
    fav: Boolean,
    epgTick: Int,
    onToggleFav: () -> Unit,
    onPlay: () -> Unit,
) {
    val now = epgNow(ch, epgTick)
    val next = remember(ch, epgTick) { EpgStore.next(ch, System.currentTimeMillis()) }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().background(Panel.copy(alpha = 0.6f)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChannelLogo(ch, 96.dp)
            Text(
                ch.name,
                color = TextMain,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (live) LiveBadge()
                Text(ch.group, color = TextMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (now != null) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.panel_now, hm(now.start), hm(now.stop)), color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(now.title, color = TextMain, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (next != null) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.panel_next, hm(next.start)), color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(next.title, color = TextMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            GradientButton(stringResource(R.string.play), Modifier.fillMaxWidth(), onClick = onPlay)
            CategoryChip(if (fav) stringResource(R.string.in_favorites) else stringResource(R.string.add_favorites), fav, onToggleFav)
        }
    }
}
