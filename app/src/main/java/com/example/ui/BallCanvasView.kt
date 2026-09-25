package com.example.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.engine.BallSimulationEngine
import com.example.model.Ball
import com.example.model.BallStyle
import com.example.model.ContainerShape
import com.example.model.Shockwave
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BallCanvasView(
    engine: BallSimulationEngine,
    modifier: Modifier = Modifier,
    onTapSpawn: (x: Float, y: Float) -> Unit = { _, _ -> }
) {
    var frameTick by remember { mutableLongStateOf(0L) }

    // High performance 60-120fps continuous physics loop
    LaunchedEffect(engine) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastTime != 0L) {
                    val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                    engine.update(dt)
                }
                lastTime = now
                frameTick = now
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("ball_canvas_container")
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTapSpawn(offset.x, offset.y)
                }
            }
            .drawBehind {
                // Reading frameTick inside drawBehind triggers ONLY the GPU Draw pass,
                // completely bypassing Compose recomposition and relayout for locked 60/120fps!
                @Suppress("UNUSED_VARIABLE")
                val _animTick = frameTick

                engine.setViewport(size.width, size.height)

                val cx = engine.containerCenterX
                val cy = engine.containerCenterY
                val radius = engine.containerRadius

                // 1. Draw Space / Container Ambient Atmosphere
                drawContainerAtmosphere(cx, cy, radius)

                // 2. Draw Giant Transparent Glass Container
                drawTransparentContainer(engine, cx, cy, radius)

                // 3. Draw Collision Shockwave Ripples
                drawShockwaves(engine.shockwaves)

                // 4. Draw All Bouncing Multiplying Balls with dynamic LOD
                drawBalls(engine.balls, engine.config.ballStyle)

                // 5. Draw Glass Foreground Highlights & Depth Glare
                drawContainerForegroundGlass(engine, cx, cy, radius)
            }
    )
}

/**
 * Draws ambient glowing field behind the container to highlight its immense scale.
 */
private fun DrawScope.drawContainerAtmosphere(cx: Float, cy: Float, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x1F38BDF8),
                Color(0x0C818CF8),
                Color(0x00000000)
            ),
            center = Offset(cx, cy),
            radius = radius * 1.35f
        ),
        radius = radius * 1.35f,
        center = Offset(cx, cy)
    )

    // Subtle container scale rings
    drawCircle(
        color = Color(0x1438BDF8),
        radius = radius * 0.65f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.2f)
    )
    drawCircle(
        color = Color(0x0E38BDF8),
        radius = radius * 0.35f,
        center = Offset(cx, cy),
        style = Stroke(width = 1f)
    )
}

/**
 * Draws the giant transparent crystal glass container.
 */
private fun DrawScope.drawTransparentContainer(
    engine: BallSimulationEngine,
    cx: Float,
    cy: Float,
    radius: Float
) {
    when (engine.config.containerShape) {
        ContainerShape.CIRCLE -> {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x0A0F172A),
                        Color(0x281E293B),
                        Color(0x400F172A)
                    ),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )

            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF38BDF8),
                        Color(0xFF818CF8),
                        Color(0xFFF43F5E),
                        Color(0xFF38BDF8)
                    ),
                    center = Offset(cx, cy)
                ),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 4.5f)
            )

            drawCircle(
                color = Color(0x50FFFFFF),
                radius = radius - 3.5f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )
        }

        ContainerShape.HEXAGON, ContainerShape.OCTAGON -> {
            val numSides = if (engine.config.containerShape == ContainerShape.HEXAGON) 6 else 8
            val rotAngle = engine.containerRotationAngle

            val path = Path().apply {
                val step = (2f * PI.toFloat()) / numSides
                for (i in 0 until numSides) {
                    val a = rotAngle + i * step
                    val px = cx + cos(a) * radius
                    val py = cy + sin(a) * radius
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }

            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x0E1E293B), Color(0x350F172A)),
                    center = Offset(cx, cy),
                    radius = radius
                )
            )

            drawPath(
                path = path,
                brush = Brush.sweepGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFFC084FC), Color(0xFF38BDF8)),
                    center = Offset(cx, cy)
                ),
                style = Stroke(width = 4f)
            )
        }

        ContainerShape.ROUNDED_TANK -> {
            val halfW = radius * 0.9f
            val halfH = radius * 1.3f
            val left = cx - halfW
            val top = cy - halfH

            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x0C1E293B), Color(0x320F172A)),
                    center = Offset(cx, cy),
                    radius = radius * 1.2f
                ),
                topLeft = Offset(left, top),
                size = Size(halfW * 2f, halfH * 2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(36f, 36f)
            )

            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFF38BDF8)),
                    start = Offset(left, top),
                    end = Offset(left + halfW * 2f, top + halfH * 2f)
                ),
                topLeft = Offset(left, top),
                size = Size(halfW * 2f, halfH * 2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(36f, 36f),
                style = Stroke(width = 4.5f)
            )
        }
    }
}

/**
 * Draws specular glass reflection arcs over the container.
 */
