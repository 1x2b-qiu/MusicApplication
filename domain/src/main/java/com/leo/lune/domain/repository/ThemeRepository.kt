package com.leo.lune.domain.repository

import com.leo.lune.domain.model.ThemeSetting
import kotlinx.coroutines.flow.Flow

// 主题设置仓储接口，持久化用户的深浅色偏好
interface ThemeRepository {
    // 观察当前主题设置（跟随系统或固定模式）
    fun observeThemeSetting(): Flow<ThemeSetting>

    // 保存用户手动选择的固定主题
    suspend fun setUserTheme(darkTheme: Boolean)
}
