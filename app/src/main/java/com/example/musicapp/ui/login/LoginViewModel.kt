package com.example.musicapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.usecase.auth.LoginWithCaptchaUseCase
import com.example.musicapp.domain.usecase.auth.SendCaptchaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 登录页 UI 状态
data class LoginUiState(
    // 手机号（仅数字，最多 11 位）
    val phone: String = "",
    // 短信验证码（仅数字，最多 4 位）
    val captcha: String = "",
    // 是否正在提交登录请求
    val isLoading: Boolean = false,
    // 是否正在发送验证码
    val isSendingCaptcha: Boolean = false,
    // 验证码按钮倒计时秒数；大于 0 时不可重复发送
    val captchaCountdown: Int = 0,
    // 验证码发送成功后的提示文案
    val captchaHint: String? = null,
    // 表单校验或接口错误信息
    val error: String? = null,
    // 登录是否成功；LoginScreen 消费后会重置为 false
    val loginSuccess: Boolean = false
)

// 登录页 ViewModel
// 负责验证码发送、验证码登录，以及 60 秒倒计时
@HiltViewModel
class LoginViewModel @Inject constructor(
    // 向手机号发送登录验证码
    private val sendCaptchaUseCase: SendCaptchaUseCase,
    // 使用手机号 + 验证码完成登录
    private val loginWithCaptchaUseCase: LoginWithCaptchaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    // 对外只读，LoginScreen 通过 collect 订阅
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // 验证码倒计时任务；页面销毁或重新发送时会取消
    private var countdownJob: Job? = null

    fun onPhoneChange(phone: String) {
        _uiState.update {
            it.copy(phone = phone.filter(Char::isDigit).take(11), error = null)
        }
    }

    fun onCaptchaChange(captcha: String) {
        _uiState.update {
            it.copy(captcha = captcha.filter(Char::isDigit).take(4), error = null)
        }
    }

    // 发送短信验证码
    // 发送中或倒计时未结束时直接返回，避免重复请求
    fun sendCaptcha() {
        val state = _uiState.value
        if (state.isSendingCaptcha || state.captchaCountdown > 0) return

        if (state.phone.length < 11) {
            _uiState.update { it.copy(error = "请输入 11 位手机号码") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSendingCaptcha = true, error = null, captchaHint = null)
            }
            val result = sendCaptchaUseCase(state.phone)
            if (result.success) {
                _uiState.update {
                    it.copy(
                        isSendingCaptcha = false,
                        captchaHint = result.message ?: "验证码已发送，请注意查收"
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

    // 提交验证码登录
    fun login() {
        val state = _uiState.value
        if (state.isLoading) return

        if (state.phone.isBlank() || state.captcha.isBlank()) {
            _uiState.update { it.copy(error = "请填写手机号和验证码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = loginWithCaptchaUseCase(state.phone, state.captcha)
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
