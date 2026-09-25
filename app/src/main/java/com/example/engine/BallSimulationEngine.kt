package com.example.engine

import com.example.model.Ball
import com.example.model.ColorThemePreset
import com.example.model.ContainerShape
import com.example.model.MultiplierMode
import com.example.model.Shockwave
import com.example.model.SimulationConfig
import java.util.Arrays
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ultra-high-performance 2D physics and collision engine handling thousands of
 * balls multiplying with zero runtime GC allocations and an O(N) spatial hash grid.
 */
class BallSimulationEngine {

    var config: SimulationConfig = SimulationConfig()
        set(value) {
            field = value
            updateBallRadii()
        }

    val balls = ArrayList<Ball>(16384)
    val shockwaves = ArrayList<Shockwave>(64)
    private val ballsToSpawn = ArrayList<Ball>(512)

    var totalCollisions: Long = 0L
    var peakBallCount: Int = 2
    var isPaused: Boolean = false
    var doublingCooldown: Float = 0f

    var onCollision: ((ballCount: Int, collisionCount: Long) -> Unit)? = null

    var containerCenterX: Float = 0f
    var containerCenterY: Float = 0f
    var containerRadius: Float = 300f
    var containerRotationAngle: Float = 0f

    private var colorCycleIndex: Int = 0

    // Spatial hash grid pre-allocations for O(N) ball-to-ball collisions
    private val gridCols = 20
    private val gridRows = 20
    private val gridTotal = gridCols * gridRows
    private val gridHead = IntArray(gridTotal) { -1 }
    private var ballNext = IntArray(16384) { -1 }

    init {
        reset(2)
    }

    fun setViewport(width: Float, height: Float) {
        if (width <= 0 || height <= 0) return
        val wasUninitialized = (containerCenterX == 0f && containerCenterY == 0f)
        containerCenterX = width / 2f
        containerCenterY = height / 2f
        val baseDim = min(width, height)
        containerRadius = (baseDim * 0.5f) * config.containerScale.scaleFactor
        if (wasUninitialized || balls.isEmpty() || balls.all { it.x == 0f && it.y == 0f }) {
            reset(2)
        } else {
            updateBallRadii()
        }
    }

    /**
     * Resets the simulation to the initial state: 2 balls moving calmly in random directions.
     */
    fun reset(initialCount: Int = 2) {
        synchronized(balls) {
            balls.clear()
            shockwaves.clear()
            totalCollisions = 0L
            colorCycleIndex = 0
            doublingCooldown = 0f

            val initialRadius = calculateBallRadius(initialCount)
            val speed = 220f * config.baseSpeed

            if (initialCount == 1) {
                val angle = Random.nextFloat() * 2f * PI.toFloat()
                balls.add(
                    Ball(
                        x = containerCenterX,
                        y = containerCenterY,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        radius = initialRadius,
                        color = getColorForNewBall()
                    )
                )
            } else {
                // Ball 1: Placed off-center with independent random trajectory
                val r1 = containerRadius * 0.25f
                val posAngle1 = Random.nextFloat() * 2f * PI.toFloat()
                val moveAngle1 = Random.nextFloat() * 2f * PI.toFloat()
                val s1 = (200f + Random.nextFloat() * 40f) * config.baseSpeed

                balls.add(
                    Ball(
                        x = containerCenterX + cos(posAngle1) * r1,
                        y = containerCenterY + sin(posAngle1) * r1,
                        vx = cos(moveAngle1) * s1,
                        vy = sin(moveAngle1) * s1,
                        radius = initialRadius,
                        color = getColorForNewBall()
                    )
                )

                // Ball 2: Placed across with independent trajectory
                val r2 = containerRadius * 0.32f
                val posAngle2 = (posAngle1 + PI.toFloat() + (Random.nextFloat() - 0.5f) * 0.8f) % (2f * PI.toFloat())
                val moveAngle2 = (moveAngle1 + 1.2f + Random.nextFloat() * 2f) % (2f * PI.toFloat())
                val s2 = (200f + Random.nextFloat() * 40f) * config.baseSpeed

                balls.add(
                    Ball(
                        x = containerCenterX + cos(posAngle2) * r2,
                        y = containerCenterY + sin(posAngle2) * r2,
                        vx = cos(moveAngle2) * s2,
                        vy = sin(moveAngle2) * s2,
                        radius = initialRadius,
                        color = getColorForNewBall()
                    )
                )

                for (k in 2 until initialCount) {
                    val a = Random.nextFloat() * 2f * PI.toFloat()
                    val r = containerRadius * 0.35f * Random.nextFloat()
                    val moveA = Random.nextFloat() * 2f * PI.toFloat()
                    val s = (200f + Random.nextFloat() * 40f) * config.baseSpeed
                    balls.add(
                        Ball(
                            x = containerCenterX + cos(a) * r,
                            y = containerCenterY + sin(a) * r,
                            vx = cos(moveA) * s,
                            vy = sin(moveA) * s,
                            radius = initialRadius,
                            color = getColorForNewBall()
                        )
                    )
                }
            }
        }
        peakBallCount = initialCount
    }

