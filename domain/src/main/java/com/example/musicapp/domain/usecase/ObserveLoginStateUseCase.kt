package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.model.LoginState
import com.example.musicapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLoginStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<LoginState> = authRepository.observeLoginState()
}
