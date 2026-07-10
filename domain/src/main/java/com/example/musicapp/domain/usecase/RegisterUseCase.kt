package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.model.LoginResult
import com.example.musicapp.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        phone: String,
        captcha: String,
        password: String,
        confirmPassword: String,
        nickname: String
    ): LoginResult {
        val trimmedPhone = phone.trim()
        val trimmedCaptcha = captcha.trim()
        val trimmedNickname = nickname.trim()

        if (trimmedPhone.isEmpty()) {
            return LoginResult(success = false, message = "请输入手机号")
        }
        if (!PHONE_REGEX.matches(trimmedPhone)) {
            return LoginResult(success = false, message = "请输入正确的手机号")
        }
        if (trimmedCaptcha.isEmpty()) {
            return LoginResult(success = false, message = "请输入验证码")
        }
        if (trimmedNickname.isEmpty()) {
            return LoginResult(success = false, message = "请输入昵称")
        }
        if (password.length < 6) {
            return LoginResult(success = false, message = "密码至少 6 位")
        }
        if (password != confirmPassword) {
            return LoginResult(success = false, message = "两次输入的密码不一致")
        }

        return authRepository.register(
            phone = trimmedPhone,
            captcha = trimmedCaptcha,
            password = password,
            nickname = trimmedNickname
        )
    }

    companion object {
        private val PHONE_REGEX = Regex("^1[3-9]\\d{9}$")
    }
}
