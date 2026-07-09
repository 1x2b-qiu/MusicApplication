package com.example.musicapp.data.remote.interceptor

import com.example.musicapp.data.session.SessionCookieHolder
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class CookieInterceptor @Inject constructor(
    private val cookieHolder: SessionCookieHolder
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val cookie = cookieHolder.get()
        val request = if (!cookie.isNullOrBlank()) {
            chain.request().newBuilder()
                .header("Cookie", cookie)
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
