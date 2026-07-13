package com.example.musicapp.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicapp.R
import com.example.musicapp.ui.component.loading.Loading
import kotlinx.coroutines.delay

// 登录页
// 验证码登录，颜色跟随 App 深浅色主题（MaterialTheme.colorScheme）
// onBack：返回上一页（预留）；onLoginSuccess：登录成功后回调
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 网易云第三方登录占位提示
    var showNeteaseMessage by remember { mutableStateOf(false) }

    // 登录成功后通知导航层返回，并消费一次性事件
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            viewModel.consumeLoginSuccess()
            onLoginSuccess()
        }
    }

    // Toast 自动消失
    LaunchedEffect(showNeteaseMessage) {
        if (showNeteaseMessage) {
            delay(2_500)
            showNeteaseMessage = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // 品牌标语区
            LoginBrandHeader()

            // 登录表单卡片
            LoginForm(
                uiState = uiState,
                onPhoneChange = viewModel::onPhoneChange,
                onCaptchaChange = viewModel::onCaptchaChange,
                onSendCaptcha = viewModel::sendCaptcha,
                onLogin = viewModel::login,
                modifier = Modifier.padding(top = 32.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // 第三方登录入口（当前为占位，未接入 OAuth）
            LoginThirdPartySection(
                onNeteaseClick = { showNeteaseMessage = true }
            )
        }

        // 底部 Toast
        LoginNeteaseToast(
            visible = showNeteaseMessage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp)
        )

        Loading(isVisible = uiState.isLoading)
    }
}

// 登录页品牌标语区
@Composable
private fun LoginBrandHeader(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = modifier) {
        Text(
            text = "MUSIC, FOR YOU",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Center,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(52.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(
                modifier = Modifier.width(32.dp),
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                thickness = 1.dp
            )
            Text(
                text = "PRIVATE FREQUENCY",
                modifier = Modifier.padding(horizontal = 12.dp),
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
        }

        Text(
            text = "听见你，\n未曾说出的部分。",
            modifier = Modifier.padding(top = 20.dp),
            color = colorScheme.onBackground,
            fontSize = 35.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 43.sp,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "一段只属于你的声音旅程。",
            modifier = Modifier.padding(top = 16.dp),
            color = colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            letterSpacing = 0.3.sp
        )
    }
}

// 登录表单卡片：手机号、验证码、提交按钮
@Composable
private fun LoginForm(
    uiState: LoginUiState,
    onPhoneChange: (String) -> Unit,
    onCaptchaChange: (String) -> Unit,
    onSendCaptcha: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isButtonEnabled = uiState.phone.isNotBlank() && uiState.captcha.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colorScheme.surfaceContainerHigh)
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        // 手机号输入
        LoginPhoneField(
            phone = uiState.phone,
            onPhoneChange = onPhoneChange
        )

        // 验证码输入 + 获取验证码
        LoginCaptchaField(
            captcha = uiState.captcha,
            captchaCountdown = uiState.captchaCountdown,
            isSendingCaptcha = uiState.isSendingCaptcha,
            onCaptchaChange = onCaptchaChange,
            onSendCaptcha = onSendCaptcha,
            modifier = Modifier.padding(top = 16.dp)
        )

        // 提交登录
        LoginSubmitButton(
            enabled = isButtonEnabled && !uiState.isLoading,
            onClick = onLogin,
            modifier = Modifier.padding(top = 20.dp)
        )

        // 错误信息优先于验证码发送提示
        val notice = uiState.error ?: uiState.captchaHint.orEmpty()
        Text(
            text = notice,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Center,
            color = if (uiState.error != null) {
                colorScheme.error
            } else {
                colorScheme.onSurfaceVariant
            },
            fontSize = 12.sp,
            minLines = 1
        )
    }
}

// 手机号输入框
@Composable
private fun LoginPhoneField(
    phone: String,
    onPhoneChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = modifier) {
        Text(
            text = "手机号",
            color = colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        LoginInputRow {
            Text(
                text = "+86",
                color = colorScheme.onSurface,
                fontSize = 15.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(18.dp)
                    .background(colorScheme.outlineVariant)
            )
            BasicTextField(
                value = phone,
                onValueChange = onPhoneChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                textStyle = TextStyle(
                    color = colorScheme.onSurface,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (phone.isEmpty()) {
                            Text(
                                text = "请输入手机号码",
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

// 验证码输入框 + 获取验证码按钮
@Composable
private fun LoginCaptchaField(
    captcha: String,
    captchaCountdown: Int,
    isSendingCaptcha: Boolean,
    onCaptchaChange: (String) -> Unit,
    onSendCaptcha: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val canSendCaptcha = captchaCountdown == 0 && !isSendingCaptcha
    val actionColor = colorScheme.onSurface.copy(alpha = if (canSendCaptcha) 0.85f else 0.45f)

    Column(modifier = modifier) {
        Text(
            text = "验证码",
            color = colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        LoginInputRow {
            BasicTextField(
                value = captcha,
                onValueChange = onCaptchaChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = colorScheme.onSurface,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (captcha.isEmpty()) {
                            Text(
                                text = "输入验证码",
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clickable(
                        enabled = canSendCaptcha,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSendCaptcha() },
                contentAlignment = Alignment.Center
            ) {
                when {
                    isSendingCaptcha -> {
                    }
                    captchaCountdown > 0 -> {
                        Text(
                            text = "${captchaCountdown}s 后重试",
                            color = actionColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    else -> {
                        Text(
                            text = "获取验证码",
                            color = actionColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// 登录提交按钮
@Composable
private fun LoginSubmitButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) colorScheme.primary else colorScheme.primary.copy(alpha = 0.5f)
            )
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "登录并继续",
            color = colorScheme.onPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
    }
}

// 登录表单统一输入行样式
@Composable
private fun LoginInputRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceVariant)
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

// 第三方登录入口（当前为占位，未接入 OAuth）
@Composable
private fun LoginThirdPartySection(
    onNeteaseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = colorScheme.outlineVariant
            )
            Text(
                text = "其他方式",
                modifier = Modifier.padding(horizontal = 12.dp),
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = colorScheme.outlineVariant
            )
        }

        Box(
            modifier = Modifier
                .padding(bottom = 26.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .background(colorScheme.surfaceVariant)
                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(50))
                .clickable(onClick = onNeteaseClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_netease),
                contentDescription = "使用网易云验证登录",
                modifier = Modifier.size(23.dp),
                tint = Color.Unspecified
            )
        }
    }
}

// 网易云第三方登录占位 Toast
@Composable
private fun LoginNeteaseToast(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Text(
            text = "正在跳转网易云验证…",
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(colorScheme.surfaceContainerHighest)
                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            color = colorScheme.onSurface,
            fontSize = 14.sp
        )
    }
}
