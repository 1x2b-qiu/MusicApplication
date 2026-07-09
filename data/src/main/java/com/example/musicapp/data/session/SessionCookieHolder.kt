package com.example.musicapp.data.session

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionCookieHolder @Inject constructor() {

    @Volatile
    private var cookie: String? = null

    fun get(): String? = cookie

    fun set(value: String?) {
        cookie = value
    }
}
