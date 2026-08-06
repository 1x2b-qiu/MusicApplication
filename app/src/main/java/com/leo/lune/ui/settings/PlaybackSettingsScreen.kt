package com.leo.lune.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leo.lune.domain.model.PlaybackQuality
import com.leo.lune.util.consumePointersUnlessResumed

// 与下载设置页一致的列表外层卡片圆角
private val CardShape = RoundedCornerShape(24.dp)

private val BadgeShape = RoundedCornerShape(6.dp)

// 播放设置页：默认音质单选 + 与其他应用同时播放开关
@Composable
fun PlaybackSettingsScreen(
    onBack: () -> Unit,
    darkTheme: Boolean,
    viewModel: PlaybackSettingsViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .consumePointersUnlessResumed()
    ) {
        PlaybackSettingsTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionLabel(title = "默认播放音质")
            Spacer(modifier = Modifier.height(12.dp))
            PlaybackSettingsCard {
                PlaybackQuality.entries.forEach { quality ->
                    PlaybackQualityOptionRow(
                        title = quality.label,
                        detail = quality.detail,
                        badge = quality.badge,
                        selected = uiState.selectedQuality == quality,
                        onClick = { viewModel.selectQuality(quality) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel(title = "音频")
            Spacer(modifier = Modifier.height(12.dp))
            PlaybackSettingsCard {
                MixWithOthersRow(
                    enabled = uiState.mixWithOthers,
                    onCheckedChange = viewModel::setMixWithOthers
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

// 顶栏：左返回 + 居中「播放设置」（与下载设置页同结构）
@Composable
private fun PlaybackSettingsTopBar(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaybackSettingsHeaderIconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "返回",
                tint = colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "播放设置",
            modifier = Modifier.weight(1f),
            color = colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.3).sp,
            textAlign = TextAlign.Center
        )
        // 与左侧返回按钮等宽，保证标题视觉居中
        Spacer(modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun SectionLabel(title: String) {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        text = title,
        color = colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

// 列表外层卡片外壳（surfaceVariant + 细描边，对齐下载设置页）
@Composable
private fun PlaybackSettingsCard(
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colorScheme.surfaceVariant)
            .border(0.67.dp, colorScheme.outlineVariant, CardShape)
    ) {
        content()
    }
}

// 音质单选行：布局对齐下载设置 QualityOptionRow，右侧保留推荐 / HiFi badge
@Composable
private fun PlaybackQualityOptionRow(
    title: String,
    detail: String,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val badgeBg = colorScheme.onBackground.copy(alpha = if (selected) 0.12f else 0.06f)
    val badgeFg = colorScheme.onBackground.copy(alpha = if (selected) 0.85f else 0.35f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    width = 1.5.dp,
                    color = if (selected) {
                        colorScheme.onBackground
                    } else {
                        colorScheme.onBackground.copy(alpha = 0.3f)
                    },
                    shape = CircleShape
                )
                .background(
                    color = if (selected) colorScheme.onBackground else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colorScheme.background)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!badge.isNullOrBlank()) {
            Text(
                text = badge,
                color = badgeFg,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(BadgeShape)
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun MixWithOthersRow(
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!enabled) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "与其他应用同时播放",
            color = colorScheme.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        PlaybackToggle(
            checked = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

// 与定时关闭弹层开关一致：主题色轨道 + 白滑块（44×22）
@Composable
private fun PlaybackToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    // 轨道 44×22，滑块 16，左右各留 3dp 边距
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 25.dp else 3.dp,
        animationSpec = tween(180),
        label = "playbackToggleThumb"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            colorScheme.primary
        } else {
            colorScheme.onSurface.copy(alpha = 0.12f)
        },
        animationSpec = tween(180),
        label = "playbackToggleTrack"
    )

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(22.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(16.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.18f),
                    spotColor = Color.Black.copy(alpha = 0.22f)
                )
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

// 与下载设置页一致的圆形图标按钮
@Composable
private fun PlaybackSettingsHeaderIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colorScheme.surfaceVariant)
            .border(0.67.dp, colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
