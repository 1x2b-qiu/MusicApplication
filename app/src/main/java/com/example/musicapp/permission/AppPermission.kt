package com.example.musicapp.permission

import android.Manifest
import android.os.Build

// 应用声明的运行时权限清单；后续新权限在此追加即可
enum class AppPermission(
    // Manifest 中的权限名
    val manifest: String,
    // 低于该 API 视为系统已授予（无需申请）
    val minSdk: Int,
    // 是否在 App 启动时主动申请
    val requestOnStartup: Boolean = false,
) {
    // 通知栏媒体控件（Android 13+）；拒绝仍可播放，仅无通知
    Notifications(
        manifest = Manifest.permission.POST_NOTIFICATIONS,
        minSdk = Build.VERSION_CODES.TIRAMISU,
        requestOnStartup = true,
    ),
}
