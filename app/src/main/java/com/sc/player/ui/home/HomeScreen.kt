package com.sc.player.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.data.Channel
import com.sc.player.data.PlaylistRepository
import com.sc.player.ui.theme.ScreenBackground
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

/** Temporary home: lists the parsed channels to prove the add-playlist flow works. */
@Composable
fun HomeScreen(playlistId: String) {
    val ctx = LocalContext.current
    val repo = remember { PlaylistRepository(ctx.applicationContext) }
    val channels by produceState(emptyList<Channel>(), playlistId) { value = repo.channels(playlistId) }

    ScreenBackground {
        Column(Modifier.fillMaxSize().padding(32.dp)) {
            Text("SC Player", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextMain)
            Text("${channels.size} قناة", color = TextMuted, modifier = Modifier.padding(bottom = 16.dp))
            LazyColumn {
                items(channels) { ch ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text(ch.name, color = TextMain, fontWeight = FontWeight.SemiBold)
                        Text(ch.group, color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
