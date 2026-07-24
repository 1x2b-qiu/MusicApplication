package com.leo.lune.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leo.lune.R
import com.leo.lune.ui.component.loading.Loading
import com.leo.lune.util.ClearFocusOnImeHidden
import com.leo.lune.util.consumePointersUnlessResumed
import com.leo.lune.util.dismissKeyboardOnTap
import com.leo.lune.util.rememberDismissKeyboard

// 登录页
// UI 按设计稿重制：启动 logo + 验证码 / 密码登录
// onBack：返回上一页（预留）；onLoginSuccess：登录成功后回调
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dismissKeyboard = rememberDismissKeyboard()
    ClearFocusOnImeHidden()

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            viewModel.consumeLoginSuccess()
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .consumePointersUnlessResumed()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 240.dp)
                .dismissKeyboardOnTap()
        ) {
            // 中间启动 logo（无边框）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Lune",
                    modifier = Modifier.size(240.dp),
                    tint = colorScheme.onBackground
                )
            }

            LoginForm(
                uiState = uiState,
                onPhoneChange = viewModel::onPhoneChange,
                onCaptchaChange = viewModel::onCaptchaChange,
                onPasswordChange = viewModel::onPasswordChange,
                onToggleShowPassword = viewModel::toggleShowPassword,
                onSwitchMode = viewModel::switchCredentialMode,
                onSendCaptcha = viewModel::sendCaptcha,
                onLogin = {
                    dismissKeyboard()
                    viewModel.login()
                },
                onDismissKeyboard = dismissKeyboard,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .align(Alignment.CenterHorizontally)
            )
        }

        LoginAgreement(
            agreed = uiState.agreed,
            onAgreedChange = viewModel::onAgreedChange,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        )
        LoginThirdPartySection(
            onNeteaseClick = { },
            onQqMusicClick = { },
            onSpotifyClick = { },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
        )
    }
}

