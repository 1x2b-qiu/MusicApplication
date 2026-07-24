package com.leo.lune.data.repository.impl

import com.leo.lune.data.local.AuthPreferences
import com.leo.lune.data.remote.api.NeteaseApi
import com.leo.lune.data.remote.response.LoginResponse
import com.leo.lune.data.session.SessionCookieHolder
import com.leo.lune.data.util.parseNeteaseCookie
import com.leo.lune.domain.model.LoginResult
import com.leo.lune.domain.model.LoginState
import com.leo.lune.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

// 认证仓储实现
// 负责验证码登录、会话恢复与登出，并持久化 Cookie 与用户信息
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val neteaseApi: NeteaseApi,
    private val authPreferences: AuthPreferences,
    private val cookieHolder: SessionCookieHolder
) : AuthRepository {

    // 观察本地持久化的登录状态
    override fun observeLoginState(): Flow<LoginState> = authPreferences.loginStateFlow

    // 启动时从 DataStore 恢复 Cookie 到内存，供后续请求携带
    override suspend fun restoreSession() {
        val cookie = authPreferences.getCookie()
        cookieHolder.set(cookie)
    }

    // 向手机号发送登录验证码
    override suspend fun sendCaptcha(phone: String): LoginResult {
        return runCatching {
            val response = neteaseApi.sendCaptcha(phone = phone)
            if (response.code == 200 && response.data == true) {
                LoginResult(success = true, message = "验证码已发送")
            } else {
                LoginResult(
                    success = false,
                    message = response.message ?: "发送验证码失败（code=${response.code}）"
                )
            }
        }.getOrElse { throwable ->
            LoginResult(
                success = false,
                message = throwable.message ?: "发送验证码失败，请检查网络和 API 服务"
            )
        }
    }

    // 使用手机号 + 验证码登录
    override suspend fun loginWithCaptcha(phone: String, captcha: String): LoginResult {
        return runCatching {
            val response = neteaseApi.loginCellphoneWithCaptcha(
                phone = phone,
                captcha = captcha
            )
            handleLoginResponse(response)
        }.getOrElse { throwable ->
            LoginResult(
                success = false,
                message = throwable.message ?: "登录失败，请检查网络和 API 服务"
            )
        }
    }

    // 使用手机号 + 密码登录
    override suspend fun loginWithPassword(phone: String, password: String): LoginResult {
        return runCatching {
            val response = neteaseApi.loginCellphoneWithPassword(
                phone = phone,
                password = password
            )
            handleLoginResponse(response)
        }.getOrElse { throwable ->
            LoginResult(
                success = false,
                message = throwable.message ?: "登录失败，请检查网络和 API 服务"
            )
        }
    }

    // 调用服务端登出并清除本地 Cookie 与用户信息
    override suspend fun logout() {
        runCatching { neteaseApi.logout() }
        cookieHolder.set(null)
        authPreferences.clear()
    }

    // 解析登录响应：校验 code、提取 Cookie 并写入内存与 DataStore
    private suspend fun handleLoginResponse(response: LoginResponse): LoginResult {
        if (response.code != 200) {
            return LoginResult(
                success = false,
                message = response.message ?: "登录失败（code=${response.code}）"
            )
        }
        val cookie = parseNeteaseCookie(response.cookie)
        if (cookie.isBlank()) {
            return LoginResult(success = false, message = "登录失败：未获取到 Cookie")
        }
        val nickname = response.profile?.nickname
        val userId = response.profile?.userId
        val avatarUrl = response.profile?.avatarUrl
        cookieHolder.set(cookie)
        authPreferences.saveLogin(cookie, nickname, userId, avatarUrl)
        return LoginResult(success = true, nickname = nickname)
    }
}
