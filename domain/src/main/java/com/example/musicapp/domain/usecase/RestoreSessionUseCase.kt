package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.repository.AuthRepository
import javax.inject.Inject

class RestoreSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.restoreSession()
    }
}
