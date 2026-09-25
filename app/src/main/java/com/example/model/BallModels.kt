package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Individual bouncing ball model with position, velocity, and visual properties.
 */
class Ball(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var color: Long,
    var cooldown: Int = 8 // Collision cooldown frames to prevent edge-sticking
)

/**
 * Expanding ripple effect generated when a ball hits a wall and multiplies.
 */
class Shockwave(
    val x: Float,
    val y: Float,
    var radius: Float,
    val maxRadius: Float,
    var alpha: Float,
    val color: Long
)

/**
 * Predefined color schemes for customization.
 */
enum class ColorThemePreset(val displayName: String, val colors: List<Long>) {
    RAINBOW(
        "彩虹霓虹",
        listOf(
            0xFFFF1744, 0xFFFF9100, 0xFFFFEA00, 0xFF00E676,
            0xFF00E5FF, 0xFF2979FF, 0xFFD500F9, 0xFFFF4081
        )
    ),
    CYBERPUNK(
        "赛博幻蓝",
        listOf(0xFF00F2FE, 0xFF4FACFE, 0xFF00C9FF, 0xFF92FE9D, 0xFF00E5FF)
    ),
    PASTEL(
        "梦幻马卡龙",
        listOf(0xFFFFB3BA, 0xFFFFDFBA, 0xFFFFFFBA, 0xFFBAFFC9, 0xFFBAE1FF, 0xFFE8BAFF)
    ),
    SUNSET(
        "日落炽焰",
        listOf(0xFFFF3366, 0xFFFF6B6B, 0xFFFFA07A, 0xFFFFD93D, 0xFFFF4500)
    ),
    EMERALD(
        "翡翠清泉",
        listOf(0xFF00F5D4, 0xFF00BBF9, 0xFF52B788, 0xFF74C69D, 0xFF95D5B2)
    ),
    COSMIC(
        "幻境星云",
        listOf(0xFF9D4EDD, 0xFFC77DFF, 0xFFE0AAFF, 0xFF7B2CBF, 0xFFF72585)
    ),
    GOLDEN(
        "流金璀璨",
        listOf(0xFFFFD700, 0xFFFFC107, 0xFFFFE082, 0xFFFFB300, 0xFFFFF8E1)
    ),
    CUSTOM(
        "自定义纯色",
        listOf(0xFF38BDF8)
    );

    fun getColor(index: Int): Long {
        return colors[index % colors.size]
    }
}

/**
 * Visual rendering styles of the balls.
 */
enum class BallStyle(val displayName: String, val icon: String) {
    GLOW_ORB("发光粒子", "✨"),
    CRYSTAL_GLASS("水晶晶球", "🔮"),
    BUBBLE("梦幻气泡", "🫧"),
    STAR("闪耀星辰", "⭐"),
    FIRE("炽热火球", "🔥"),
    GEM("能量宝石", "💎")
}

/**
 * Giant transparent container shapes.
 */
enum class ContainerShape(val displayName: String, val icon: String) {
    CIRCLE("巨型水晶球", "⚪"),
    HEXAGON("超大六边形", "⬡"),
    OCTAGON("巨型八角仓", "🛑"),
    ROUNDED_TANK("超大圆角缸", "🔲")
}

/**
 * Container scale settings to make it "很大很大".
 */
enum class ContainerScale(val displayName: String, val scaleFactor: Float) {
    STANDARD("适中", 0.85f),
    LARGE("巨大", 0.94f),
    MASSIVE("超巨大 (沉浸)", 0.98f)
}

/**
 * Multiplication factor setting.
 */
enum class MultiplierMode(val displayName: String, val factor: Int) {
    DOUBLE("2倍指数翻倍 (2→4→8...)", 2),
    TRIPLE("3倍指数激增 (2→6→18...)", 3),
    QUADRUPLE("4倍爆发式增长 (2→8→32...)", 4)
}

/**
 * Simulation configuration and settings.
 */
data class SimulationConfig(
    val colorTheme: ColorThemePreset = ColorThemePreset.RAINBOW,
    val customColor: Long = 0xFF38BDF8,
    val ballStyle: BallStyle = BallStyle.GLOW_ORB,
    val containerShape: ContainerShape = ContainerShape.CIRCLE,
    val containerScale: ContainerScale = ContainerScale.LARGE,
    val multiplierMode: MultiplierMode = MultiplierMode.DOUBLE,
    val maxBalls: Int = 10000,
    val baseSpeed: Float = 1.0f,
    val hasGravity: Boolean = false,
    val rotateContainer: Boolean = false,
    val autoShrinkBalls: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val trailsEnabled: Boolean = false
)
