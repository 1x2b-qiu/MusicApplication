package com.example.musicapp.data.remote.response

// 发送验证码接口响应
data class CaptchaSentResponse(
    // 业务状态码，200 表示成功
    val code: Int,
    // 是否发送成功
    val data: Boolean?,
    val message: String?
)
