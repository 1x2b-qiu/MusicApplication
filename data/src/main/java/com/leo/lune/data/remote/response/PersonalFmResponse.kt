package com.leo.lune.data.remote.response

// 私人 FM 接口响应（需登录）
data class PersonalFmResponse(
    // 业务状态码，200 表示成功
    val code: Int,
    // 本批推荐歌曲；再次请求可拿到下一批
    val data: List<SongDto>?
)