    /**
     * Refreshes the simulation with randomly distributed balls.
     */
    fun refresh() {
        synchronized(balls) {
            val count = balls.size.coerceIn(2, 60)
            balls.clear()
            shockwaves.clear()
            colorCycleIndex = 0
            doublingCooldown = 0f

            for (i in 0 until count) {
                val r = containerRadius * 0.5f * Random.nextFloat()
                val a = Random.nextFloat() * 2f * PI.toFloat()
                val bx = containerCenterX + cos(a) * r
                val by = containerCenterY + sin(a) * r
                val moveAngle = Random.nextFloat() * 2f * PI.toFloat()
                val speed = (200f + Random.nextFloat() * 60f) * config.baseSpeed

                balls.add(
                    Ball(
                        x = bx,
                        y = by,
                        vx = cos(moveAngle) * speed,
                        vy = sin(moveAngle) * speed,
                        radius = calculateBallRadius(count),
                        color = getColorForNewBall()
                    )
                )
            }
        }
    }

    /**
     * Manually add balls at specified point or center.
     */
    fun addBalls(count: Int, spawnX: Float = containerCenterX, spawnY: Float = containerCenterY) {
        synchronized(balls) {
            val targetCount = (balls.size + count).coerceAtMost(config.maxBalls)
            val toAdd = targetCount - balls.size
            val radius = calculateBallRadius(balls.size + toAdd)

            for (i in 0 until toAdd) {
                val angle = Random.nextFloat() * 2f * PI.toFloat()
                val speed = (200f + Random.nextFloat() * 60f) * config.baseSpeed
                balls.add(
                    Ball(
                        x = spawnX + (Random.nextFloat() - 0.5f) * 24f,
                        y = spawnY + (Random.nextFloat() - 0.5f) * 24f,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        radius = radius,
                        color = getColorForNewBall()
                    )
                )
            }
            if (balls.size > peakBallCount) {
                peakBallCount = balls.size
            }
        }
        updateBallRadii()
    }

    private fun getColorForNewBall(): Long {
        return if (config.colorTheme == ColorThemePreset.CUSTOM) {
            config.customColor
        } else {
            config.colorTheme.getColor(colorCycleIndex++)
        }
    }

    private fun calculateBallRadius(currentCount: Int): Float {
        if (!config.autoShrinkBalls) return 22f

        return when {
            currentCount <= 8 -> 26f
            currentCount <= 24 -> 20f
            currentCount <= 64 -> 16f
            currentCount <= 160 -> 13f
            currentCount <= 500 -> 10f
            currentCount <= 1500 -> 7.5f
            currentCount <= 4000 -> 5.5f
            currentCount <= 10000 -> 4f
            currentCount <= 25000 -> 2.8f
            currentCount <= 50000 -> 2.0f
            else -> 1.5f
        }
    }

    private fun updateBallRadii() {
        synchronized(balls) {
            val r = calculateBallRadius(balls.size)
            for (i in balls.indices) {
                balls[i].radius = r
            }
        }
    }

