package com.leo.lune.data.remote.response

// 每日推荐歌曲接口响应（需登录）
data class RecommendSongsResponse(
    // 业务状态码，200 表示成功
    val code: Int,
    // 推荐数据体
    val data: RecommendSongsData?
)

// 每日推荐数据：dailySongs 为当日推荐曲目列表
data class RecommendSongsData(
    val dailySongs: List<SongDto>?
)
