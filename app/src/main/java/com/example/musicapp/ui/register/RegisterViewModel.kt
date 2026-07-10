package com.example.musicapp.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.usecase.RegisterUseCase
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

data class RegisterUiState(
    val phone: String = "",
    val captcha: String = "",
    val nickname: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isSendingCaptcha: Boolean = false,
    val captchaCountdown: Int = 0,
    val captchaHint: String? = null,
    val error: String? = null,
    val registerSuccess: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val sendCaptchaUseCase: SendCaptchaUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone, error = null) }
    }

    fun onCaptchaChange(captcha: String) {
        _uiState.update { it.copy(captcha = captcha, error = null) }
    }

    fun onNicknameChange(nickname: String) {
        _uiState.update { it.copy(nickname = nickname, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, error = null) }
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
                    it.copy(isSendingCaptcha = false, captchaHint = result.message)
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

    fun register() {
        val state = _uiState.value
        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = registerUseCase(
                phone = state.phone,
                captcha = state.captcha,
                password = state.password,
                confirmPassword = state.confirmPassword,
                nickname = state.nickname
            )
            if (result.success) {
                _uiState.update {
                    it.copy(isLoading = false, registerSuccess = true)
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.message ?: "注册失败"
                    )
                }
            }
        }
    }

    fun consumeRegisterSuccess() {
        _uiState.update { it.copy(registerSuccess = false) }
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
