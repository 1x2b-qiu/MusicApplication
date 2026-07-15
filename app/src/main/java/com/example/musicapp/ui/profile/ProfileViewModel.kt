package com.example.musicapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.model.LoginState
import com.example.musicapp.domain.usecase.LogoutUseCase
import com.example.musicapp.domain.usecase.ObserveLoginStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 我的页 UI 状态
data class ProfileUiState(
    // 登录状态，用于展示头像区文案和登录 / 退出按钮
    val loginState: LoginState = LoginState()
)

// 我的页 ViewModel
// 进页时读取一次登录状态，并处理退出登录
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val observeLoginStateUseCase: ObserveLoginStateUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    // 对外只读，ProfileScreen 通过 collect 订阅
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // 进页时只取一次登录态，同步到 UI
        viewModelScope.launch {
            val loginState = observeLoginStateUseCase().first()
            _uiState.update { it.copy(loginState = loginState) }
        }
    }

    // 退出登录，完成后回调导航层回到登录页
    fun logout(onLoggedOut: () -> Unit = {}) {
        viewModelScope.launch {
            logoutUseCase()
            onLoggedOut()
        }
    }
}
