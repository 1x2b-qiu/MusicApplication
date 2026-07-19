package com.example.musicapp.domain.usecase.auth

import com.example.musicapp.domain.model.LoginState
import com.example.musicapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// 观察当前登录状态变化
class ObserveLoginStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    // 返回登录状态变化的 Flow
    operator fun invoke(): Flow<LoginState> = authRepository.observeLoginState()
}
