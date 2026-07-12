package com.example.musicapp.domain.usecase

import com.example.musicapp.domain.model.LoginResult
import com.example.musicapp.domain.repository.AuthRepository
import javax.inject.Inject

// 使用手机号 + 密码登录
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    // 校验输入后发起密码登录
    suspend operator fun invoke(phone: String, password: String): LoginResult {
        val trimmedPhone = phone.trim()
        if (trimmedPhone.isEmpty()) {
            return LoginResult(success = false, message = "请输入手机号")
        }
        if (password.isEmpty()) {
            return LoginResult(success = false, message = "请输入密码")
        }
        return authRepository.login(trimmedPhone, password)
    }
}
