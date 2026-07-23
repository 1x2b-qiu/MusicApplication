package com.leo.lune.ui.component.sidebar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.domain.usecase.auth.ObserveLoginStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    observeLoginStateUseCase: ObserveLoginStateUseCase
) : ViewModel() {

    val uiState: StateFlow<SidebarUiState> = observeLoginStateUseCase()
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
}
