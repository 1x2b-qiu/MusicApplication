package com.leo.lune.domain.repository

import com.leo.lune.domain.model.LoginResult
import com.leo.lune.domain.model.LoginState
import kotlinx.coroutines.flow.Flow

// 认证仓储接口，负责登录态持久化与账号操作
interface AuthRepository {
    // 观察当前登录状态变化
    fun observeLoginState(): Flow<LoginState>
    // 从本地存储恢复上次登录会话
    suspend fun restoreSession()
    // 向手机号发送验证码
    suspend fun sendCaptcha(phone: String): LoginResult
    // 使用手机号 + 验证码登录
    suspend fun loginWithCaptcha(phone: String, captcha: String): LoginResult
    // 退出登录并清除本地会话
    suspend fun logout()
}
