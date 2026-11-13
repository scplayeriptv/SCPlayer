package com.sc.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sc.player.data.PlayQueue
import com.sc.player.data.PlaylistRepository
import com.sc.player.data.UserPrefs
import com.sc.player.ui.add.AddPlaylistScreen
import com.sc.player.ui.guide.GuideScreen
import com.sc.player.ui.home.HomeScreen
import com.sc.player.ui.multi.MultiViewScreen
import com.sc.player.ui.onboarding.OnboardingScreen
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.sc.player.ui.player.PlayerScreen
import com.sc.player.ui.playlists.PlaylistsScreen
import com.sc.player.ui.splash.SplashScreen

@Composable
fun AppNav() {
    val ctx = LocalContext.current
    val nav = rememberNavController()
    val prefs = remember { UserPrefs(ctx) }

    /** يفتح الرئيسية لقائمة معيّنة ويصفّر الـ back stack (الرجوع منها بيقفل التطبيق). */
    fun openHome(id: String) {
        prefs.setActive(id)
        nav.navigate("home/$id") { popUpTo(nav.graph.id) { inclusive = true } }
    }

    fun afterIntro() {
        val all = PlaylistRepository(ctx.applicationContext).playlists()
        val target = all.firstOrNull { it.id == prefs.activePlaylist() } ?: all.firstOrNull()
        if (target == null) {
            nav.navigate("add") { popUpTo(nav.graph.id) { inclusive = true } }
        } else {
            openHome(target.id)
        }
    }

    NavHost(
        nav,
        startDestination = "splash",
        enterTransition = { fadeIn(tween(260)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(260)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) {
        composable("splash") {
            SplashScreen(onFinish = {
                if (!prefs.onboarded()) {
                    nav.navigate("onboarding") { popUpTo("splash") { inclusive = true } }
                } else {
                    afterIntro()
                }
            })
        }
        composable("onboarding") {
            OnboardingScreen(onDone = {
                prefs.setOnboarded()
                afterIntro()
            })
        }
        composable("add") {
            AddPlaylistScreen(
                onDone = { id -> openHome(id) },
                onBack = if (nav.previousBackStackEntry != null) ({ nav.popBackStack() }) else null,
            )
        }
        composable("playlists") {
            PlaylistsScreen(
                activeId = prefs.activePlaylist(),
                onOpen = { id -> openHome(id) },
                onAdd = { nav.navigate("add") },
                onBack = { nav.popBackStack() },
                onEmpty = { nav.navigate("add") { popUpTo(nav.graph.id) { inclusive = true } } },
            )
        }
        composable("home/{id}") { entry ->
            HomeScreen(
                playlistId = entry.arguments?.getString("id").orEmpty(),
                onPlay = { list, i ->
                    PlayQueue.list = list
                    nav.navigate("player/$i")
                },
                onPlaylists = { nav.navigate("playlists") },
                onGuide = { list ->
                    PlayQueue.list = list
                    nav.navigate("guide")
                },
                onMulti = { list ->
                    PlayQueue.list = list
                    nav.navigate("multi")
                },
            )
        }
        composable("multi") {
            MultiViewScreen(
                onFullscreen = { i -> nav.navigate("player/$i") },
                onBack = { nav.popBackStack() },
            )
        }
        composable("guide") {
            GuideScreen(
                onPlay = { i -> nav.navigate("player/$i") },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            "player/{index}",
            arguments = listOf(navArgument("index") { type = NavType.IntType }),
        ) { entry ->
            PlayerScreen(
                startIndex = entry.arguments?.getInt("index") ?: 0,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
