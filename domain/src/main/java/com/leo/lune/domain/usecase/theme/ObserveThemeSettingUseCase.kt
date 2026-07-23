package com.leo.lune.domain.usecase.theme

import com.leo.lune.domain.model.ThemeSetting
import com.leo.lune.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// 观察当前主题设置（跟随系统或固定深浅色）
class ObserveThemeSettingUseCase @Inject constructor(
    private val themeRepository: ThemeRepository
) {
    // 返回主题设置变化的 Flow
    operator fun invoke(): Flow<ThemeSetting> = themeRepository.observeThemeSetting()
}
