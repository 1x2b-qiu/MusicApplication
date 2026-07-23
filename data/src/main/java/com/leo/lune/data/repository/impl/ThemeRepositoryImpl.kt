package com.leo.lune.data.repository.impl

import com.leo.lune.data.local.ThemePreferences
import com.leo.lune.domain.model.ThemeSetting
import com.leo.lune.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

// 主题仓储实现：读写用户深浅色偏好
@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val themePreferences: ThemePreferences
) : ThemeRepository {

    // 观察当前主题设置
    override fun observeThemeSetting(): Flow<ThemeSetting> = themePreferences.themeSettingFlow

    // 保存用户选择的深浅色主题
    override suspend fun setUserTheme(darkTheme: Boolean) {
        themePreferences.setUserTheme(darkTheme)
    }
}
