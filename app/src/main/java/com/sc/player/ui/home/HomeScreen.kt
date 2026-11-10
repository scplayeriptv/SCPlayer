package com.sc.player.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sc.player.data.Channel
import com.sc.player.data.PlaylistRepository
import com.sc.player.data.UserPrefs
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.Ink
import com.sc.player.ui.theme.ScreenBackground
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

private const val FAV_KEY = "__fav__"
private val Gold = Color(0xFFFBBF24)

@Composable
fun HomeScreen(playlistId: String, onPlay: (List<Channel>, Int) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { PlaylistRepository(ctx.applicationContext) }
    val prefs = remember { UserPrefs(ctx) }
    val loaded by produceState<List<Channel>?>(null, playlistId) { value = repo.channels(playlistId) }

    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var favs by remember { mutableStateOf(prefs.favorites()) }
    val recentUrls = remember { prefs.recents() }

    fun play(list: List<Channel>, i: Int) {
        prefs.addRecent(list[i].url)
        onPlay(list, i)
    }

    ScreenBackground {
        val channels = loaded
        if (channels == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("جاري تحميل القنوات...", color = TextMuted, fontSize = 16.sp)
            }
            return@ScreenBackground
        }

        val groups = remember(channels) { channels.groupingBy { it.group }.eachCount().entries.toList() }
        val favCount = remember(channels, favs) { channels.count { it.url in favs } }
        val byUrl = remember(channels) { channels.associateBy { it.url } }
        val recents = remember(channels) { recentUrls.mapNotNull { byUrl[it] } }
        val filtered = remember(channels, selected, query, favs) {
            val q = query.trim()
            channels.filter {
                val inCategory = when (selected) {
                    null -> true
                    FAV_KEY -> it.url in favs
                    else -> it.group == selected
                }
                inCategory && (q.isEmpty() || it.name.contains(q, ignoreCase = true))
            }
        }
        val showRecents = recents.isNotEmpty() && selected == null && query.isBlank()

        Column(Modifier.fillMaxSize().padding(top = 20.dp)) {
            Header(query = query, onQuery = { query = it }, count = filtered.size)

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 14.dp),
            ) {
                item {
                    CategoryChip("الكل (${channels.size})", selected == null) { selected = null }
                }
                item {
                    CategoryChip("★ المفضلة ($favCount)", selected == FAV_KEY) { selected = FAV_KEY }
                }
                items(groups) { g ->
                    CategoryChip("${g.key} (${g.value})", selected == g.key) { selected = g.key }
                }
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (selected == FAV_KEY) "مفيش مفضلة لسه. اضغط ☆ على أي قناة." else "مفيش قنوات مطابقة.",
                        color = TextMuted,
                        fontSize = 16.sp,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (showRecents) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionTitle("آخر ما شاهدته")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    itemsIndexed(recents) { i, ch ->
                                        Box(Modifier.width(150.dp)) {
                                            ChannelCard(
                                                ch = ch,
                                                fav = ch.url in favs,
                                                onToggleFav = { favs = prefs.toggleFavorite(ch.url) },
                                            ) { play(recents, i) }
                                        }
                                    }
                                }
                                SectionTitle("كل القنوات")
                            }
                        }
                    }
                    itemsIndexed(filtered) { i, ch ->
                        ChannelCard(
                            ch = ch,
                            fav = ch.url in favs,
                            onToggleFav = { favs = prefs.toggleFavorite(ch.url) },
                        ) { play(filtered, i) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun Header(query: String, onQuery: (String) -> Unit, count: Int) {
    Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "SC Player",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(brush = BrandBrush),
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                placeholder = { Text("ابحث عن قناة...", color = TextMuted) },
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
        }
        Text("$count قناة · اضغط ☆ أو اضغط مطوّلًا على القناة للمفضلة", color = TextMuted, fontSize = 13.sp)
    }
}

@Composable
private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    val look = if (selected) Modifier.background(BrandBrush) else Modifier.background(Glass)
    Box(
        Modifier
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer { scaleX = if (focused) 1.06f else 1f; scaleY = if (focused) 1.06f else 1f }
            .clip(shape)
            .then(look)
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else GlassBorder, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) Ink else TextMain,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelCard(ch: Channel, fav: Boolean, onToggleFav: () -> Unit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer { scaleX = if (focused) 1.05f else 1f; scaleY = if (focused) 1.05f else 1f }
            .clip(shape)
            .background(Glass)
            .border(if (focused) 2.dp else 1.dp, if (focused) Cyan else GlassBorder, shape)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onToggleFav),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                var failed by remember(ch.logo) { mutableStateOf(false) }
                if (ch.logo == null || failed) {
                    Box(Modifier.size(60.dp).clip(CircleShape).background(BrandBrush), contentAlignment = Alignment.Center) {
                        Text(
                            ch.name.trim().take(1).uppercase(),
                            color = Ink,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                } else {
                    AsyncImage(
                        model = ch.logo,
                        contentDescription = ch.name,
                        contentScale = ContentScale.Fit,
                        onError = { failed = true },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Text(
                ch.name,
                color = TextMain,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                ch.group,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggleFav),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (fav) "★" else "☆", color = if (fav) Gold else TextMuted, fontSize = 20.sp)
        }
    }
}
