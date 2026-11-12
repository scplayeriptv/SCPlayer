package com.sc.player.ui.home

import com.sc.player.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.data.Channel
import com.sc.player.data.Kind
import com.sc.player.data.kind
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.Glass
import com.sc.player.ui.theme.GlassBorder
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val MAX_RESULTS = 300

/** بحث شامل في القنوات والأفلام والمسلسلات والراديو مرة واحدة. */
@Composable
fun SearchScreen(
    items: List<Channel>,
    favs: Set<String>,
    epgTick: Int,
    onToggleFav: (String) -> Unit,
    onPlay: (List<Channel>, Int) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var debounced by remember { mutableStateOf(query) }
    LaunchedEffect(query) {
        if (query != debounced) {
            delay(250)
            debounced = query
        }
    }
    val results by produceState(emptyList<Channel>(), items, debounced) {
        val q = debounced.trim()
        value = if (q.length < 2) emptyList() else withContext(Dispatchers.Default) {
            items.asSequence().filter { it.name.contains(q, ignoreCase = true) }.take(MAX_RESULTS).toList()
        }
    }

    Column(Modifier.fillMaxSize().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenTitle(stringResource(R.string.nav_search))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint), color = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
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
        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (debounced.trim().length < 2) stringResource(R.string.search_prompt) else stringResource(R.string.empty_nomatch),
                    color = TextMuted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(results) { i, ch ->
                    ChannelRow(
                        ch = ch,
                        now = if (ch.kind() == Kind.Live) epgNow(ch, epgTick) else null,
                        fav = ch.url in favs,
                        live = ch.kind() == Kind.Live,
                        onFocus = {},
                        onToggleFav = { onToggleFav(ch.url) },
                    ) { onPlay(results, i) }
                }
            }
        }
    }
}
