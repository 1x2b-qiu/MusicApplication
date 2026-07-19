package com.example.musicapp.domain.usecase.auth

import com.example.musicapp.domain.repository.AuthRepository
import javax.inject.Inject

// 退出登录并清除本地会话
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    // 清除本地会话并退出登录
    suspend operator fun invoke() {
        authRepository.logout()
    }
}
