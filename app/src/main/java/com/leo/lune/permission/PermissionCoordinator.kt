package com.leo.lune.permission

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
    private var pendingResult: ((Map<String, Boolean>) -> Unit)? = null

    // 批量申请；单权限也走同一 launcher，便于后续扩展
    // 必须在 Activity 进入 STARTED 前注册（构造时完成）
    private val permissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val callback = pendingResult
            pendingResult = null
            callback?.invoke(result)
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
    // onResult：全部所需权限均已授予时为 true
    fun request(
        vararg permissions: AppPermission,
        onResult: ((granted: Boolean) -> Unit)? = null,
    ) {
        val needed = permissions
            .filter { !isGranted(it) }
            .map { it.manifest }
            .toTypedArray()
        if (needed.isEmpty()) {
            onResult?.invoke(true)
            return
        }
        pendingResult = { result ->
            val allGranted = permissions.all { isGranted(it) } ||
                needed.all { manifest -> result[manifest] == true }
            onResult?.invoke(allGranted)
        }
        permissionLauncher.launch(needed)
    }
}
