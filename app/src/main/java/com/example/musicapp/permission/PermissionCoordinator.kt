package com.example.musicapp.permission

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

// 权限协调器：挂在 ComponentActivity 上注册 launcher，统一检查与申请
// MainActivity 只负责 requestStartup；其他页面可再调 request / isGranted
class PermissionCoordinator(
    private val activity: ComponentActivity,
) {
    // 批量申请；单权限也走同一 launcher，便于后续扩展
    // 必须在 Activity 进入 STARTED 前注册（构造时完成）
    private val permissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // 各权限拒绝策略不同，具体业务自行兜底（如通知拒绝仍可播）
        }

    // 当前系统是否已授予（低于 minSdk 视为已授予）
    fun isGranted(permission: AppPermission): Boolean {
        if (Build.VERSION.SDK_INT < permission.minSdk) return true
        return ContextCompat.checkSelfPermission(
            activity,
            permission.manifest
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 启动时申请所有标记了 requestOnStartup 且尚未授予的权限
    fun requestStartup() {
        request(*AppPermission.entries.filter { it.requestOnStartup }.toTypedArray())
    }

    // 按需申请一个或多个权限（已授予的会跳过）
    fun request(vararg permissions: AppPermission) {
        val needed = permissions
            .filter { !isGranted(it) }
            .map { it.manifest }
            .toTypedArray()
        if (needed.isEmpty()) return
        permissionLauncher.launch(needed)
    }
}
