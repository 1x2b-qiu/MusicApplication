package com.example.musicapp.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.usecase.ObserveLoginStateUseCase
import com.example.musicapp.domain.usecase.RestoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Splash 页 UI 状态
data class SplashUiState(
    // 是否仍在检查本地会话
    val isChecking: Boolean = true,
    // 本地是否存在有效登录会话
    val isLoggedIn: Boolean = false
)

// Splash 页 ViewModel：恢复 Cookie 并判断应进入首页还是登录页
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val observeLoginStateUseCase: ObserveLoginStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            restoreSessionUseCase()
            val isLoggedIn = observeLoginStateUseCase().first().isLoggedIn
            _uiState.update {
                it.copy(isChecking = false, isLoggedIn = isLoggedIn)
            }
        }
    }
}
