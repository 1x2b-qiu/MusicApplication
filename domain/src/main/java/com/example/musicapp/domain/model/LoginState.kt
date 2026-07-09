package com.example.musicapp.domain.model

data class LoginState(
    val isLoggedIn: Boolean = false,
    val nickname: String? = null,
    val userId: Long? = null
)

data class LoginResult(
    val success: Boolean,
    val message: String? = null,
    val nickname: String? = null
)
