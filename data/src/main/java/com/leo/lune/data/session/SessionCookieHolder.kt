package com.leo.lune.data.session

import javax.inject.Inject
import javax.inject.Singleton

// 会话 Cookie 内存持有者
// 登录成功后写入，OkHttp 拦截器读取并附加到请求头
@Singleton
class SessionCookieHolder @Inject constructor() {

    @Volatile
    private var cookie: String? = null

    // 获取当前会话 Cookie
    fun get(): String? = cookie

    // 更新或清除会话 Cookie
    fun set(value: String?) {
        cookie = value
    }
}
