package com.leo.lune.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leo.lune.domain.repository.AuthRepository
import com.leo.lune.navigation.MusicRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// 进 NavHost 前：恢复 Cookie，并决定 startDestination（Library / Login）
@HiltViewModel
class SessionBootstrapViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // null = 仍在恢复会话；非 null 后才创建 NavHost
    private val _startRoute = MutableStateFlow<MusicRoute?>(null)
    val startRoute: StateFlow<MusicRoute?> = _startRoute.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.restoreSession()
            val isLoggedIn = authRepository.observeLoginState().first().isLoggedIn
            _startRoute.value = if (isLoggedIn) MusicRoute.Library else MusicRoute.Login
        }
    }
}
