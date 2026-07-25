package com.leo.lune.data.remote.api

import com.leo.lune.data.remote.response.AuddRecognizeResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// AudD 听歌识曲 API（上传本地录音片段）
interface AuddApi {

    // 上传音频文件识别曲目；api_token 与 file 均为 form-data
    @Multipart
    @POST(".")
    suspend fun recognize(
        @Part("api_token") apiToken: RequestBody,
        @Part file: MultipartBody.Part,
    ): AuddRecognizeResponse
}
