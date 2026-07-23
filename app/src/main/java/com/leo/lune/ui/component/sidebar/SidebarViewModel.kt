package com.leo.lune.ui.component.sidebar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.domain.usecase.auth.ObserveLoginStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 侧边栏资料区展示状态
data class SidebarUiState(
    // 用户昵称；空则 UI 显示「未登录」
    val nickname: String? = null,
    // 头像 URL
    val avatarUrl: String? = null
)

// 侧边栏 ViewModel：自行订阅登录态，不依赖导航回传
@HiltViewModel
class SidebarViewModel @Inject constructor(
    private val observeLoginStateUseCase: ObserveLoginStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SidebarUiState())
    val uiState: StateFlow<SidebarUiState> = _uiState.asStateFlow()

    init {
        // 打开侧栏前取一次登录态即可，不做持续订阅
        viewModelScope.launch {
            val login = observeLoginStateUseCase().first()
            _uiState.update {
                it.copy(
                    nickname = login.nickname,
                    avatarUrl = login.avatarUrl
                )
            }
        }
    }
}
