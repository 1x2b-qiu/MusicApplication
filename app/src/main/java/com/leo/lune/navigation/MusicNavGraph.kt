package com.leo.lune.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.leo.lune.ui.component.bottombar.BottomTabBar
import com.leo.lune.ui.component.minplayer.MiniPlayerBar
import com.leo.lune.ui.component.sidebar.AppSidebar
import com.leo.lune.ui.category.CategoryScreen
import com.leo.lune.ui.downloads.DownloadsScreen
import com.leo.lune.ui.home.HomeScreen
import com.leo.lune.ui.liked.LikedScreen
import com.leo.lune.ui.login.LoginScreen
import com.leo.lune.ui.player.PlayerScreen
import com.leo.lune.ui.radio.RadioScreen
import com.leo.lune.ui.recent.RecentScreen
import com.leo.lune.ui.search.SearchScreen
import com.leo.lune.ui.settings.SettingsScreen
import com.leo.lune.ui.startup.SessionBootstrapViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun MusicNavGraph(
    darkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    bootstrapViewModel: SessionBootstrapViewModel = hiltViewModel()
) {
    val startRoute by bootstrapViewModel.startRoute.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 会话恢复完成后再建 NavHost，保证 Cookie 已就绪且 startDestination 正确
        val route = startRoute
        if (route != null) {
            MusicNavHost(
                startRoute = route,
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme
            )
        }
    }
}

@Composable
private fun MusicNavHost(
    startRoute: MusicRoute,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val selectedTab = when {
        currentDestination?.hasRoute<MusicRoute.Home>() == true -> MainTab.Home
        currentDestination?.hasRoute<MusicRoute.Radio>() == true -> MainTab.Radio
        currentDestination?.hasRoute<MusicRoute.Category>() == true -> MainTab.Category
        else -> MainTab.Home
    }
    val showBottomTabBar = currentDestination?.hasRoute<MusicRoute.Home>() == true ||
            currentDestination?.hasRoute<MusicRoute.Radio>() == true ||
            currentDestination?.hasRoute<MusicRoute.Category>() == true
    val showMiniPlayerBar = showBottomTabBar ||
            currentDestination?.hasRoute<MusicRoute.Search>() == true ||
            currentDestination?.hasRoute<MusicRoute.Liked>() == true ||
            currentDestination?.hasRoute<MusicRoute.Recent>() == true ||
            currentDestination?.hasRoute<MusicRoute.Downloads>() == true

    val hazeState = rememberHazeState()
    var sidebarOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            NavHost(
                navController = navController,
                startDestination = startRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                composable<MusicRoute.Login> {
                    LoginScreen(
                        onBack = {
                            if (navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                        },
                        onLoginSuccess = {
                            navController.navigate(MusicRoute.Home) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable<MusicRoute.Home> {
                    HomeScreen(
                        hazeState = hazeState,
                        onSearchClick = {
                            navController.navigate(MusicRoute.Search) {
                                popUpTo(MusicRoute.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onLikedClick = {
                            navController.navigateSingleTopTo(MusicRoute.Liked)
                        },
                        onRecentClick = {
                            navController.navigateSingleTopTo(MusicRoute.Recent)
                        },
                        onLoginClick = {
                            navController.navigateSingleTopTo(MusicRoute.Login)
                        },
                        onOpenSidebar = { sidebarOpen = true },
                        darkTheme = darkTheme,
                        onToggleTheme = onToggleTheme
                    )
                }

                composable<MusicRoute.Radio> {
                    RadioScreen()
                }

                composable<MusicRoute.Category> {
                    CategoryScreen()
                }

                composable<MusicRoute.Settings> {
                    SettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<MusicRoute.Downloads> {
                    DownloadsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<MusicRoute.Search> {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        darkTheme = darkTheme,
                        hazeState = hazeState
                    )
                }

                composable<MusicRoute.Liked> {
                    LikedScreen(
                        onBack = { navController.popBackStack() },
                        darkTheme = darkTheme,
                        hazeState = hazeState
                    )
                }

                composable<MusicRoute.Recent> {
                    RecentScreen(
                        onBack = { navController.popBackStack() },
                        darkTheme = darkTheme,
                        hazeState = hazeState
                    )
                }

                composable<MusicRoute.Player>(
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Up,
                            animationSpec = tween(320)
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Down,
                            animationSpec = tween(280)
                        )
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Up,
                            animationSpec = tween(320)
                        )
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Down,
                            animationSpec = tween(280)
                        )
                    }
                ) {
                    PlayerScreen(
                        onBack = { navController.popBackStack() },
                        onDownloadsClick = {
                            navController.navigateSingleTopTo(MusicRoute.Downloads)
                        }
                    )
                }
            }
        }

        if (showMiniPlayerBar) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
            ) {
                MiniPlayerBar(
                    hazeState = hazeState,
                    onPlayerClick = {
                        navController.navigateSingleTopTo(MusicRoute.Player)
                    }
                )
                if (showBottomTabBar) {
                    BottomTabBar(
                        hazeState = hazeState,
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            navController.navigate(tab.toRoute()) {
                                popUpTo(MusicRoute.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }

        AppSidebar(
            open = sidebarOpen,
            onDismiss = { sidebarOpen = false },
            darkTheme = darkTheme,
            onMenuClick = { id ->
                when (id) {
                    "download" -> navController.navigateSingleTopTo(MusicRoute.Downloads)
                    "settings" -> navController.navigateSingleTopTo(MusicRoute.Settings)
                }
            }
        )
    }
}

// 若目标已在返回栈中则 pop 到该页，否则再 navigate。
// 避免 Player ↔ Downloads 等互相跳转时同页无限叠加。
inline fun <reified T : Any> NavController.navigateSingleTopTo(route: T) {
    if (popBackStack<T>(inclusive = false)) return
    navigate(route) {
        launchSingleTop = true
    }
}
