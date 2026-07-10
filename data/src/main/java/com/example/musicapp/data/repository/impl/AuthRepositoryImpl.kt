package com.example.musicapp.data.repository.impl

import com.example.musicapp.data.local.AuthPreferences
import com.example.musicapp.data.remote.api.NeteaseApi
import com.example.musicapp.data.remote.response.LoginResponse
import com.example.musicapp.data.session.SessionCookieHolder
import com.example.musicapp.data.util.parseNeteaseCookie
import com.example.musicapp.data.util.toMd5
import com.example.musicapp.domain.model.LoginResult
import com.example.musicapp.domain.model.LoginState
import com.example.musicapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val neteaseApi: NeteaseApi,
    private val authPreferences: AuthPreferences,
    private val cookieHolder: SessionCookieHolder
) : AuthRepository {

    override fun observeLoginState(): Flow<LoginState> = authPreferences.loginStateFlow

    override suspend fun restoreSession() {
        val cookie = authPreferences.getCookie()
        cookieHolder.set(cookie)
    }

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

    override suspend fun login(phone: String, password: String): LoginResult {
        return runCatching {
            val response = neteaseApi.loginCellphoneWithPassword(
                phone = phone,
                md5Password = password.toMd5()
            )
            handleLoginResponse(response)
        }.getOrElse { throwable ->
            LoginResult(
                success = false,
                message = throwable.message ?: "登录失败，请检查网络和 API 服务"
            )
        }
    }

    override suspend fun register(
        phone: String,
        captcha: String,
        password: String,
        nickname: String
    ): LoginResult {
        return runCatching {
            val response = neteaseApi.registerCellphone(
                phone = phone,
                captcha = captcha,
                password = password,
                nickname = nickname
            )
            handleLoginResponse(response, failurePrefix = "注册")
        }.getOrElse { throwable ->
            LoginResult(
                success = false,
                message = throwable.message ?: "注册失败，请检查网络和 API 服务"
            )
        }
    }

    override suspend fun logout() {
        runCatching { neteaseApi.logout() }
        cookieHolder.set(null)
        authPreferences.clear()
    }

    private suspend fun handleLoginResponse(
        response: LoginResponse,
        failurePrefix: String = "登录"
    ): LoginResult {
        if (response.code != 200) {
            return LoginResult(
                success = false,
                message = response.message ?: "${failurePrefix}失败（code=${response.code}）"
            )
        }
        val cookie = parseNeteaseCookie(response.cookie)
        if (cookie.isBlank()) {
            return LoginResult(success = false, message = "${failurePrefix}失败：未获取到 Cookie")
        }
        val nickname = response.profile?.nickname
        val userId = response.profile?.userId
        cookieHolder.set(cookie)
        authPreferences.saveLogin(cookie, nickname, userId)
        return LoginResult(success = true, nickname = nickname)
    }
}
