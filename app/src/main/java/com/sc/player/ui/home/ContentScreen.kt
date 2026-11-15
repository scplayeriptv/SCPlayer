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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
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
            ScreenTitle(kindTitle, "${items.size}")
            SearchPill(query, { query = it }, stringResource(R.string.search_hint), Modifier.weight(1f))
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
                    Modifier
                        .padding(start = 24.dp, bottom = 16.dp)
                        .width(240.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .background(Panel.copy(alpha = 0.55f))
                        .border(1.dp, GlassBorder, RoundedCornerShape(26.dp)),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(10.dp),
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
    val shape = RoundedCornerShape(18.dp)
    val fill: androidx.compose.ui.graphics.Brush = when {
        selected -> BrandBrush
        focused -> androidx.compose.ui.graphics.SolidColor(Cyan.copy(alpha = 0.16f))
        else -> androidx.compose.ui.graphics.SolidColor(Color.Transparent)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(fill, shape)
            .then(if (focused) Modifier.border(2.dp, Cyan, shape) else Modifier)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(start = 8.dp, end = 10.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) Color.White else if (focused) Cyan else Color.Transparent),
        )
        Text(
            label,
            color = if (selected) Ink else TextMain,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            count.toString(),
            color = if (selected) Ink else TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) Color(0x33000000) else Color(0x22FFFFFF))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
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
