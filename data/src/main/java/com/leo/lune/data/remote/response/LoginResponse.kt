package com.leo.lune.data.remote.response

// 登录接口响应
data class LoginResponse(
    // 业务状态码，200 表示成功
    val code: Int,
    // Cookie 可能是字符串或字符串列表
    val cookie: Any?,
    val profile: LoginProfileDto?,
    val message: String?
)

// 登录成功后返回的用户资料
data class LoginProfileDto(
    val userId: Long?,
    val nickname: String?,
    val avatarUrl: String?
)

// 登录状态查询响应
data class LoginStatusResponse(
    // 登录状态详情，未登录时为 null
    val data: LoginStatusData?
)

// 登录状态详情
data class LoginStatusData(
    // 业务状态码，200 表示已登录
    val code: Int,
    val account: LoginAccountDto?,
    val profile: LoginProfileDto?
)

// 账号信息
data class LoginAccountDto(
    // 是否为匿名用户
    val anonimousUser: Boolean?
)
