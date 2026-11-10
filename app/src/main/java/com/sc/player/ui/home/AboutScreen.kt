package com.sc.player.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sc.player.ui.theme.BrandBrush
import com.sc.player.ui.theme.SCLogo
import com.sc.player.ui.theme.TextMain
import com.sc.player.ui.theme.TextMuted

@Composable
fun AboutScreen() {
    val ctx = LocalContext.current
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        SCLogo(96.dp)
        Text("SC Player", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, style = TextStyle(brush = BrandBrush))
        Text("Your IPTV Partner", color = TextMuted, fontSize = 16.sp)
        Text("Version $version", color = TextMain, fontSize = 14.sp)
        Text(
            "SC Player is a media player only. It does not provide, host or sell any channels or content. " +
                "Add your own M3U or Xtream playlist from a provider you have the right to use.",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
fun ComingSoonScreen(title: String, note: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Text(title, color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text("Coming soon", style = TextStyle(brush = BrandBrush), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(note, color = TextMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}
