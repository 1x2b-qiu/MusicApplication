package com.leo.lune.ui.component.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// 全局确认弹窗请求（回调随请求携带，由宿主触发）
data class ConfirmDialogRequest(
    val title: String,
    val message: String? = null,
    val confirmLabel: String = "确认",
    val cancelLabel: String = "取消",
    val danger: Boolean = false,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit = {}
)

// 全局确认弹窗控制器：供各页发起请求，由导航根宿主展示
@Singleton
class ConfirmDialogController @Inject constructor() {
    private val _request = MutableStateFlow<ConfirmDialogRequest?>(null)
    val request: StateFlow<ConfirmDialogRequest?> = _request.asStateFlow()

    fun show(request: ConfirmDialogRequest) {
        _request.value = request
    }

    fun dismiss() {
        _request.value = null
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ConfirmDialogEntryPoint {
    fun confirmDialogController(): ConfirmDialogController
}

@Composable
fun rememberConfirmDialogController(): ConfirmDialogController {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) {
        EntryPointAccessors.fromApplication(
            appContext,
            ConfirmDialogEntryPoint::class.java
        ).confirmDialogController()
    }
}
