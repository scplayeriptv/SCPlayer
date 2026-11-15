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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.LocalContext
import com.sc.player.data.PositionStore
import com.sc.player.data.SeenStore
import com.sc.player.data.UserPrefs
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
import com.sc.player.ui.theme.Violet
import com.sc.player.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val FAV_KEY = "__fav__"
private const val CONT_KEY = "__cont__"
private const val NEW_KEY = "__new__"

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
    val ctx = LocalContext.current
    val vod = kind == Kind.Movies || kind == Kind.Series
    // "أكمل المشاهدة": الأفلام/المسلسلات من مواضع المشاهدة، والقنوات/الراديو من آخر المشاهَد
    val cont by produceState(emptyList<Channel>(), items, kind) {
        value = withContext(Dispatchers.IO) {
            val byUrl = items.associateBy { it.url }
            val urls = if (vod) PositionStore(ctx).inProgress() else UserPrefs(ctx).recents()
            urls.mapNotNull { byUrl[it] }
        }
    }
    val fresh by produceState(emptyList<Channel>(), items, kind) {
        value = withContext(Dispatchers.IO) { SeenStore(ctx).recent(kind, items) }
    }

    // البحث بيستنى 250ms بعد آخر حرف، والفلترة بتتم في الخلفية عشان القوائم الكبيرة
    var debounced by remember { mutableStateOf(query) }
    LaunchedEffect(query) {
        if (query != debounced) {
            delay(250)
            debounced = query
        }
    }
    val filtered by produceState(items, items, selected, debounced, favs, cont, fresh) {
        val q = debounced.trim()
        val sel = selected
        val fav = favs
        val contRank = cont.withIndex().associate { it.value.url to it.index }
        val newRank = fresh.withIndex().associate { it.value.url to it.index }
        value = withContext(Dispatchers.Default) {
            val list = items.filter {
                val inCategory = when (sel) {
                    null -> true
                    FAV_KEY -> it.url in fav
                    CONT_KEY -> it.url in contRank
                    NEW_KEY -> it.url in newRank
                    else -> it.group == sel
                }
                inCategory && (q.isEmpty() || it.name.contains(q, ignoreCase = true))
            }
            when (sel) {
                CONT_KEY -> list.sortedBy { contRank[it.url] ?: Int.MAX_VALUE }
                NEW_KEY -> list.sortedBy { newRank[it.url] ?: Int.MAX_VALUE }
                else -> list
            }
        }
    }
    val live = kind == Kind.Live
    val posters = kind == Kind.Movies || kind == Kind.Series
    val allLabel = if (live) stringResource(R.string.all_channels) else stringResource(R.string.all_items)
    val favLabel = stringResource(R.string.favorites)
    val contLabel = stringResource(R.string.continue_watching)
    val newLabel = stringResource(R.string.cat_recent)
    val kindTitle = stringResource(kind.titleRes)

    var catQuery by rememberSaveable(kind.name) { mutableStateOf("") }
    val shownGroups = remember(groups, catQuery) {
        val q = catQuery.trim()
        if (q.isEmpty()) groups else groups.filter { it.key.contains(q, ignoreCase = true) }
    }
    val compactH = LocalConfiguration.current.screenHeightDp < 480
    val sel = selected
    val title = when {
        sel == null -> allLabel
        sel == FAV_KEY -> favLabel
        sel == CONT_KEY -> contLabel
        sel == NEW_KEY -> newLabel
        else -> sel
    }
    val searchHint = stringResource(R.string.search_hint)

    // نتائج القسم (قنوات/بوسترات) — بتتعرض جوه اللوحة الملوّنة
    val results: @Composable () -> Unit = {
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    when {
                        items.isEmpty() -> stringResource(R.string.empty_none, kindTitle)
                        selected == FAV_KEY -> stringResource(R.string.empty_favs)
                        else -> stringResource(R.string.empty_nomatch)
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else if (posters) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(if (compactH) 92.dp else 128.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
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
    }

    val header: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.widthIn(max = 230.dp)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = if (compactH) 18.sp else 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("$kindTitle  ·  ${filtered.size}", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, maxLines = 1)
            }
            SearchPill(query, { query = it }, searchHint, Modifier.weight(1f).height(42.dp))
            if (live) CategoryChip(stringResource(R.string.guide), false) { onGuide(filtered) }
        }
    }

    val paneBrush = androidx.compose.ui.graphics.Brush.linearGradient(
        listOf(Cyan.copy(alpha = 0.55f), Violet.copy(alpha = 0.80f), Color(0xFF1B1446)),
    )
    val paneShape = RoundedCornerShape(28.dp)

    if (wide) {
        Row(Modifier.fillMaxSize().padding(top = 8.dp)) {
            CategoryPanel(
                modifier = Modifier.padding(start = 12.dp, bottom = 12.dp).width(if (compactH) 200.dp else 250.dp).fillMaxHeight(),
                catQuery = catQuery,
                onCatQuery = { catQuery = it },
                hint = searchHint,
                allLabel = allLabel,
                allCount = items.size,
                favLabel = favLabel,
                favCount = favCount,
                contLabel = contLabel,
                contCount = cont.size,
                newLabel = newLabel,
                newCount = fresh.size,
                groups = shownGroups,
                selected = selected,
                onSelect = { selected = it },
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .clip(paneShape)
                    .background(paneBrush)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), paneShape),
            ) {
                header()
                Box(Modifier.weight(1f).fillMaxWidth()) { results() }
            }
            if (huge && !posters) {
                val shown = highlighted?.takeIf { h -> filtered.any { it === h } } ?: filtered.firstOrNull()
                Box(Modifier.width(320.dp).fillMaxHeight().padding(end = 24.dp, bottom = 12.dp)) {
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
    } else {
        Column(Modifier.fillMaxSize().padding(top = 8.dp)) {
            header()
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                item { CategoryChip("$allLabel (${items.size})", selected == null) { selected = null } }
                item { CategoryChip("$favLabel ($favCount)", selected == FAV_KEY) { selected = FAV_KEY } }
                if (cont.isNotEmpty()) item { CategoryChip("$contLabel (${cont.size})", selected == CONT_KEY) { selected = CONT_KEY } }
                if (fresh.isNotEmpty()) item { CategoryChip("$newLabel (${fresh.size})", selected == NEW_KEY) { selected = NEW_KEY } }
                items(groups) { g ->
                    CategoryChip("${g.key} (${g.value})", selected == g.key) { selected = g.key }
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(paneShape)
                    .background(paneBrush),
            ) { results() }
        }
    }
}

