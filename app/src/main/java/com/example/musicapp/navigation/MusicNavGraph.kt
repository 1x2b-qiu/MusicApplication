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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.musicapp.ui.component.sidebar.AppSidebar
import com.example.musicapp.ui.category.CategoryScreen
import com.example.musicapp.ui.downloads.DownloadsScreen
import com.example.musicapp.ui.home.HomeScreen
import com.example.musicapp.ui.liked.LikedScreen
import com.example.musicapp.ui.login.LoginScreen
import com.example.musicapp.ui.player.PlayerScreen
import com.example.musicapp.ui.radio.RadioScreen
import com.example.musicapp.ui.recent.RecentScreen
import com.example.musicapp.ui.search.SearchScreen
import com.example.musicapp.ui.settings.SettingsScreen
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
        // 在分类页 → 高亮「分类」
        currentDestination?.hasRoute<MusicRoute.Category>() == true -> MainTab.Category
        // 登录/播放器等非 Tab 页 → 默认高亮首页
        else -> MainTab.Home
    }
    // 仅在 Home / Radio / Category 三个 Tab 页显示底部 Tab 栏
    val showBottomTabBar = currentDestination?.hasRoute<MusicRoute.Home>() == true ||
            currentDestination?.hasRoute<MusicRoute.Radio>() == true ||
            currentDestination?.hasRoute<MusicRoute.Category>() == true
    // 搜索 / 喜欢 / 最近 / 本地下载：只显示迷你播放栏；Tab 页同时显示迷你播放栏与 Tab 栏
    val showMiniPlayerBar = showBottomTabBar ||
            currentDestination?.hasRoute<MusicRoute.Search>() == true ||
            currentDestination?.hasRoute<MusicRoute.Liked>() == true ||
            currentDestination?.hasRoute<MusicRoute.Recent>() == true ||
            currentDestination?.hasRoute<MusicRoute.Downloads>() == true

    // 创建 Haze 模糊状态，供底部 Tab 栏 / 侧边栏做玻璃磨砂背景
    val hazeState = rememberHazeState()
    // 侧边栏开关（昵称 / 头像由 SidebarViewModel 自行订阅）
    var sidebarOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 模糊源铺满全屏（含状态栏与左右边缘），侧边栏顶部/左缘才能采到内容；
        // 状态栏避让与左右边距下沉到 NavHost / 底部栏自身
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            NavHost(
                navController = navController,
                startDestination = MusicRoute.Splash,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
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
                            navController.navigate(MusicRoute.Downloads) {
                                launchSingleTop = true
                            }
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

        // 侧边栏铺满全屏（不受内容区 16dp 边距约束）
        AppSidebar(
            open = sidebarOpen,
            onDismiss = { sidebarOpen = false },
            darkTheme = darkTheme,
            onMenuClick = { id ->
                when (id) {
                    "download" -> navController.navigate(MusicRoute.Downloads) {
                        launchSingleTop = true
                    }
                    "settings" -> navController.navigate(MusicRoute.Settings) {
                        launchSingleTop = true
                    }
                }
            }
        )
    }
}
