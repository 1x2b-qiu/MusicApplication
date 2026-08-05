package com.leo.lune.ui.component.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

private val SheetShape = RoundedCornerShape(28.dp)
private val OptionShape = RoundedCornerShape(16.dp)
private val ConfirmShape = RoundedCornerShape(16.dp)

private val WheelItemHeight = 40.dp
private const val WheelVisibleCount = 3

// 定时关闭选项（UI 骨架，暂未接入真实计时）
enum class SleepTimerOption(
    val label: String
) {
    Minutes15("15 分钟"),
    Minutes30("30 分钟"),
    Minutes45("45 分钟"),
    Minutes60("60 分钟"),
    Custom("自定义")
}

/**
 * 定时关闭底部弹层：风格对齐下载音质弹层
 * 预设分钟单选 + 确认；点「自定义」再弹出独立时间拨轮弹层
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    onDismiss: () -> Unit,
    // 确认回调预留；功能未接时由调用方直接 dismiss 即可
    onConfirm: (SleepTimerOption, customMinutes: Int?) -> Unit = { _, _ -> }
) {
    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // 默认选中 15 分钟
    var selected by remember { mutableStateOf(SleepTimerOption.Minutes15) }
    // 点击「自定义」后叠一层时间选择弹窗
    var customPickerOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.background,
        tonalElevation = 0.dp,
        scrimColor = colorScheme.scrim.copy(alpha = 0.55f),
        dragHandle = null,
        shape = SheetShape,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.background, SheetShape)
                .border(1.dp, colorScheme.primary, SheetShape)
                .padding(20.dp)
        ) {
            Text(
                text = "定时关闭",
                color = colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SleepTimerOption.entries.forEach { option ->
                    SleepTimerOptionRow(
                        option = option,
                        selected = selected == option,
                        onClick = {
                            if (option == SleepTimerOption.Custom) {
                                // 自定义：另开时间选择弹层，不在本层展开拨轮
                                customPickerOpen = true
                            } else {
                                selected = option
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ConfirmShape)
                    .background(colorScheme.primary)
                    .clickable {
                        if (selected == SleepTimerOption.Custom) {
                            customPickerOpen = true
                        } else {
                            onConfirm(selected, null)
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "确认",
                    color = colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (customPickerOpen) {
        SleepTimerCustomPickerSheet(
            onDismiss = { customPickerOpen = false },
            onConfirm = { totalMinutes ->
                customPickerOpen = false
                selected = SleepTimerOption.Custom
                onConfirm(SleepTimerOption.Custom, totalMinutes)
            }
        )
    }
}

/**
 * 自定义时长弹层：时 / 分拨轮 + 确认（叠在定时关闭弹层之上）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerCustomPickerSheet(
    onDismiss: () -> Unit,
    onConfirm: (totalMinutes: Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(30) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.background,
        // 关闭色调抬升，避免叠层弹窗表面被 surface tint 染成与主弹层不一致
        tonalElevation = 0.dp,
        scrimColor = colorScheme.scrim.copy(alpha = 0.55f),
        dragHandle = null,
        shape = SheetShape,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.background, SheetShape)
                .border(1.dp, colorScheme.primary, SheetShape)
                .padding(20.dp)
        ) {
            Text(
                text = "自定义时长",
                color = colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            DurationWheelPicker(
                hours = hours,
                minutes = minutes,
                onHoursChange = { hours = it },
                onMinutesChange = { minutes = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ConfirmShape)
                    .background(colorScheme.primary)
                    .clickable { onConfirm(hours * 60 + minutes) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "确认",
                    color = colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SleepTimerOptionRow(
    option: SleepTimerOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OptionShape)
            .then(
                if (selected) {
                    Modifier.background(colorScheme.secondaryContainer)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = option.label,
            modifier = Modifier.weight(1f),
            color = colorScheme.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Box(
            modifier = Modifier
                .size(17.dp)
                .border(
                    width = 1.dp,
                    color = if (selected) colorScheme.primary else colorScheme.outlineVariant,
                    shape = CircleShape
                )
                .then(
                    if (selected) {
                        Modifier.background(colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(colorScheme.onPrimary, CircleShape)
                )
            }
        }
    }
}

// 时 / 分双列拨轮，居中高亮当前值（无外层卡片容器）
@Composable
private fun DurationWheelPicker(
    hours: Int,
    minutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NumberWheel(
            value = hours,
            range = 0..23,
            onValueChange = onHoursChange,
            modifier = Modifier.width(88.dp)
        )
        Text(
            text = "时",
            color = colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(end = 16.dp)
        )
        NumberWheel(
            value = minutes,
            range = 0..59,
            onValueChange = onMinutesChange,
            modifier = Modifier.width(88.dp)
        )
        Text(
            text = "分",
            color = colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberWheel(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val values = remember(range) { range.toList() }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (value - range.first).coerceIn(0, values.lastIndex)
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val pickerHeight = WheelItemHeight * WheelVisibleCount

    // 滚动停止后，取最靠近视口中心的项
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                val layoutInfo = listState.layoutInfo
                val viewportCenter = layoutInfo.viewportStartOffset +
                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                val centered = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    abs((item.offset + item.size / 2) - viewportCenter)
                } ?: return@collect
                val next = values.getOrNull(centered.index) ?: return@collect
                if (next != value) onValueChange(next)
            }
    }

    // 外部 value 变化时对齐列表（如初次进入）
    LaunchedEffect(value) {
        val index = (value - range.first).coerceIn(0, values.lastIndex)
        if (listState.firstVisibleItemIndex != index || listState.firstVisibleItemScrollOffset != 0) {
            listState.animateScrollToItem(index)
        }
    }

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset +
                (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                abs((item.offset + item.size / 2) - viewportCenter)
            }?.index ?: listState.firstVisibleItemIndex
        }
    }

    Box(
        modifier = modifier.height(pickerHeight),
        contentAlignment = Alignment.Center
    ) {
        // 中线高亮条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelItemHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(colorScheme.onBackground.copy(alpha = 0.06f))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = WheelItemHeight),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(values.size) { index ->
                val number = values[index]
                val isCentered = index == centeredIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WheelItemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "%02d".format(number),
                        color = if (isCentered) {
                            colorScheme.onBackground
                        } else {
                            colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                        fontSize = if (isCentered) 20.sp else 16.sp,
                        fontWeight = if (isCentered) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