/** لوحة التصنيفات: بحث في التصنيفات + الكل + المفضلة + المجموعات. */
@Composable
private fun CategoryPanel(
    modifier: Modifier,
    catQuery: String,
    onCatQuery: (String) -> Unit,
    hint: String,
    allLabel: String,
    allCount: Int,
    favLabel: String,
    favCount: Int,
    contLabel: String,
    contCount: Int,
    newLabel: String,
    newCount: Int,
    groups: List<Map.Entry<String, Int>>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .clip(shape)
            .background(Panel.copy(alpha = 0.65f))
            .border(1.dp, GlassBorder, shape),
    ) {
        SearchPill(catQuery, onCatQuery, hint, Modifier.padding(10.dp).fillMaxWidth().height(42.dp))
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 10.dp),
        ) {
            if (catQuery.isBlank()) {
                item { CategoryRow(allLabel, allCount, selected == null) { onSelect(null) } }
                item { CategoryRow(favLabel, favCount, selected == FAV_KEY) { onSelect(FAV_KEY) } }
                if (contCount > 0) item { CategoryRow(contLabel, contCount, selected == CONT_KEY) { onSelect(CONT_KEY) } }
                if (newCount > 0) item { CategoryRow(newLabel, newCount, selected == NEW_KEY) { onSelect(NEW_KEY) } }
            }
            items(groups) { g -> CategoryRow(g.key, g.value, selected == g.key) { onSelect(g.key) } }
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
