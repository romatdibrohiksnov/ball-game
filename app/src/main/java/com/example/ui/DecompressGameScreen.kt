package com.example.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BallSimulationEngine
import com.example.model.SimulationConfig
import com.example.sound.SoundSynthesizer
import java.text.NumberFormat

private const val PREFS_NAME = "bouncing_ball_prefs"
private const val KEY_PEAK_RECORD = "peak_record"

@Composable
fun DecompressGameScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var peakSavedRecord by remember { mutableIntStateOf(prefs.getInt(KEY_PEAK_RECORD, 1)) }

    val soundSynthesizer = remember { SoundSynthesizer() }
    val engine = remember { BallSimulationEngine() }

    var config by remember { mutableStateOf(SimulationConfig()) }
    var ballCount by remember { mutableIntStateOf(2) }
    var collisionCount by remember { mutableLongStateOf(0L) }
    var isPaused by remember { mutableStateOf(false) }
    var showCustomizeSheet by remember { mutableStateOf(false) }
    var isZenMode by remember { mutableStateOf(false) }

    // Connect config changes to engine & sound
    LaunchedEffect(config) {
        engine.config = config
        soundSynthesizer.setMuted(!config.soundEnabled)
    }

    // Throttled HUD stats refresh (10 times per second) - eliminates Compose recomposition storms!
    LaunchedEffect(engine) {
        while (true) {
            kotlinx.coroutines.delay(100)
            val currentCount = engine.balls.size
            val collisions = engine.totalCollisions
            if (ballCount != currentCount) {
                ballCount = currentCount
            }
            if (collisionCount != collisions) {
                collisionCount = collisions
            }
            if (currentCount > peakSavedRecord) {
                peakSavedRecord = currentCount
                prefs.edit().putInt(KEY_PEAK_RECORD, currentCount).apply()
            }
        }
    }

    // Connect audio & haptic to collisions (Zero Compose Recomposition overhead!)
    DisposableEffect(Unit) {
        var lastHapticMs = 0L
        engine.onCollision = { currentCount, _ ->
            // Play procedural chime
            soundSynthesizer.playBounceChime(currentCount)

            // Throttled haptic click
            if (config.hapticEnabled) {
                val now = System.currentTimeMillis()
                if (now - lastHapticMs > 50) {
                    lastHapticMs = now
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (_: Exception) {}
                }
            }
        }
        onDispose {
            engine.onCollision = null
            soundSynthesizer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
    ) {
        // 1. Interactive Canvas with Huge Container & Multiplying Balls
        BallCanvasView(
            engine = engine,
            modifier = Modifier.fillMaxSize(),
            onTapSpawn = { tapX, tapY ->
                engine.addBalls(5, tapX, tapY)
                ballCount = engine.balls.size
                if (config.hapticEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        )

        // 2. Top HUD Bar (Score, Record, Quick Controls)
        AnimatedVisibility(
            visible = !isZenMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TopHudBar(
                ballCount = ballCount,
                maxBalls = config.maxBalls,
                collisionCount = collisionCount,
                peakRecord = peakSavedRecord,
                multiplierMode = config.multiplierMode,
                soundEnabled = config.soundEnabled,
                onToggleSound = {
                    config = config.copy(soundEnabled = !config.soundEnabled)
                },
                onToggleZenMode = { isZenMode = true },
                onOpenCustomize = { showCustomizeSheet = true }
            )
        }

        // Zen Mode restore button (when HUD is hidden)
        if (isZenMode) {
            IconButton(
                onClick = { isZenMode = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color(0x800F172A))
                    .testTag("exit_zen_mode_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "退出沉浸模式",
                    tint = Color.White
                )
            }
        }

        // Center Hint for Initial State (Disappears once bouncing starts)
        if (ballCount <= 2 && collisionCount == 0L) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x700F172A))
                    .border(1.dp, Color(0x3038BDF8), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "💡 初始2颗小球高速碰撞 • 碰到壁面裂变翻倍 • 碰撞小球相互反弹",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFBAE6FD),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // 3. Bottom Control Toolbar (Reset, Refresh, Pause, Customization, Add)
        AnimatedVisibility(
            visible = !isZenMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BottomControlBar(
                isPaused = isPaused,
                onReset = {
                    engine.reset(2)
                    ballCount = engine.balls.size
                    collisionCount = engine.totalCollisions
                    isPaused = false
                    engine.isPaused = false
                },
                onRefresh = {
                    engine.refresh()
                    ballCount = engine.balls.size
                    isPaused = false
                    engine.isPaused = false
                },
                onTogglePause = {
                    isPaused = !isPaused
                    engine.isPaused = isPaused
                },
                onAddOneBall = {
                    engine.addBalls(1)
                    ballCount = engine.balls.size
                },
                onAddFiveBalls = {
                    engine.addBalls(5)
                    ballCount = engine.balls.size
                },
                onOpenCustomize = { showCustomizeSheet = true }
            )
        }

        // 4. Modal Customization Bottom Sheet
        if (showCustomizeSheet) {
            CustomizeSheet(
                config = config,
                onConfigChange = { newConfig -> config = newConfig },
                onDismiss = { showCustomizeSheet = false }
            )
        }
    }
}

