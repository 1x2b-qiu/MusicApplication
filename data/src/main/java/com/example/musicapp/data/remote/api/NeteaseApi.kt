package com.example.musicapp.data.remote.api

import com.example.musicapp.data.remote.response.CaptchaSentResponse
import com.example.musicapp.data.remote.response.LoginResponse
import com.example.musicapp.data.remote.response.LoginStatusResponse
import com.example.musicapp.data.remote.response.SearchResponse
import com.example.musicapp.data.remote.response.SongUrlResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface NeteaseApi {

    @GET("search")
    suspend fun search(
        @Query("keywords") keywords: String,
        @Query("limit") limit: Int = 20
    ): SearchResponse

    @GET("song/url")
    suspend fun getSongUrl(
        @Query("id") songId: Long
    ): SongUrlResponse

    @GET("captcha/sent")
    suspend fun sendCaptcha(
        @Query("phone") phone: String,
        @Query("ctcode") countryCode: String = "86"
    ): CaptchaSentResponse

    @FormUrlEncoded
    @POST("login/cellphone")
    suspend fun loginCellphoneWithPassword(
        @Field("phone") phone: String,
        @Field("md5_password") md5Password: String,
        @Field("countrycode") countryCode: String = "86"
    ): LoginResponse

    @FormUrlEncoded
    @POST("login/cellphone")
    suspend fun loginCellphoneWithCaptcha(
        @Field("phone") phone: String,
        @Field("captcha") captcha: String,
        @Field("countrycode") countryCode: String = "86"
    ): LoginResponse

    @FormUrlEncoded
    @POST("register/cellphone")
    suspend fun registerCellphone(
        @Field("phone") phone: String,
        @Field("captcha") captcha: String,
        @Field("password") password: String,
        @Field("nickname") nickname: String,
        @Field("countrycode") countryCode: String = "86"
    ): LoginResponse

    @GET("login/status")
    suspend fun loginStatus(): LoginStatusResponse

    @GET("logout")
    suspend fun logout(): LoginResponse
}
