package com.leo.lune.domain.usecase.auth

import com.leo.lune.domain.model.LoginResult
import com.leo.lune.domain.repository.AuthRepository
import javax.inject.Inject

// 向手机号发送登录验证码
class SendCaptchaUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    // 校验手机号后发送验证码
    suspend operator fun invoke(phone: String): LoginResult {
        val trimmedPhone = phone.trim()
        if (trimmedPhone.isEmpty()) {
            return LoginResult(success = false, message = "请输入手机号")
        }
        return authRepository.sendCaptcha(trimmedPhone)
    }
}
