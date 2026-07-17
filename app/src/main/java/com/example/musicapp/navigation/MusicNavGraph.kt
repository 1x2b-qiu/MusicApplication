package com.example.musicapp.navigation

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicapp.ui.component.bottombar.BottomTabBar
import com.example.musicapp.ui.component.minplayer.MiniPlayerBar
import com.example.musicapp.ui.home.HomeScreen
import com.example.musicapp.ui.liked.LikedScreen
import com.example.musicapp.ui.login.LoginScreen
import com.example.musicapp.ui.player.PlayerScreen
import com.example.musicapp.ui.profile.ProfileScreen
import com.example.musicapp.ui.radio.RadioScreen
import com.example.musicapp.ui.recent.RecentScreen
import com.example.musicapp.ui.search.SearchScreen
import com.example.musicapp.ui.splash.SplashScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun MusicNavGraph(
    darkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    // 创建并记住 NavController，负责页面跳转与返回栈管理
    val navController = rememberNavController()
    // 订阅当前导航栈顶页面，页面切换时会自动重组
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    // 从栈顶条目取出当前目的地，用于判断在哪个页面
    val currentDestination = navBackStackEntry?.destination
    // 根据当前路由，计算底部 Tab 栏应高亮哪一项
    val selectedTab = when {
        // 在首页 → 高亮「首页」
        currentDestination?.hasRoute<MusicRoute.Home>() == true -> MainTab.Home
        // 在电台页 → 高亮「电台」
        currentDestination?.hasRoute<MusicRoute.Radio>() == true -> MainTab.Radio
        // 在我的页 → 高亮「我的」
        currentDestination?.hasRoute<MusicRoute.Profile>() == true -> MainTab.Profile
        // 登录/播放器等非 Tab 页 → 默认高亮首页
        else -> MainTab.Home
    }
    // 仅在 Home / Radio / Profile 三个 Tab 页显示底部 Tab 栏
    val showBottomTabBar = currentDestination?.hasRoute<MusicRoute.Home>() == true ||
            currentDestination?.hasRoute<MusicRoute.Radio>() == true ||
            currentDestination?.hasRoute<MusicRoute.Profile>() == true
    // 搜索页 / 我喜欢的 / 最近播放：只显示迷你播放栏；Tab 页同时显示迷你播放栏与 Tab 栏
    val showMiniPlayerBar = showBottomTabBar ||
            currentDestination?.hasRoute<MusicRoute.Search>() == true ||
            currentDestination?.hasRoute<MusicRoute.Liked>() == true ||
            currentDestination?.hasRoute<MusicRoute.Recent>() == true

    // 创建 Haze 模糊状态，供底部 Tab 栏做玻璃磨砂背景
    val hazeState = rememberHazeState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        NavHost(
            navController = navController,
            startDestination = MusicRoute.Splash,
            modifier = Modifier.hazeSource(state = hazeState)
        ) {
            composable<MusicRoute.Splash> {
                SplashScreen(
                    onSuccess = {
                        // Splash / 登录成功后进入首页，并清空 auth 栈（含 Splash）
                        navController.navigate(MusicRoute.Home) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onFail = {
                        // Splash 未登录时进入登录页，并移除 Splash，避免返回
                        navController.navigate(MusicRoute.Login) {
                            popUpTo<MusicRoute.Splash> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable<MusicRoute.Login> {
                LoginScreen(
                    onBack = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        }
                    },
                    onLoginSuccess = {
                        // Splash / 登录成功后进入首页，并清空 auth 栈（含 Splash）
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
                        navController.navigate(MusicRoute.Liked) {
                            launchSingleTop = true
                        }
                    },
                    onRecentClick = {
                        navController.navigate(MusicRoute.Recent) {
                            launchSingleTop = true
                        }
                    },
                    onLoginClick = {
                        navController.navigate(MusicRoute.Login) {
                            launchSingleTop = true
                        }
                    },
                    darkTheme = darkTheme,
                    onToggleTheme = onToggleTheme
                )
            }

            composable<MusicRoute.Radio> {
                RadioScreen()
            }

            composable<MusicRoute.Profile> {
                ProfileScreen(
                    onLoginClick = {
                        navController.navigate(MusicRoute.Login) {
                            launchSingleTop = true
                        }
                    },
                    onLoggedOut = {
                        // 退出登录后回到登录页，并清空主界面栈
                        navController.navigate(MusicRoute.Login) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
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
                    onBack = { navController.popBackStack() }
                )
            }
        }

        if (showMiniPlayerBar) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                MiniPlayerBar(
                    hazeState = hazeState,
                    onPlayerClick = {
                        navController.navigate(MusicRoute.Player) {
                            launchSingleTop = true
                        }
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
    }
}
