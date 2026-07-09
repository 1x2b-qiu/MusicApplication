package com.example.musicapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.usecase.LoginUseCase
import com.example.musicapp.domain.usecase.LoginWithCaptchaUseCase
import com.example.musicapp.domain.usecase.SendCaptchaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoginMode {
    CAPTCHA,
    PASSWORD
}

data class LoginUiState(
    val mode: LoginMode = LoginMode.CAPTCHA,
    val phone: String = "",
    val captcha: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isSendingCaptcha: Boolean = false,
    val captchaCountdown: Int = 0,
    val captchaHint: String? = null,
    val error: String? = null,
    val loginSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sendCaptchaUseCase: SendCaptchaUseCase,
    private val loginWithCaptchaUseCase: LoginWithCaptchaUseCase,
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun onModeChange(mode: LoginMode) {
        _uiState.update { it.copy(mode = mode, error = null) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone, error = null) }
    }

    fun onCaptchaChange(captcha: String) {
        _uiState.update { it.copy(captcha = captcha, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun sendCaptcha() {
        val state = _uiState.value
        if (state.isSendingCaptcha || state.captchaCountdown > 0) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSendingCaptcha = true, error = null, captchaHint = null)
            }
            val result = sendCaptchaUseCase(state.phone)
            if (result.success) {
                _uiState.update {
                    it.copy(
                        isSendingCaptcha = false,
                        captchaHint = result.message
                    )
                }
                startCaptchaCountdown()
            } else {
                _uiState.update {
                    it.copy(
                        isSendingCaptcha = false,
                        error = result.message ?: "发送验证码失败"
                    )
                }
            }
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = when (state.mode) {
                LoginMode.CAPTCHA -> loginWithCaptchaUseCase(state.phone, state.captcha)
                LoginMode.PASSWORD -> loginUseCase(state.phone, state.password)
            }
            if (result.success) {
                _uiState.update {
                    it.copy(isLoading = false, loginSuccess = true)
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.message ?: "登录失败"
                    )
                }
            }
        }
    }

    fun consumeLoginSuccess() {
        _uiState.update { it.copy(loginSuccess = false) }
    }

    private fun startCaptchaCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (seconds in 60 downTo 1) {
                _uiState.update { it.copy(captchaCountdown = seconds) }
                delay(1_000)
            }
            _uiState.update { it.copy(captchaCountdown = 0) }
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }
}
