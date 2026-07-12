package com.example.musicapp.data.remote.api

import com.example.musicapp.data.remote.response.CaptchaSentResponse
import com.example.musicapp.data.remote.response.LikeResponse
import com.example.musicapp.data.remote.response.LikelistResponse
import com.example.musicapp.data.remote.response.LoginResponse
import com.example.musicapp.data.remote.response.LoginStatusResponse
import com.example.musicapp.data.remote.response.LyricResponse
import com.example.musicapp.data.remote.response.PlaylistTrackAllResponse
import com.example.musicapp.data.remote.response.SearchResponse
import com.example.musicapp.data.remote.response.SongDetailResponse
import com.example.musicapp.data.remote.response.SongUrlResponse
import com.example.musicapp.data.remote.response.UserPlaylistResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// 网易云音乐 OpenAPI 接口定义
interface NeteaseApi {

    // 搜索歌曲（cloudsearch 比 search 返回更完整的 al.picUrl 封面）
    @GET("cloudsearch")
    suspend fun search(
        @Query("keywords") keywords: String,
        @Query("limit") limit: Int = 20,
        @Query("type") type: Int = 1
    ): SearchResponse

    // 获取歌曲播放地址
    @GET("song/url")
    suspend fun getSongUrl(
        @Query("id") songId: Long
    ): SongUrlResponse

    // 获取歌曲歌词
    @GET("lyric")
    suspend fun getLyric(
        @Query("id") songId: Long
    ): LyricResponse

    // 收藏或取消收藏歌曲
    @GET("like")
    suspend fun likeSong(
        @Query("id") songId: Long,
        @Query("like") like: Boolean = true
    ): LikeResponse

    // 获取用户红心歌单中的歌曲 ID 列表
    @GET("likelist")
    suspend fun getLikelist(
        @Query("uid") userId: Long
    ): LikelistResponse

    // 批量获取歌曲详情（ids 为逗号分隔的 ID 字符串）
    @GET("song/detail")
    suspend fun getSongDetail(
        @Query("ids") songIds: String
    ): SongDetailResponse

    // 获取用户创建的歌单列表
    @GET("user/playlist")
    suspend fun getUserPlaylists(
        @Query("uid") userId: Long,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0
    ): UserPlaylistResponse

    // 获取歌单内全部歌曲
    @GET("playlist/track/all")
    suspend fun getPlaylistTrackAll(
        @Query("id") playlistId: Long,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int = 0
    ): PlaylistTrackAllResponse

    // 向手机号发送登录验证码
    @GET("captcha/sent")
    suspend fun sendCaptcha(
        @Query("phone") phone: String,
        @Query("ctcode") countryCode: String = "86"
    ): CaptchaSentResponse

    // 手机号 + 验证码登录
    @FormUrlEncoded
    @POST("login/cellphone")
    suspend fun loginCellphoneWithCaptcha(
        @Field("phone") phone: String,
        @Field("captcha") captcha: String,
        @Field("countrycode") countryCode: String = "86"
    ): LoginResponse

    // 查询当前登录状态
    @GET("login/status")
    suspend fun loginStatus(): LoginStatusResponse

    // 登出当前会话
    @GET("logout")
    suspend fun logout(): LoginResponse
}
