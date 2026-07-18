package com.example.musicapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicapp.domain.model.ThemeSetting
import com.example.musicapp.navigation.MusicNavGraph
import com.example.musicapp.permission.PermissionCoordinator
import com.example.musicapp.ui.theme.MusicApplicationTheme
import com.example.musicapp.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 权限协调器：须在 STARTED 前完成 launcher 注册（字段初始化阶段）
    private val permissions = PermissionCoordinator(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissions.requestStartup()
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeSetting by themeViewModel.themeSetting.collectAsStateWithLifecycle()
            val systemDarkTheme = isSystemInDarkTheme()
            // DataStore 无记录时跟随系统；有记录时使用用户手动选择的主题
            val darkTheme = when (val setting = themeSetting) {
                ThemeSetting.FollowSystem -> systemDarkTheme
                is ThemeSetting.Fixed -> setting.darkTheme
            }
            MusicApplicationTheme(darkTheme = darkTheme) {
                MusicNavGraph(
                    darkTheme = darkTheme,
                    onToggleTheme = { themeViewModel.setUserTheme(!darkTheme) }
                )
            }
        }
    }
}
