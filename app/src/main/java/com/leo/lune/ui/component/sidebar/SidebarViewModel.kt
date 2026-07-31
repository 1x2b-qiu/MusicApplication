package com.leo.lune.ui.component.sidebar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.controller.MusicPlayerController
import com.leo.lune.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// 侧边栏资料区展示状态
data class SidebarUiState(
    // 用户昵称；空则 UI 显示「未登录」
    val nickname: String? = null,
    // 头像 URL
    val avatarUrl: String? = null
)

// 侧边栏 ViewModel：持续订阅登录态（根布局早于登录页创建，一次性读取会拿到空资料）
@HiltViewModel
class SidebarViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val playerController: MusicPlayerController
) : ViewModel() {

    val uiState: StateFlow<SidebarUiState> = authRepository.observeLoginState()
        .map { login ->
            SidebarUiState(
                nickname = login.nickname,
                avatarUrl = login.avatarUrl
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SidebarUiState()
        )

    // 退出登录：清服务端会话 / 本地 Cookie，并停止播放；完成后回调（如跳转登录页）
    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                authRepository.logout()
                playerController.clearQueue()
            }
            onComplete()
        }
    }
}
