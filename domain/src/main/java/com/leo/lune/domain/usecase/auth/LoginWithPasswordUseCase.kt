package com.leo.lune.domain.usecase.auth

import com.leo.lune.domain.model.LoginResult
import com.leo.lune.domain.repository.AuthRepository
import javax.inject.Inject

// 使用手机号 + 密码登录
class LoginWithPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    // 校验输入后发起密码登录
    suspend operator fun invoke(phone: String, password: String): LoginResult {
        val trimmedPhone = phone.trim()
        val trimmedPassword = password.trim()
        if (trimmedPhone.isEmpty()) {
            return LoginResult(success = false, message = "请输入手机号")
        }
        if (trimmedPassword.isEmpty()) {
            return LoginResult(success = false, message = "请输入密码")
        }
        return authRepository.loginWithPassword(trimmedPhone, trimmedPassword)
    }
}
