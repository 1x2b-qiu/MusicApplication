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

// 登录方式
enum class LoginMode {
    // 验证码登录
    CAPTCHA,
    // 密码登录
    PASSWORD
}

// 登录页 UI 状态
data class LoginUiState(
    // 当前登录方式
    val mode: LoginMode = LoginMode.CAPTCHA,
    // 手机号
    val phone: String = "",
    // 短信验证码
    val captcha: String = "",
    // 密码
    val password: String = "",
    // 是否正在提交登录请求
    val isLoading: Boolean = false,
    // 是否正在发送验证码
    val isSendingCaptcha: Boolean = false,
    // 验证码按钮倒计时秒数；大于 0 时不可重复发送
    val captchaCountdown: Int = 0,
    // 验证码发送成功后的提示文案
    val captchaHint: String? = null,
    // 表单或接口错误信息
    val error: String? = null,
    // 登录是否成功；LoginScreen 消费后会重置为 false
    val loginSuccess: Boolean = false
)

// 登录页 ViewModel
// 负责切换登录方式、发送验证码、提交登录，以及验证码倒计时
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sendCaptchaUseCase: SendCaptchaUseCase,
    private val loginWithCaptchaUseCase: LoginWithCaptchaUseCase,
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    // 对外只读，LoginScreen 通过 collect 订阅
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // 验证码倒计时任务；页面销毁或重新发送时会取消
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

    // 发送短信验证码
    // 发送中或倒计时未结束时直接返回，避免重复请求
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

    // 提交登录
    // 根据当前登录方式调用对应 UseCase
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

    // 登录成功事件已被 UI 处理后调用，防止重复触发导航
    fun consumeLoginSuccess() {
        _uiState.update { it.copy(loginSuccess = false) }
    }

    // 启动 60 秒验证码倒计时
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
