package com.example.musicapp.domain.usecase.auth

import com.example.musicapp.domain.model.LoginResult
import com.example.musicapp.domain.repository.AuthRepository
import javax.inject.Inject

// 使用手机号 + 验证码登录
class LoginWithCaptchaUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    // 校验输入后发起验证码登录
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
