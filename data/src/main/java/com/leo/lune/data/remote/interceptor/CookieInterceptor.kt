package com.leo.lune.data.remote.interceptor

import com.leo.lune.data.session.SessionCookieHolder
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

// OkHttp 请求拦截器：自动附加登录 Cookie 到请求头
class CookieInterceptor @Inject constructor(
    private val cookieHolder: SessionCookieHolder
) : Interceptor {

    // 若内存中有 Cookie，则附加到请求头后放行
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
