package com.example.musicapp.data.remote.response

data class LoginResponse(
    val code: Int,
    val cookie: Any?,
    val profile: LoginProfileDto?,
    val message: String?
)

data class LoginProfileDto(
    val userId: Long?,
    val nickname: String?
)

data class LoginStatusResponse(
    val data: LoginStatusData?
)

data class LoginStatusData(
    val code: Int,
    val account: LoginAccountDto?,
    val profile: LoginProfileDto?
)

data class LoginAccountDto(
    val anonimousUser: Boolean?
)
