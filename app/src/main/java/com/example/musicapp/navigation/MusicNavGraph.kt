package com.example.musicapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.musicapp.ui.login.LoginScreen
import com.example.musicapp.ui.player.PlayerScreen
import com.example.musicapp.ui.register.RegisterScreen
import com.example.musicapp.ui.search.SearchScreen

@Composable
fun MusicNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MusicRoute.Search
    ) {
        composable<MusicRoute.Search> {
            SearchScreen(
                onSongClick = { song ->
                    navController.navigate(
                        MusicRoute.Player(
                            songId = song.id,
                            songName = song.name,
                            artistName = song.artists,
                            coverUrl = song.coverUrl.orEmpty()
                        )
                    )
                },
                onLoginClick = { navController.navigate(MusicRoute.Login) }
            )
        }

        composable<MusicRoute.Login> {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onRegisterClick = { navController.navigate(MusicRoute.Register) },
                onLoginSuccess = { navController.popBackStack() }
            )
        }

        composable<MusicRoute.Register> {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.popBackStack(MusicRoute.Search, inclusive = false)
                }
            )
        }

        composable<MusicRoute.Player> {
            PlayerScreen(
                onBack = { navController.popBackStack() },
                onLoginClick = { navController.navigate(MusicRoute.Login) }
            )
        }
    }
}
