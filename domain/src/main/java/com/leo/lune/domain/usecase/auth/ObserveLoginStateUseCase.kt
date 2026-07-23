package com.leo.lune.domain.usecase.auth

import com.leo.lune.domain.model.LoginState
import com.leo.lune.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// 观察当前登录状态变化
class ObserveLoginStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    // 返回登录状态变化的 Flow
    operator fun invoke(): Flow<LoginState> = authRepository.observeLoginState()
}
