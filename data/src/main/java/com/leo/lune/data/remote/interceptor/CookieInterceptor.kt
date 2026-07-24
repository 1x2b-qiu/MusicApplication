package com.leo.lune.data.remote.interceptor

import com.leo.lune.data.session.SessionCookieHolder
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

// OkHttp 请求拦截器：自动附加登录 Cookie 到请求头
class CookieInterceptor @Inject constructor(
    private val cookieHolder: SessionCookieHolder
) : Interceptor {

    // 先等待 Cookie 恢复完成（进程启动即恢复），若内存中有 Cookie 则附加到请求头后放行
    override fun intercept(chain: Interceptor.Chain): Response {
        // 等待恢复完成；超时则放弃等待，避免请求永久挂起
        // 拦截器运行在 OkHttp 后台线程，此处阻塞安全
        runBlocking {
            withTimeoutOrNull(AWAIT_READY_TIMEOUT_MS) {
                cookieHolder.awaitReady()
            }
        }
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

    private companion object {
        // 等待 Cookie 恢复的最长时间
        const val AWAIT_READY_TIMEOUT_MS = 3_000L
    }
}