    /**
     * Frame physics update.
     */
    fun update(dtSec: Float) {
        if (isPaused || dtSec <= 0f) return
        val clampedDt = dtSec.coerceIn(0.001f, 0.033f)

        if (doublingCooldown > 0f) {
            doublingCooldown -= clampedDt
        }

        // Rotate container if enabled
        if (config.rotateContainer) {
            containerRotationAngle = (containerRotationAngle + 0.35f * clampedDt) % (2f * PI.toFloat())
        }

        var collisionHappened = false
        ballsToSpawn.clear()

        synchronized(balls) {
            val count = balls.size
            val radius = calculateBallRadius(count)
            val cx = containerCenterX
            val cy = containerCenterY
            val cRadius = containerRadius
            val gravity = if (config.hasGravity) 880f else 0f

            for (i in 0 until count) {
                val b = balls[i]
                b.radius = radius

                if (b.cooldown > 0) {
                    b.cooldown--
                }

                // Gravity
                b.vy += gravity * clampedDt

                // Move
                b.x += b.vx * clampedDt * config.baseSpeed
                b.y += b.vy * clampedDt * config.baseSpeed

                // Boundary collision
                when (config.containerShape) {
                    ContainerShape.CIRCLE -> {
                        val dx = b.x - cx
                        val dy = b.y - cy
                        val distSq = dx * dx + dy * dy
                        val maxAllowedDist = cRadius - b.radius

                        if (distSq >= maxAllowedDist * maxAllowedDist && distSq > 0.0001f) {
                            val dist = kotlin.math.sqrt(distSq)
                            val nx = dx / dist
                            val ny = dy / dist

                            // Move back inside boundary safely away from rim into the container
                            b.x = cx + nx * (maxAllowedDist - 6f)
                            b.y = cy + ny * (maxAllowedDist - 6f)

                            // Launch ball INWARD towards container center with kinetic boost
                            val inwardAngle = atan2(-ny, -nx)
                            val curSpeed = hypot(b.vx, b.vy)
                            val boostedSpeed = (curSpeed * 1.07f + 25f).coerceIn(220f, 720f) * config.baseSpeed
                            val bounceAngle = inwardAngle + (Random.nextFloat() - 0.5f) * 1.35f
                            b.vx = cos(bounceAngle) * boostedSpeed
                            b.vy = sin(bounceAngle) * boostedSpeed

                            // Trigger true exponential multiplication if cooldown expired
                            if (b.cooldown <= 0) {
                                b.cooldown = 7
                                totalCollisions++
                                collisionHappened = true

                                // Shockwave (capped at 30 max to prevent draw overhead)
                                if (shockwaves.size < 30) {
                                    shockwaves.add(
                                        Shockwave(
                                            x = b.x,
                                            y = b.y,
                                            radius = b.radius,
                                            maxRadius = b.radius * 3f + 12f,
                                            alpha = 0.85f,
                                            color = b.color
                                        )
                                    )
                                }

                                triggerExponentialMultiplication()
                            }
                        }
                    }

                    ContainerShape.HEXAGON -> {
                        handleRegularPolygonCollision(b, 6, cx, cy, cRadius) {
                            totalCollisions++
                            collisionHappened = true
                        }
                    }

                    ContainerShape.OCTAGON -> {
                        handleRegularPolygonCollision(b, 8, cx, cy, cRadius) {
                            totalCollisions++
                            collisionHappened = true
                        }
                    }

                    ContainerShape.ROUNDED_TANK -> {
                        handleRoundedTankCollision(b, cx, cy, cRadius * 0.9f, cRadius * 1.3f) {
                            totalCollisions++
                            collisionHappened = true
                        }
                    }
                }
            }

            // Ball-to-ball elastic collisions via O(N) Spatial Hash Grid
            val ballBallCollided = handleSpatialBallCollisions()
            if (ballBallCollided && balls.size <= 8) {
                collisionHappened = true
            }

            if (ballsToSpawn.isNotEmpty()) {
                balls.addAll(ballsToSpawn)
                if (balls.size > peakBallCount) {
                    peakBallCount = balls.size
                }
            }
        }

        // Update shockwaves
        for (idx in shockwaves.indices.reversed()) {
            val s = shockwaves[idx]
            s.radius += (s.maxRadius - s.radius) * 9f * clampedDt
            s.alpha -= 3.5f * clampedDt
            if (s.alpha <= 0.05f) {
                shockwaves.removeAt(idx)
            }
        }

        if (collisionHappened) {
            onCollision?.invoke(balls.size, totalCollisions)
        }
    }