private fun DrawScope.drawContainerForegroundGlass(
    engine: BallSimulationEngine,
    cx: Float,
    cy: Float,
    radius: Float
) {
    if (engine.config.containerShape == ContainerShape.CIRCLE) {
        drawArc(
            color = Color(0x80FFFFFF),
            startAngle = 200f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(cx - radius + 8f, cy - radius + 8f),
            size = Size((radius - 8f) * 2f, (radius - 8f) * 2f),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0x35FFFFFF),
            startAngle = 40f,
            sweepAngle = 45f,
            useCenter = false,
            topLeft = Offset(cx - radius + 10f, cy - radius + 10f),
            size = Size((radius - 10f) * 2f, (radius - 10f) * 2f),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Draws shockwave ripples expanding from collision points.
 */
private fun DrawScope.drawShockwaves(shockwaves: List<Shockwave>) {
    synchronized(shockwaves) {
        for (i in shockwaves.indices) {
            val s = shockwaves[i]
            val col = Color(s.color).copy(alpha = s.alpha.coerceIn(0f, 1f))
            drawCircle(
                color = col,
                radius = s.radius,
                center = Offset(s.x, s.y),
                style = Stroke(width = (2.5f * s.alpha).coerceAtLeast(1f))
            )
        }
    }
}

/**
 * High-performance batched drawing with Level-of-Detail (LOD).
 * Automatically simplifies rendering passes as ball count increases to guarantee 60-120fps.
 */
private fun DrawScope.drawBalls(balls: List<Ball>, style: BallStyle) {
    synchronized(balls) {
        val count = balls.size
        // Level of Detail thresholds
        val highDetail = count <= 80
        val mediumDetail = count <= 250

        for (i in 0 until count) {
            val b = balls[i]
            val baseColor = Color(b.color)
            val center = Offset(b.x, b.y)
            val r = b.radius

            if (!mediumDetail) {
                // Low detail for thousands of balls: single fast native circle per ball
                drawCircle(
                    color = baseColor,
                    radius = r,
                    center = center
                )
                continue
            }

            when (style) {
                BallStyle.GLOW_ORB -> {
                    if (highDetail) {
                        drawCircle(
                            color = baseColor.copy(alpha = 0.28f),
                            radius = r * 1.55f,
                            center = center
                        )
                    }
                    drawCircle(
                        color = baseColor,
                        radius = r,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = (r * 0.32f).coerceAtLeast(1.2f),
                        center = Offset(b.x - r * 0.33f, b.y - r * 0.33f)
                    )
                }

                BallStyle.CRYSTAL_GLASS -> {
                    drawCircle(
                        color = baseColor.copy(alpha = 0.45f),
                        radius = r,
                        center = center
                    )
                    drawCircle(
                        color = baseColor,
                        radius = r,
                        center = center,
                        style = Stroke(width = (r * 0.25f).coerceIn(1.2f, 3.5f))
                    )
                    if (highDetail) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = (r * 0.28f).coerceAtLeast(1.2f),
                            center = Offset(b.x - r * 0.35f, b.y - r * 0.35f)
                        )
                    }
                }

                BallStyle.BUBBLE -> {
                    drawCircle(
                        color = baseColor.copy(alpha = 0.2f),
                        radius = r,
                        center = center
                    )
                    drawCircle(
                        color = baseColor.copy(alpha = 0.9f),
                        radius = r,
                        center = center,
                        style = Stroke(width = (r * 0.2f).coerceIn(1.2f, 3.5f))
                    )
                    if (highDetail) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.9f),
                            startAngle = 190f,
                            sweepAngle = 70f,
                            useCenter = false,
                            topLeft = Offset(b.x - r * 0.85f, b.y - r * 0.85f),
                            size = Size(r * 1.7f, r * 1.7f),
                            style = Stroke(width = (r * 0.22f).coerceIn(1f, 2.8f), cap = StrokeCap.Round)
                        )
                    }
                }

                BallStyle.STAR -> {
                    drawCircle(
                        color = baseColor,
                        radius = r,
                        center = center
                    )
                    if (highDetail) {
                        val gleamLen = r * 1.5f
                        drawLine(
                            color = Color.White.copy(alpha = 0.9f),
                            start = Offset(b.x - gleamLen, b.y),
                            end = Offset(b.x + gleamLen, b.y),
                            strokeWidth = (r * 0.22f).coerceIn(1f, 2.5f),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.9f),
                            start = Offset(b.x, b.y - gleamLen),
                            end = Offset(b.x, b.y + gleamLen),
                            strokeWidth = (r * 0.22f).coerceIn(1f, 2.5f),
                            cap = StrokeCap.Round
                        )
                    }
                }

                BallStyle.FIRE -> {
                    if (highDetail) {
                        drawCircle(
                            color = Color(0xFFFF5722).copy(alpha = 0.35f),
                            radius = r * 1.6f,
                            center = center
                        )
                    }
                    drawCircle(
                        color = baseColor,
                        radius = r,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFFFFF176),
                        radius = r * 0.5f,
                        center = center
                    )
                }

                BallStyle.GEM -> {
                    drawCircle(
                        color = baseColor,
                        radius = r,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = r * 0.55f,
                        center = center,
                        style = Stroke(width = 1.2f)
                    )
                }
            }
        }
    }
}
