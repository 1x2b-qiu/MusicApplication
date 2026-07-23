package com.leo.lune.domain.usecase.auth

import com.leo.lune.domain.repository.AuthRepository
import javax.inject.Inject

// 从本地存储恢复上次登录会话
class RestoreSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    // 从本地 DataStore 恢复 Cookie 和用户信息
    suspend operator fun invoke() {
        authRepository.restoreSession()
    }
}
