package com.sc.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sc.player.data.PlaylistRepository
import com.sc.player.ui.add.AddPlaylistScreen
import com.sc.player.ui.home.HomeScreen
import com.sc.player.ui.splash.SplashScreen

@Composable
fun AppNav() {
    val ctx = LocalContext.current
    val nav = rememberNavController()
    NavHost(nav, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onFinish = {
                val first = PlaylistRepository(ctx.applicationContext).playlists().firstOrNull()
                val dest = if (first == null) "add" else "home/${first.id}"
                nav.navigate(dest) { popUpTo("splash") { inclusive = true } }
            })
        }
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
