package com.example.musicapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import com.example.musicapp.domain.model.Song
import com.example.musicapp.ui.component.bottombar.BottomTabBar
import com.example.musicapp.ui.component.minplayer.MiniPlayerBar
import com.example.musicapp.ui.home.HomeColors
import com.example.musicapp.ui.home.HomeScreen
import com.example.musicapp.ui.login.LoginScreen
import com.example.musicapp.ui.player.PlayerScreen
import com.example.musicapp.ui.profile.ProfileScreen
import com.example.musicapp.ui.radio.RadioScreen
import com.example.musicapp.ui.register.RegisterScreen
import com.example.musicapp.ui.search.SearchScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun MusicNavGraph() {
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
        // 登录/注册/播放器等非 Tab 页 → 默认高亮首页
        else -> MainTab.Home
    }
    // 仅在 Home / Radio / Profile 三个 Tab 页显示底部迷你播放栏和 Tab 栏
    val showBottomChrome = currentDestination?.hasRoute<MusicRoute.Home>() == true ||
            currentDestination?.hasRoute<MusicRoute.Radio>() == true ||
            currentDestination?.hasRoute<MusicRoute.Profile>() == true

    // 创建 Haze 模糊状态，供底部 Tab 栏做玻璃磨砂背景
    val hazeState = rememberHazeState()


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeColors.Background)
    ) {
        NavHost(
            navController = navController,
            startDestination = MusicRoute.Home,
            modifier = Modifier.hazeSource(state = hazeState)
        ) {
            composable<MusicRoute.Home> {
                HomeScreen(
                    onSearchClick = {
                        navController.navigate(MusicRoute.Search) {
                            popUpTo(MusicRoute.Home) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onLoginClick = { navController.navigate(MusicRoute.Login) }
                )
            }

            composable<MusicRoute.Radio> {
                RadioScreen()
            }

            composable<MusicRoute.Profile> {
                ProfileScreen(
                    onLoginClick = { navController.navigate(MusicRoute.Login) }
                )
            }

            composable<MusicRoute.Search> {
                SearchScreen(
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
                        navController.popBackStack(MusicRoute.Home, inclusive = false)
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

        if (showBottomChrome) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
            ) {
                MiniPlayerBar(onPlayerClick = { song ->
                    navController.navigate(
                        MusicRoute.Player(
                            songId = song.id,
                            songName = song.name,
                            artistName = song.artists,
                            coverUrl = song.coverUrl.orEmpty()
                        )
                    )
                })
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
