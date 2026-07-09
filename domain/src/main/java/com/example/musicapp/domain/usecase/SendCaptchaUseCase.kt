package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.model.LoginResult
import com.example.musicapp.domain.repository.AuthRepository
import javax.inject.Inject

class SendCaptchaUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phone: String): LoginResult {
        val trimmedPhone = phone.trim()
        if (trimmedPhone.isEmpty()) {
            return LoginResult(success = false, message = "请输入手机号")
        }
        return authRepository.sendCaptcha(trimmedPhone)
    }
}
