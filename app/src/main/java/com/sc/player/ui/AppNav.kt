package com.sc.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sc.player.data.PlaylistRepository
import com.sc.player.ui.add.AddPlaylistScreen
import com.sc.player.ui.home.HomeScreen

@Composable
fun AppNav() {
    val ctx = LocalContext.current
    val nav = rememberNavController()
    val start = remember {
        val first = PlaylistRepository(ctx.applicationContext).playlists().firstOrNull()
        if (first == null) "add" else "home/${first.id}"
    }
    NavHost(nav, startDestination = start) {
        composable("add") {
            AddPlaylistScreen(onDone = { id ->
                nav.navigate("home/$id") { popUpTo("add") { inclusive = true } }
            })
        }
        composable("home/{id}") { entry ->
            HomeScreen(entry.arguments?.getString("id").orEmpty())
        }
    }
}