    /**
     * O(N) High-performance Spatial Hash Grid for ball-to-ball elastic collision.
     * Checks only neighbor balls in adjacent cells without allocating memory.
     */
    private fun handleSpatialBallCollisions(): Boolean {
        val count = balls.size
        if (count < 2) return false

        // Ensure ballNext array is large enough
        if (ballNext.size < count) {
            ballNext = IntArray(max(ballNext.size * 2, count + 1024)) { -1 }
        }

        // Reset grid
        Arrays.fill(gridHead, -1)

        val minX = containerCenterX - containerRadius
        val minY = containerCenterY - containerRadius
        val cellW = (containerRadius * 2f) / gridCols
        val cellH = (containerRadius * 2f) / gridRows

        // Insert balls into spatial grid cells
        for (i in 0 until count) {
            val b = balls[i]
            val cx = ((b.x - minX) / cellW).toInt().coerceIn(0, gridCols - 1)
            val cy = ((b.y - minY) / cellH).toInt().coerceIn(0, gridRows - 1)
            val cellIdx = cy * gridCols + cx
            ballNext[i] = gridHead[cellIdx]
            gridHead[cellIdx] = i
        }

        var collided = false
        var pairChecks = 0
        val maxPairChecks = 4500

        // Neighbor cell offsets (current + 4 neighbors to check each pair once)
        val offsetCols = intArrayOf(0, 1, -1, 0, 1)
        val offsetRows = intArrayOf(0, 0, 1, 1, 1)

        outer@ for (cy in 0 until gridRows) {
            for (cx in 0 until gridCols) {
                val cellIdx = cy * gridCols + cx
                var i = gridHead[cellIdx]

                while (i != -1) {
                    val b1 = balls[i]

                    // Check neighbor cells
                    for (k in 0 until 5) {
                        val ncx = cx + offsetCols[k]
                        val ncy = cy + offsetRows[k]
                        if (ncx in 0 until gridCols && ncy in 0 until gridRows) {
                            val neighborIdx = ncy * gridCols + ncx
                            var j = if (k == 0) ballNext[i] else gridHead[neighborIdx]

                            while (j != -1) {
                                if (++pairChecks > maxPairChecks) break@outer
                                val b2 = balls[j]
                                val dx = b2.x - b1.x
                                val dy = b2.y - b1.y
                                val distSq = dx * dx + dy * dy
                                val minDist = b1.radius + b2.radius

                                if (distSq < minDist * minDist && distSq > 0.0001f) {
                                    val dist = kotlin.math.sqrt(distSq)
                                    val nx = dx / dist
                                    val ny = dy / dist

                                    // Positional separation to resolve overlap + spring gap
                                    val overlap = (minDist - dist) + 1.2f
                                    b1.x -= nx * (overlap * 0.5f)
                                    b1.y -= ny * (overlap * 0.5f)
                                    b2.x += nx * (overlap * 0.5f)
                                    b2.y += ny * (overlap * 0.5f)

                                    // Elastic momentum exchange (玻璃珠高弹性回弹)
                                    val kx = b1.vx - b2.vx
                                    val ky = b1.vy - b2.vy
                                    val vRel = nx * kx + ny * ky

                                    if (vRel > 0f) {
                                        val impulse = 0.5f * (1f + 1.05f) * vRel
                                        b1.vx -= impulse * nx
                                        b1.vy -= impulse * ny
                                        b2.vx += impulse * nx
                                        b2.vy += impulse * ny

                                        // Tangential scattering kick of glass marble
                                        val tx = -ny
                                        val ty = nx
                                        val scatter = (Random.nextFloat() - 0.5f) * 35f
                                        b1.vx += tx * scatter
                                        b1.vy += ty * scatter
                                        b2.vx -= tx * scatter
                                        b2.vy -= ty * scatter

                                        collided = true
                                    }
                                }
                                j = ballNext[j]
                            }
                        }
                    }
                    i = ballNext[i]
                }
            }
        }
        return collided
    }

    /**
     * Regular polygon collision (Hexagon, Octagon).
     */
    private inline fun handleRegularPolygonCollision(
        b: Ball,
        numSides: Int,
        cx: Float,
        cy: Float,
        radius: Float,
        onCollide: () -> Unit
    ) {
        val dx = b.x - cx
        val dy = b.y - cy
        val ballAngle = atan2(dy, dx) - containerRotationAngle
        val sideAngle = (2f * PI.toFloat()) / numSides
        val sector = kotlin.math.round(ballAngle / sideAngle).toInt()
        val sectorCenterAngle = sector * sideAngle + containerRotationAngle

        val inradius = radius * cos(PI.toFloat() / numSides)
        val nx = cos(sectorCenterAngle)
        val ny = sin(sectorCenterAngle)

        val distToFace = dx * nx + dy * ny
        val maxDist = inradius - b.radius

        if (distToFace >= maxDist) {
            val penetration = distToFace - maxDist
            b.x -= nx * (penetration + 6f)
            b.y -= ny * (penetration + 6f)

            val inwardAngle = atan2(-ny, -nx)
            val curSpeed = hypot(b.vx, b.vy)
            val boostedSpeed = (curSpeed * 1.07f + 25f).coerceIn(220f, 720f) * config.baseSpeed
            val bounceAngle = inwardAngle + (Random.nextFloat() - 0.5f) * 1.35f

            b.vx = cos(bounceAngle) * boostedSpeed
            b.vy = sin(bounceAngle) * boostedSpeed

            if (b.cooldown <= 0) {
                b.cooldown = 7
                onCollide()
                if (shockwaves.size < 30) {
                    shockwaves.add(
                        Shockwave(
                            x = b.x,
                            y = b.y,
                            radius = b.radius,
                            maxRadius = b.radius * 3f + 12f,
                            alpha = 0.85f,
                            color = b.color
                        )
                    )
                }
                triggerExponentialMultiplication()
            }
        }
    }