// 登录表单：手机号 + 验证码/密码 + 提交
@Composable
private fun LoginForm(
    uiState: LoginUiState,
    onPhoneChange: (String) -> Unit,
    onCaptchaChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleShowPassword: () -> Unit,
    onSwitchMode: () -> Unit,
    onSendCaptcha: () -> Unit,
    onLogin: () -> Unit,
    onDismissKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val credentialFilled = when (uiState.credentialMode) {
        LoginCredentialMode.Captcha -> uiState.captcha.isNotBlank()
        LoginCredentialMode.Password -> uiState.password.isNotBlank()
    }
    val isButtonEnabled = uiState.agreed &&
        uiState.phone.isNotBlank() &&
        credentialFilled &&
        !uiState.isLoading

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LoginPhoneField(
            phone = uiState.phone,
            onPhoneChange = onPhoneChange
        )

        LoginCredentialField(
            mode = uiState.credentialMode,
            captcha = uiState.captcha,
            captchaCountdown = uiState.captchaCountdown,
            isSendingCaptcha = uiState.isSendingCaptcha,
            password = uiState.password,
            showPassword = uiState.showPassword,
            onCaptchaChange = onCaptchaChange,
            onPasswordChange = onPasswordChange,
            onToggleShowPassword = onToggleShowPassword,
            onSwitchMode = onSwitchMode,
            onSendCaptcha = onSendCaptcha,
            onDismissKeyboard = onDismissKeyboard
        )
        val notice = uiState.error ?: uiState.captchaHint.orEmpty()
        if (notice.isNotEmpty()) {
            Text(
                text = notice,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = if (uiState.error != null) {
                    colorScheme.error
                } else {
                    colorScheme.onSurfaceVariant
                },
                fontSize = 12.sp
            )
        }
        LoginSubmitButton(
            enabled = isButtonEnabled,
            onClick = onLogin,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun LoginPhoneField(
    phone: String,
    onPhoneChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val textStyle = LoginFieldTextStyle(color = colorScheme.onSurface)
    val placeholderStyle = LoginFieldTextStyle(color = colorScheme.onSurface.copy(alpha = 0.35f))

    LoginInputRow(modifier = modifier) {
        LoginFieldLeadingIcon(icon = Icons.Outlined.PhoneIphone)
        BasicTextField(
            value = phone,
            onValueChange = onPhoneChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            textStyle = textStyle,
            cursorBrush = SolidColor(colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            decorationBox = { innerTextField ->
                LoginFieldDecorationBox(
                    showPlaceholder = phone.isEmpty(),
                    placeholder = "手机号",
                    placeholderStyle = placeholderStyle,
                    innerTextField = innerTextField
                )
            }
        )
    }
}

@Composable
private fun LoginCredentialField(
    mode: LoginCredentialMode,
    captcha: String,
    captchaCountdown: Int,
    isSendingCaptcha: Boolean,
    password: String,
    showPassword: Boolean,
    onCaptchaChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleShowPassword: () -> Unit,
    onSwitchMode: () -> Unit,
    onSendCaptcha: () -> Unit,
    onDismissKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    LoginInputRow(modifier = modifier) {
        LoginFieldLeadingIcon(
            icon = if (mode == LoginCredentialMode.Captcha) {
                Icons.Outlined.MailOutline
            } else {
                Icons.Outlined.Lock
            },
            contentDescription = if (mode == LoginCredentialMode.Captcha) {
                "切换至密码登录"
            } else {
                "切换至验证码登录"
            },
            onClick = onSwitchMode
        )
        BasicTextField(
            value = if (mode == LoginCredentialMode.Captcha) captcha else password,
            onValueChange = {
                if (mode == LoginCredentialMode.Captcha) {
                    onCaptchaChange(it)
                } else {
                    onPasswordChange(it)
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            textStyle = LoginFieldTextStyle(color = colorScheme.onSurface),
            cursorBrush = SolidColor(colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (mode == LoginCredentialMode.Captcha) {
                    KeyboardType.Number
                } else {
                    KeyboardType.Password
                },
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDismissKeyboard() }
            ),
            visualTransformation = if (mode == LoginCredentialMode.Password && !showPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            singleLine = true,
            decorationBox = { innerTextField ->
                LoginFieldDecorationBox(
                    showPlaceholder = if (mode == LoginCredentialMode.Captcha) {
                        captcha.isEmpty()
                    } else {
                        password.isEmpty()
                    },
                    placeholder = if (mode == LoginCredentialMode.Captcha) "验证码" else "密码",
                    placeholderStyle = LoginFieldTextStyle(
                        color = colorScheme.onSurface.copy(alpha = 0.35f)
                    ),
                    innerTextField = innerTextField
                )
            }
        )

        if (mode == LoginCredentialMode.Password) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onToggleShowPassword
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (showPassword) {
                        Icons.Outlined.VisibilityOff
                    } else {
                        Icons.Outlined.Visibility
                    },
                    contentDescription = if (showPassword) "隐藏密码" else "显示密码",
                    modifier = Modifier.size(17.dp),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }
        } else {
            Text(
                text = when {
                    captchaCountdown > 0 -> "${captchaCountdown}s"
                    else -> "获取验证码"
                },
                modifier = Modifier
                    .clickable(
                        enabled = captchaCountdown == 0 && !isSendingCaptcha,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSendCaptcha() }
                    .padding(horizontal = 4.dp),
                color = if (captchaCountdown == 0 && !isSendingCaptcha) {
                    colorScheme.onSurface
                } else {
                    colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// 与手机号行同一套左侧图标布局：18.dp 图标 + 12.dp 间距
@Composable
private fun RowScope.LoginFieldLeadingIcon(
    icon: ImageVector,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier
            .size(18.dp)
            .then(
                if (onClick != null) {
                    Modifier.pointerInput(onClick) {
                        detectTapGestures { onClick() }
                    }
                } else {
                    Modifier
                }
            ),
        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    )
    Spacer(modifier = Modifier.width(12.dp))
}

@Composable
private fun LoginFieldDecorationBox(
    showPlaceholder: Boolean,
    placeholder: String,
    placeholderStyle: TextStyle,
    innerTextField: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        if (showPlaceholder) {
            Text(
                text = placeholder,
                style = placeholderStyle
            )
        }
        innerTextField()
    }
}

private fun LoginFieldTextStyle(color: Color): TextStyle {
    return TextStyle(
        color = color,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
}

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
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.primary)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "登录",
            color = colorScheme.onPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LoginAgreement(
    agreed: Boolean,
    onAgreedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onAgreedChange(!agreed) }
                .padding(top = 3.dp)
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (agreed) colorScheme.onBackground else Color.Transparent
                )
                .border(
                    width = 1.dp,
                    color = if (agreed) {
                        colorScheme.onBackground
                    } else {
                        colorScheme.onSurface.copy(alpha = 0.35f)
                    },
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (agreed) {
                Text(
                    text = "✓",
                    color = colorScheme.background,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = colorScheme.onBackground)) {
                    append("我已阅读并同意")
                }
                append("《服务条款》")
                withStyle(SpanStyle(color = colorScheme.onBackground)) {
                    append("、")
                }
                append("《隐私政策》")
            },
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            fontSize = 11.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoginInputRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surface.copy(alpha = 0.22f))
            .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

// 第三方登录入口（当前为占位，未接入 OAuth）
@Composable
private fun LoginThirdPartySection(
    onNeteaseClick: () -> Unit,
    onQqMusicClick: () -> Unit,
    onSpotifyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LoginThirdPartyIcon(
            iconRes = R.drawable.ic_netease,
            contentDescription = "使用网易云登录",
            onClick = onNeteaseClick
        )
        Spacer(modifier = Modifier.width(40.dp))
        LoginThirdPartyIcon(
            iconRes = R.drawable.ic_qq_music,
            contentDescription = "使用 QQ 音乐登录",
            onClick = onQqMusicClick
        )
        Spacer(modifier = Modifier.width(40.dp))
        LoginThirdPartyIcon(
            iconRes = R.drawable.ic_spotify,
            contentDescription = "使用 Spotify 登录",
            onClick = onSpotifyClick
        )
    }
}

@Composable
private fun LoginThirdPartyIcon(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(colorScheme.surface.copy(alpha = 0.2f))
            .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(25.dp),
            tint = Color.Unspecified
        )
    }
}
