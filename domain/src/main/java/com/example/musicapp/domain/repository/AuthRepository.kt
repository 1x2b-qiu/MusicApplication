package com.example.musicapp.domain.repository

import com.example.musicapp.domain.model.LoginResult
import com.example.musicapp.domain.model.LoginState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeLoginState(): Flow<LoginState>
    suspend fun restoreSession()
    suspend fun sendCaptcha(phone: String): LoginResult
    suspend fun loginWithCaptcha(phone: String, captcha: String): LoginResult
    suspend fun login(phone: String, password: String): LoginResult
    suspend fun logout()
}