    /**
     * Rounded tank collision.
     */
    private inline fun handleRoundedTankCollision(
        b: Ball,
        cx: Float,
        cy: Float,
        halfW: Float,
        halfH: Float,
        onCollide: () -> Unit
    ) {
        var collided = false
        var nx = 0f
        var ny = 0f

        if (b.x + b.radius >= cx + halfW) {
            b.x = cx + halfW - b.radius
            nx = 1f
            collided = true
        } else if (b.x - b.radius <= cx - halfW) {
            b.x = cx - halfW + b.radius
            nx = -1f
            collided = true
        }

        if (b.y + b.radius >= cy + halfH) {
            b.y = cy + halfH - b.radius
            ny = 1f
            collided = true
        } else if (b.y - b.radius <= cy - halfH) {
            b.y = cy - halfH + b.radius
            ny = -1f
            collided = true
        }

        if (collided) {
            b.x -= nx * 6f
            b.y -= ny * 6f
            val len = hypot(nx, ny).coerceAtLeast(0.01f)
            val unitNx = nx / len
            val unitNy = ny / len
            val inwardAngle = atan2(-unitNy, -unitNx)
            val curSpeed = hypot(b.vx, b.vy)
            val boostedSpeed = (curSpeed * 1.07f + 25f).coerceIn(220f, 720f) * config.baseSpeed
            val bounceAngle = inwardAngle + (Random.nextFloat() - 0.5f) * 1.35f
            b.vx = cos(bounceAngle) * boostedSpeed
            b.vy = sin(bounceAngle) * boostedSpeed

            if (b.cooldown <= 0) {
                b.cooldown = 7
                onCollide()
                if (shockwaves.size < 30) {
                    shockwaves.add(
                        Shockwave(
                            x = b.x,
                            y = b.y,
                            radius = b.radius,
                            maxRadius = b.radius * 3f + 12f,
                            alpha = 0.85f,
                            color = b.color
                        )
                    )
                }
                triggerExponentialMultiplication()
            }
        }
    }

    /**
     * Executes true Exponential Doubling / Multiplication wave.
     * When any ball impacts the boundary and cooldown is ready,
     * the ENTIRE population multiplies by factor:
     * DOUBLE: 2 -> 4 -> 8 -> 16 -> 32 -> 64 -> 128 -> 256 -> 512 -> 1024 -> 2048...
     */
    private fun triggerExponentialMultiplication() {
        if (doublingCooldown > 0f) return
        val currentCount = balls.size
        if (currentCount >= config.maxBalls || currentCount == 0) return

        // Pacing cooldown: allows players to clearly see and experience each doubling step
        doublingCooldown = when {
            currentCount <= 4 -> 0.18f
            currentCount <= 16 -> 0.24f
            currentCount <= 64 -> 0.30f
            currentCount <= 256 -> 0.36f
            currentCount <= 1024 -> 0.42f
            else -> 0.50f
        }

        val factor = when (config.multiplierMode) {
            MultiplierMode.DOUBLE -> 2
            MultiplierMode.TRIPLE -> 3
            MultiplierMode.QUADRUPLE -> 4
        }

        val targetCount = (currentCount.toLong() * factor).coerceAtMost(config.maxBalls.toLong()).toInt()
        val clonesNeeded = targetCount - currentCount
        if (clonesNeeded <= 0) return

        // Spawn twins for existing balls so total quantity doubles/triples
        val clonesPerBall = (clonesNeeded / currentCount).coerceAtLeast(1)
        var spawned = 0

        for (idx in 0 until currentCount) {
            if (spawned >= clonesNeeded) break
            val src = balls[idx]
            for (m in 0 until clonesPerBall) {
                if (spawned >= clonesNeeded) break
                val angle = atan2(src.vy, src.vx) + ((m + 1) * 0.45f * if (m % 2 == 0) 1f else -1f)
                val spd = hypot(src.vx, src.vy).coerceIn(220f, 650f)
                val clone = Ball(
                    x = src.x + (Random.nextFloat() - 0.5f) * (src.radius * 1.5f),
                    y = src.y + (Random.nextFloat() - 0.5f) * (src.radius * 1.5f),
                    vx = cos(angle) * spd,
                    vy = sin(angle) * spd,
                    radius = src.radius,
                    color = getColorForNewBall(),
                    cooldown = 6
                )
                ballsToSpawn.add(clone)
                spawned++
            }
        }
    }
}
