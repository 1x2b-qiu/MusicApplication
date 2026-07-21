package com.example.musicapp.domain.model

import androidx.compose.runtime.Immutable

// 当前登录用户的持久化状态快照
@Immutable
data class LoginState(
    // 是否已登录
    val isLoggedIn: Boolean = false,
    // 用户昵称
    val nickname: String? = null,
    // 用户 ID
    val userId: Long? = null,
    // 头像 URL
    val avatarUrl: String? = null
)

// 登录或验证码请求的操作结果
@Immutable
data class LoginResult(
    // 操作是否成功
    val success: Boolean,
    // 失败时的提示信息
    val message: String? = null,
    // 登录成功后的用户昵称
        val nickname: String? = null
)
