package com.leo.lune.data.remote.response

// 推荐新音乐（猜你喜欢）接口响应
data class PersonalizedNewsongResponse(
    // 业务状态码，200 表示成功
    val code: Int,
    // 推荐条目列表
    val result: List<PersonalizedNewsongItem>?
)

// 推荐新音乐条目：真实歌曲嵌套在 song 字段
data class PersonalizedNewsongItem(
    val id: Long? = null,
    val song: SongDto? = null
)