/**
 * Top HUD Bar displaying ball count, collision metrics, and quick actions.
 */
@Composable
private fun TopHudBar(
    ballCount: Int,
    maxBalls: Int,
    collisionCount: Long,
    peakRecord: Int,
    multiplierMode: com.example.model.MultiplierMode = com.example.model.MultiplierMode.DOUBLE,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onToggleZenMode: () -> Unit,
    onOpenCustomize: () -> Unit
) {
    val numberFormatter = remember { NumberFormat.getNumberInstance() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color(0x2538BDF8), RoundedCornerShape(24.dp)),
        color = Color(0xCC0F172A),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ball count badge (clickable to adjust max limit)
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenCustomize() }
                    .padding(vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "小球: ${numberFormatter.format(ballCount)} / ${numberFormatter.format(maxBalls)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier.testTag("ball_count_text")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "调整上限",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    if (ballCount >= maxBalls) {
                        Text(
                            text = " (已达上限)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFF43F5E),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "碰撞: ${numberFormatter.format(collisionCount)}次",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                    )
                    Text(
                        text = "• ${multiplierMode.factor}x指数倍增",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "• 最佳: ${numberFormatter.format(peakRecord)}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFFBBF24))
                    )
                }
            }

            // Action icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier.testTag("toggle_sound_button")
                ) {
                    Icon(
                        imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = "音效开关",
                        tint = if (soundEnabled) Color(0xFF38BDF8) else Color(0xFF64748B)
                    )
                }

                IconButton(
                    onClick = onOpenCustomize,
                    modifier = Modifier.testTag("open_customize_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "定制球体与容器",
                        tint = Color(0xFFA855F7)
                    )
                }

                IconButton(
                    onClick = onToggleZenMode,
                    modifier = Modifier.testTag("toggle_zen_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "全屏沉浸",
                        tint = Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}

/**
 * Bottom Floating Action Bar with Reset, Refresh, Pause, and Add Balls.
 */
@Composable
private fun BottomControlBar(
    isPaused: Boolean,
    onReset: () -> Unit,
    onRefresh: () -> Unit,
    onTogglePause: () -> Unit,
    onAddOneBall: () -> Unit,
    onAddFiveBalls: () -> Unit,
    onOpenCustomize: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .border(1.2.dp, Color(0x35818CF8), RoundedCornerShape(28.dp)),
        color = Color(0xDD0F172A),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset Button (重置按钮)
            FilledTonalButton(
                onClick = onReset,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF334155),
                    contentColor = Color(0xFFF1F5F9)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "重置",
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "重置", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            // Refresh Button (刷新按钮)
            FilledTonalButton(
                onClick = onRefresh,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF1E3A8A),
                    contentColor = Color(0xFF93C5FD)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新",
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "刷新", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            // Pause / Play Button
            IconButton(
                onClick = onTogglePause,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isPaused) Color(0xFF10B981) else Color(0xFF334155))
                    .testTag("pause_play_button")
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "继续" else "暂停",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Add 1 Ball Button (+1球)
            FilledTonalButton(
                onClick = onAddOneBall,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF0369A1),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("add_one_ball_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "加1球",
                    modifier = Modifier.size(15.dp)
                )
                Text(text = "1", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Add 5 Balls Button (+5球)
            FilledTonalButton(
                onClick = onAddFiveBalls,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("add_five_balls_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "加5球",
                    modifier = Modifier.size(15.dp)
                )
                Text(text = "5", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Customize Theme Button (不同颜色球体自定义)
            IconButton(
                onClick = onOpenCustomize,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                        )
                    )
                    .testTag("bottom_customize_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "自定义球体与容器",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
