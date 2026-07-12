package com.example.musicapp.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF4F2FB),
    onPrimary = Color(0xFF0E0E10),
    primaryContainer = Color(0x26B79BFF),
    onPrimaryContainer = Color(0xFFF4F2FB),
    secondaryContainer = Color(0x1AFFFFFF),
    onSecondaryContainer = Color(0xB3FFFFFF),
    tertiary = Color(0x997B5CFF),
    background = Color(0xFF0E0E10),
    onBackground = Color(0xFFF4F2FB),
    onSurface = Color(0xFFF4F2FB),
    onSurfaceVariant = Color(0xFFA29FB8),
    surface = Color(0x0FFFFFFF),
    surfaceVariant = Color(0x0AFFFFFF),
    surfaceContainerLow = Color(0x0DFFFFFF),
    surfaceContainerHigh = Color(0x1AFFFFFF),
    surfaceDim = Color(0x1AFFFFFF),
    surfaceBright = Color(0x26FFFFFF),
    surfaceContainerHighest = Color(0x40FFFFFF),
    outline = Color(0x66B79BFF),
    outlineVariant = Color(0x1FFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0E0E10),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0x267B5CFF),
    onPrimaryContainer = Color(0xFF1E1A2E),
    secondaryContainer = Color(0x14000000),
    onSecondaryContainer = Color(0x99000000),
    tertiary = Color(0x997B5CFF),
    background = Color(0xFFF5F4F8),
    onBackground = Color(0xFF1E1A2E),
    onSurface = Color(0xFF1E1A2E),
    onSurfaceVariant = Color(0xFF6B6880),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0EEF4),
    surfaceContainerLow = Color(0xFFEAE8F0),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE0DEE8),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFD8D5E2),
    outline = Color(0x667B5CFF),
    outlineVariant = Color(0x1A000000)
)

// App 主题入口；同时同步系统状态栏/导航栏图标颜色
@Composable
fun MusicApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                // 浅色主题用深色图标，深色主题用浅色图标
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
