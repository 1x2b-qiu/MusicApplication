package com.leo.lune.data.session

import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject
import javax.inject.Singleton

// 会话 Cookie 内存持有者
// 登录成功后写入，OkHttp 拦截器读取并附加到请求头
@Singleton
class SessionCookieHolder @Inject constructor() {

    @Volatile
    private var cookie: String? = null

    // 就绪信号：Cookie 恢复流程结束（无论是否已登录）后释放
    // 保证网络请求不会在 Cookie 恢复完成前「裸奔」
    private val ready = CompletableDeferred<Unit>()

    // 获取当前会话 Cookie
    fun get(): String? = cookie

    // 更新或清除会话 Cookie，并标记恢复完成（complete 幂等，多次调用安全）
    fun set(value: String?) {
        cookie = value
        ready.complete(Unit)
    }

    // 挂起直到 Cookie 恢复完成（set 被调用）
    suspend fun awaitReady() {
        ready.await()
    }
}
