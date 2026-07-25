package com.leo.lune.di

import javax.inject.Qualifier

// 标识 AudD 专用 OkHttp / Retrofit（无网易云 Cookie）
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Audd
