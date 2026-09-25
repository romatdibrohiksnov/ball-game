package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import kotlin.math.roundToInt
import com.example.model.BallStyle
import com.example.model.ColorThemePreset
import com.example.model.ContainerScale
import com.example.model.ContainerShape
import com.example.model.MultiplierMode
import com.example.model.SimulationConfig

val customPaletteColors = listOf(
    0xFF38BDF8, // Sky Cyan
    0xFFF43F5E, // Rose Pink
    0xFF10B981, // Emerald Green
    0xFFFBBF24, // Amber Gold
    0xFFA855F7, // Electric Purple
    0xFFFF6B6B, // Coral
    0xFF00E5FF, // Neon Aqua
    0xFFFFFFFF  // Starlight White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeSheet(
    config: SimulationConfig,
    onConfigChange: (SimulationConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        contentColor = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "球体与容器个性化定制",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "随心挑选色彩、材质、容器形态与倍增物理",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8)
                        )
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_customize_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭设置",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Color Themes
            SectionHeader(title = "🎨 球体色彩主题")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ColorThemePreset.entries.forEach { preset ->
                    val isSelected = config.colorTheme == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = { onConfigChange(config.copy(colorTheme = preset)) },
                        label = { Text(preset.displayName) },
                        leadingIcon = {
                            if (preset.colors.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.sweepGradient(preset.colors.map { Color(it) })
                                        )
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(if (preset == ColorThemePreset.CUSTOM) config.customColor else preset.colors.first()))
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2563EB),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("color_theme_${preset.name}")
                    )
                }
            }

            // Custom color selector if CUSTOM selected
            if (config.colorTheme == ColorThemePreset.CUSTOM) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "选择纯色原力：",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    customPaletteColors.forEach { colorVal ->
                        val isPicked = config.customColor == colorVal
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .clickable {
                                    onConfigChange(config.copy(customColor = colorVal))
                                }
                                .then(
                                    if (isPicked) {
                                        Modifier.border(2.5.dp, Color.White, CircleShape)
                                    } else Modifier
                                )
                                .testTag("custom_color_pick_$colorVal")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Ball Visual Style
            SectionHeader(title = "✨ 球体视觉质感")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BallStyle.entries.forEach { style ->
                    val isSelected = config.ballStyle == style
                    FilterChip(
                        selected = isSelected,
                        onClick = { onConfigChange(config.copy(ballStyle = style)) },
                        label = { Text("${style.icon} ${style.displayName}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("ball_style_${style.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Container Shape & Size
            SectionHeader(title = "🔮 透明容器形状")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContainerShape.entries.forEach { shape ->
                    val isSelected = config.containerShape == shape
                    FilterChip(
                        selected = isSelected,
                        onClick = { onConfigChange(config.copy(containerShape = shape)) },
                        label = { Text("${shape.icon} ${shape.displayName}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF7C3AED),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("container_shape_${shape.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Container Scale ("容器要很大很大")
            SectionHeader(title = "📐 容器沉浸容积 (很大很大)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContainerScale.entries.forEach { scale ->
                    val isSelected = config.containerScale == scale
                    FilterChip(
                        selected = isSelected,
                        onClick = { onConfigChange(config.copy(containerScale = scale)) },
                        label = { Text(scale.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("container_scale_${scale.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Rotate Container Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "容器自转模式",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "让透明容器缓慢自转，产生离心旋转碰撞",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                    )
                }
                Switch(
                    checked = config.rotateContainer,
                    onCheckedChange = { onConfigChange(config.copy(rotateContainer = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF38BDF8)),
                    modifier = Modifier.testTag("switch_rotate_container")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Multiplication & Max Balls
            SectionHeader(title = "💥 碰撞分裂倍数 (翻倍模式)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MultiplierMode.entries.forEach { mode ->
                    val isSelected = config.multiplierMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onConfigChange(config.copy(multiplierMode = mode)) },
                        label = { Text(mode.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDC2626),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("multiplier_mode_${mode.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Max Balls Limit Selection & Custom Free Adjustment
            SectionHeader(title = "🔢 球体数量上限 (自由调节 & 超万球支持)")

            // Quick Presets Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5000, 10000, 20000, 50000, 100000).forEach { limit ->
                    val isSelected = config.maxBalls == limit
                    val label = when (limit) {
                        5000 -> "5,000 颗"
                        10000 -> "10,000 颗 (推荐)"
                        20000 -> "20,000 颗"
                        50000 -> "50,000 颗 (超载星群)"
                        100000 -> "100,000 颗 (极致星云)"
                        else -> "$limit 颗"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onConfigChange(config.copy(maxBalls = limit)) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF059669),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("max_balls_$limit")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Custom free-form adjustment header & steppers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "当前自定义上限: ${NumberFormat.getInstance().format(config.maxBalls)} 颗",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    ),
                    modifier = Modifier.testTag("custom_max_balls_label")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val newL = (config.maxBalls - 1000).coerceAtLeast(500)
                            onConfigChange(config.copy(maxBalls = newL))
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF334155),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("btn_minus_1k")
                    ) {
                        Text("-1K", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    FilledTonalButton(
                        onClick = {
                            val newL = (config.maxBalls + 1000).coerceAtMost(100000)
                            onConfigChange(config.copy(maxBalls = newL))
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF1E3A8A),
                            contentColor = Color(0xFF93C5FD)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("btn_plus_1k")
                    ) {
                        Text("+1K", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    FilledTonalButton(
                        onClick = {
                            val newL = (config.maxBalls + 10000).coerceAtMost(100000)
                            onConfigChange(config.copy(maxBalls = newL))
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF065F46),
                            contentColor = Color(0xFF6EE7B7)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("btn_plus_10k")
                    ) {
                        Text("+10K", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Continuous Slider from 500 to 100,000
            Slider(
                value = config.maxBalls.toFloat(),
                onValueChange = {
                    val step = if (it < 10000f) 500f else if (it < 30000f) 1000f else 2500f
                    val rounded = ((it / step).roundToInt() * step).toInt().coerceIn(500, 100000)
                    onConfigChange(config.copy(maxBalls = rounded))
                },
                valueRange = 500f..100000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF10B981),
                    activeTrackColor = Color(0xFF10B981),
                    inactiveTrackColor = Color(0xFF334155)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("max_balls_continuous_slider")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Speed Slider
            Text(
                text = "运动速度: ${(config.baseSpeed * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            Slider(
                value = config.baseSpeed,
                onValueChange = { onConfigChange(config.copy(baseSpeed = it)) },
                valueRange = 0.4f..2.5f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF38BDF8),
                    activeTrackColor = Color(0xFF38BDF8),
                    inactiveTrackColor = Color(0xFF334155)
                ),
                modifier = Modifier.testTag("speed_slider")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Gravity Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "重力模式",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = if (config.hasGravity) "模拟真实向下重力" else "失重太空悬浮全向反弹",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                    )
                }
                Switch(
                    checked = config.hasGravity,
                    onCheckedChange = { onConfigChange(config.copy(hasGravity = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981)),
                    modifier = Modifier.testTag("switch_gravity")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sound FX Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "治愈碰撞音效",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "五度音律解压叮咚音阶（随球数音调升降）",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                    )
                }
                Switch(
                    checked = config.soundEnabled,
                    onCheckedChange = { onConfigChange(config.copy(soundEnabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFF59E0B)),
                    modifier = Modifier.testTag("switch_sound")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Haptic Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "碰撞触感反馈",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "轻微清脆的物理震动",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                    )
                }
                Switch(
                    checked = config.hapticEnabled,
                    onCheckedChange = { onConfigChange(config.copy(hapticEnabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFA855F7)),
                    modifier = Modifier.testTag("switch_haptic")
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            fontSize = 14.sp
        ),
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
