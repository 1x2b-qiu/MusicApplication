package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.repository.ThemeRepository
import javax.inject.Inject

// 保存用户手动选择的固定主题
class SetUserThemeUseCase @Inject constructor(
    private val themeRepository: ThemeRepository
) {
    // darkTheme=true 深色模式，false 浅色模式
    suspend operator fun invoke(darkTheme: Boolean) {
        themeRepository.setUserTheme(darkTheme)
    }
}
