package com.leo.lune

import android.app.Application
import com.leo.lune.domain.usecase.auth.RestoreSessionUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class MusicApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 进程启动即恢复会话 Cookie，使其与 Activity 生命周期解耦
        // 无论进程为 Activity / Service / 广播拉起，都先于业务组件执行
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SessionRestoreEntryPoint::class.java
        )
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            entryPoint.restoreSessionUseCase()
        }
    }
}

// 供 Application 在 onCreate 中访问 Hilt 单例的入口
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SessionRestoreEntryPoint {
    fun restoreSessionUseCase(): RestoreSessionUseCase
}
