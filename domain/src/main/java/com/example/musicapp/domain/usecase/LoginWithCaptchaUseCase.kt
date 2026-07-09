package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.model.LoginResult
import com.example.musicapp.domain.repository.AuthRepository
import javax.inject.Inject

class LoginWithCaptchaUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phone: String, captcha: String): LoginResult {
        val trimmedPhone = phone.trim()
        val trimmedCaptcha = captcha.trim()
        if (trimmedPhone.isEmpty()) {
            return LoginResult(success = false, message = "请输入手机号")
        }
        if (trimmedCaptcha.isEmpty()) {
            return LoginResult(success = false, message = "请输入验证码")
        }
        return authRepository.loginWithCaptcha(trimmedPhone, trimmedCaptcha)
    }
}
