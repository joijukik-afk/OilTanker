package com.oiltanker

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ============================================================================
//  CONSTANTS & HELPERS
// ============================================================================

private const val BASE_SPEED_MPS = 30f
private const val MAX_LIVES = 3

private fun hash01(seed: Int): Float {
    var x = seed * 374761393 + 668265263
    x = (x xor (x shr 13)) * 1274126177
    x = x xor (x shr 16)
    return ((x and 0x7FFFFFFF) % 10000) / 10000f
}

private val STAR_FIELD: List<Triple<Float, Float, Float>> =
    (0 until 70).map { i ->
        Triple(hash01(i * 3 + 1), hash01(i * 7 + 5), 0.35f + hash01(i * 11 + 9) * 0.9f)
    }

private fun overlap(
    ax: Float, ay: Float, aw: Float, ah: Float,
    bx: Float, by: Float, bw: Float, bh: Float
): Boolean = abs(ax - bx) * 2f < (aw + bw) && abs(ay - by) * 2f < (ah + bh)

// ============================================================================
//  ENUMS & MODELS
// ============================================================================

private enum class Screen { SPLASH, MENU, SETTINGS, ABOUT, PLAYING, PAUSED, GAMEOVER }

private enum class ObstacleType { ROCK, MINE, ICEBERG, WHALE, PIRATE }

private enum class ItemType { BARREL, HEART, SHIELD, MAGNET, TURBO }

private class Obstacle(
    var type: ObstacleType,
    var x: Float,
    var y: Float,
    var w: Float,
    var h: Float,
    var phase: Float
) {
    var baseY: Float = y
    var flash: Float = 0f
    var fireTimer: Float = 2f
}

private class Item(
    var type: ItemType,
    var x: Float,
    var y: Float,
    var baseY: Float,
    var phase: Float,
    var r: Float
)

private class Bullet(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var r: Float,
    var enemy: Boolean
)

private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var maxLife: Float,
    var size: Float,
    var color: Color,
    var gravity: Float = 0f
)

private class Boss(var x: Float, var y: Float, var w: Float, var h: Float) {
    var baseY: Float = y
    var targetX: Float = x
    var hp: Int = 5
    var flash: Float = 0f
    var fireTimer: Float = 1.8f
    var entering: Boolean = true
    var phase: Float = 0f
}

// ============================================================================
//  PREFERENCES
// ============================================================================

private class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("oil_tanker_prefs", Context.MODE_PRIVATE)

    var best by mutableStateOf(sp.getInt("best", 0))
    var sound by mutableStateOf(sp.getBoolean("sound", true))
    var vibration by mutableStateOf(sp.getBoolean("vibration", true))

    fun saveBest(value: Int) {
        if (value > best) {
            best = value
            sp.edit().putInt("best", value).apply()
        }
    }

    fun saveSound(value: Boolean) {
        sound = value
        sp.edit().putBoolean("sound", value).apply()
    }

    fun saveVibration(value: Boolean) {
        vibration = value
        sp.edit().putBoolean("vibration", value).apply()
    }
}

// ============================================================================
//  SOUND
// ============================================================================

private class Sfx {
    private var tone: ToneGenerator? = null
    var enabled: Boolean = true

    init {
        tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 85) }.getOrNull()
    }

    private fun play(type: Int, ms: Int) {
        if (!enabled) return
        runCatching { tone?.startTone(type, ms) }
    }

    fun coin() = play(ToneGenerator.TONE_PROP_BEEP, 70)
    fun hit() = play(ToneGenerator.TONE_PROP_NACK, 130)
    fun shoot() = play(ToneGenerator.TONE_PROP_ACK, 45)
    fun powerUp() = play(ToneGenerator.TONE_PROP_BEEP2, 130)
    fun gameOver() = play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 450)

    fun release() {
        runCatching { tone?.release() }
        tone = null
    }
}

// ============================================================================
//  VIBRATION
// ============================================================================

private class Vibro(context: Context) {
    var enabled: Boolean = true

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun buzz() {
        if (!enabled) return
        val v = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(100)
            }
        }
    }
}

// ============================================================================
//  GAME WORLD
// ============================================================================

private class GameWorld(
    private val sfx: Sfx,
    private val vibro: Vibro
) {

    var viewW = 0f
    var viewH = 0f

    var frame by mutableStateOf(0)
    var score by mutableStateOf(0)
    var lives by mutableStateOf(MAX_LIVES)
    var distance by mutableStateOf(0f)
    var barrels by mutableStateOf(0)
    var shieldTime by mutableStateOf(0f)
    var magnetTime by mutableStateOf(0f)
    var turboTime by mutableStateOf(0f)
    var bossActive by mutableStateOf(false)
    var bossHp by mutableStateOf(0)

    var onGameOver: (() -> Unit)? = null

    private var scoreF = 0f
    private var time = 0f
    private var worldScroll = 0f
    private var over = false

    private var shipYFrac = 0.5f
    private var targetYFrac = 0.5f
    private var invuln = 0f
    private var redFlash = 0f
    private var whiteFlash = 0f
    private var fireCooldown = 0f
    private var smokeTimer = 0f

    private var spawnTimer = 1.6f
    private var itemTimer = 3f
    private var nextBossDistance = 1000f
    private var bossDefeatTimer = 0f
    private var nextStormDistance = 3000f
    private var stormTime = 0f
    private var stormLevel = 0f
    private var lightningTimer = 1f

    private var dayNight = 0f
    private var dayNightTarget = 0f
    private var dayNightFrom = 0f
    private var dayNightLerp = 1f
    private var dayNightIndex = 0

    private val obstacles = mutableListOf<Obstacle>()
    private val items = mutableListOf<Item>()
    private val bullets = mutableListOf<Bullet>()
    private val particles = mutableListOf<Particle>()
    private var boss: Boss? = null

    private val surfaceY: Float get() = viewH * 0.10f
    private val shipW: Float get() = viewW * 0.16f
    private val shipH: Float get() = shipW * 0.55f
    private val shipCX: Float get() = viewW * 0.20f
    private val shipCY: Float get() = shipYFrac * viewH

    fun resize(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        if (viewW == w && viewH == h) return
        val first = viewW == 0f
        viewW = w
        viewH = h
        if (first) {
            shipYFrac = 0.5f
            targetYFrac = 0.5f
        }
    }

    fun reset() {
        score = 0
        scoreF = 0f
        lives = MAX_LIVES
        distance = 0f
        barrels = 0
        shieldTime = 0f
        magnetTime = 0f
        turboTime = 0f
        bossActive = false
        bossHp = 0
        over = false
        time = 0f
        worldScroll = 0f
        shipYFrac = 0.5f
        targetYFrac = 0.5f
        invuln = 0f
        redFlash = 0f
        whiteFlash = 0f
        fireCooldown = 0f
        smokeTimer = 0f
        spawnTimer = 1.6f
        itemTimer = 2.5f
        nextBossDistance = 1000f
        bossDefeatTimer = 0f
        nextStormDistance = 3000f
        stormTime = 0f
        stormLevel = 0f
        lightningTimer = 1f
        dayNight = 0f
        dayNightTarget = 0f
        dayNightFrom = 0f
        dayNightLerp = 1f
        dayNightIndex = 0
        obstacles.clear()
        items.clear()
        bullets.clear()
        particles.clear()
        boss = null
    }

    fun onDrag(dy: Float) {
        if (viewH <= 0f) return
        targetYFrac = (targetYFrac + dy / viewH).coerceIn(0.15f, 0.85f)
    }

    fun fire() {
        if (boss == null || over) return
        if (fireCooldown > 0f) return
        fireCooldown = 0.22f
        bullets.add(
            Bullet(
                shipCX + shipW * 0.52f,
                shipCY,
                viewW * 0.95f,
                0f,
                viewH * 0.013f,
                false
            )
        )
        sfx.shoot()
    }

    private fun speedMultiplier(): Float {
        val step = floor(distance / 100f)
        return (1f + 0.05f * step).coerceAtMost(3.4f)
    }

    private fun metersPerSec(): Float =
        BASE_SPEED_MPS * speedMultiplier() * (if (turboTime > 0f) 2f else 1f)

    private fun scrollSpeedPx(): Float = metersPerSec() * (viewH / 100f)

    private fun spawnInterval(): Float {
        val base = (1.75f * 0.9f.pow(distance / 500f)).coerceAtLeast(0.5f)
        return base * (0.75f + Random.nextFloat() * 0.5f)
    }

    fun update(dtRaw: Float) {
        if (viewW <= 0f || viewH <= 0f) return
        val dt = dtRaw.coerceIn(0f, 0.05f)
        time += dt

        val scroll = scrollSpeedPx()
        worldScroll += scroll * dt
        if (worldScroll > 10_000_000f) worldScroll -= 10_000_000f

        if (shieldTime > 0f) shieldTime = max(0f, shieldTime - dt)
        if (magnetTime > 0f) magnetTime = max(0f, magnetTime - dt)
        if (turboTime > 0f) turboTime = max(0f, turboTime - dt)
        if (invuln > 0f) invuln = max(0f, invuln - dt)
        if (fireCooldown > 0f) fireCooldown = max(0f, fireCooldown - dt)
        if (redFlash > 0f) redFlash = max(0f, redFlash - dt * 1.5f)
        if (whiteFlash > 0f) whiteFlash = max(0f, whiteFlash - dt * 2.5f)

        if (over) {
            updateParticles(dt)
            return
        }

        val mps = metersPerSec()
        distance += mps * dt
        scoreF += mps * dt * 0.5f
        score = scoreF.toInt()

        shipYFrac += (targetYFrac - shipYFrac) * min(1f, dt * 9f)

        updateDayNight(dt)
        updateStorm(dt)

        smokeTimer -= dt
        if (smokeTimer <= 0f) {
            smokeTimer = 0.10f
            spawnSmoke()
        }

        if (boss == null && bossDefeatTimer <= 0f && distance >= nextBossDistance) {
            spawnBoss()
        }
        if (bossDefeatTimer > 0f) bossDefeatTimer -= dt

        if (boss == null && bossDefeatTimer <= 0f) {
            spawnTimer -= dt
            if (spawnTimer <= 0f) {
                spawnObstacle()
                spawnTimer = spawnInterval()
            }
        }

        itemTimer -= dt
        if (itemTimer <= 0f) {
            spawnItem()
            itemTimer = 2.2f + Random.nextFloat() * 3.0f
        }

        updateObstacles(dt, scroll)
        updateItems(dt, scroll)
        updateBullets(dt)
        updateBoss(dt)
        updateParticles(dt)
        collide()
    }

    private fun spawnObstacle() {
        val r = Random.nextFloat()
        val type = when {
            r < 0.28f -> ObstacleType.ROCK
            r < 0.50f -> ObstacleType.MINE
            r < 0.68f -> ObstacleType.ICEBERG
            r < 0.86f -> ObstacleType.WHALE
            else -> ObstacleType.PIRATE
        }
        val y = viewH * (0.22f + Random.nextFloat() * 0.60f)
        val w: Float
        val h: Float
        when (type) {
            ObstacleType.ROCK -> { w = viewH * 0.20f; h = viewH * 0.20f }
            ObstacleType.MINE -> { w = viewH * 0.11f; h = viewH * 0.11f }
            ObstacleType.ICEBERG -> { w = viewH * 0.24f; h = viewH * 0.26f }
            ObstacleType.WHALE -> { w = viewH * 0.34f; h = viewH * 0.18f }
            ObstacleType.PIRATE -> { w = viewH * 0.32f; h = viewH * 0.26f }
        }
        obstacles.add(
            Obstacle(type, viewW + w, y, w, h, Random.nextFloat() * 10f).also {
                it.baseY = y
                it.fireTimer = 1.4f + Random.nextFloat()
            }
        )
    }

    private fun spawnItem() {
        val r = Random.nextFloat()
        val type = when {
            r < 0.50f -> ItemType.BARREL
            r < 0.62f -> ItemType.HEART
            r < 0.76f -> ItemType.SHIELD
            r < 0.89f -> ItemType.MAGNET
            else -> ItemType.TURBO
        }
        val y = viewH * (0.22f + Random.nextFloat() * 0.58f)
        items.add(
            Item(
                type,
                viewW + viewH * 0.08f,
                y,
                y,
                Random.nextFloat() * 6f,
                viewH * 0.038f
            )
        )
    }

    private fun spawnSmoke() {
        val fx = shipCX - shipW * 0.44f
        val fy = shipCY - shipH * 0.62f
        particles.add(
            Particle(
                fx, fy,
                -viewW * 0.010f - Random.nextFloat() * viewW * 0.012f,
                -viewH * 0.055f - Random.nextFloat() * viewH * 0.03f,
                1.4f, 1.4f,
                viewW * 0.010f + Random.nextFloat() * viewW * 0.006f,
                Color(0x889E9E9E)
            )
        )
        if (particles.size > 420) {
            while (particles.size > 420) particles.removeAt(0)
        }
    }

    private fun burst(x: Float, y: Float, radius: Float) {
        val n = 22
        for (i in 0 until n) {
            val a = Random.nextFloat() * PI.toFloat() * 2f
            val sp = radius * (0.8f + Random.nextFloat() * 2.4f)
            particles.add(
                Particle(
                    x, y,
                    cos(a) * sp, sin(a) * sp,
                    0.55f + Random.nextFloat() * 0.5f, 1.05f,
                    radius * (0.10f + Random.nextFloat() * 0.16f),
                    if (i % 2 == 0) Color(0xFFFFB300) else Color(0xFFFF5722)
                )
            )
        }
        if (particles.size > 420) {
            while (particles.size > 420) particles.removeAt(0)
        }
    }

    private fun spawnBoss() {
        val w = viewH * 0.62f
        val h = viewH * 0.46f
        val b = Boss(viewW + w * 0.85f, viewH * 0.5f, w, h)
        b.baseY = b.y
        b.targetX = viewW * 0.78f
        b.hp = 5
        boss = b
        bossActive = true
        bossHp = 5
    }

    private fun updateDayNight(dt: Float) {
        val idx = floor(distance / 2000f).toInt()
        if (idx != dayNightIndex) {
            dayNightIndex = idx
            dayNightFrom = dayNight
            dayNightTarget = if (idx % 2 == 0) 0f else 1f
            dayNightLerp = 0f
        }
        if (dayNightLerp < 1f) {
            dayNightLerp = min(1f, dayNightLerp + dt / 3f)
            val t = dayNightLerp * dayNightLerp * (3f - 2f * dayNightLerp)
            dayNight = dayNightFrom + (dayNightTarget - dayNightFrom) * t
        }
    }

    private fun updateStorm(dt: Float) {
        if (distance >= nextStormDistance) {
            nextStormDistance += 3000f
            stormTime = 10f
            lightningTimer = 0.8f
        }
        if (stormTime > 0f) {
            stormTime -= dt
            stormLevel = min(1f, stormLevel + dt / 1.5f)
            lightningTimer -= dt
            if (lightningTimer <= 0f) {
                lightningTimer = 0.7f + Random.nextFloat() * 2.6f
                whiteFlash = 0.45f
            }
        } else {
            stormLevel = max(0f, stormLevel - dt / 2.5f)
        }
    }

    private fun updateObstacles(dt: Float, scroll: Float) {
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val o = iterator.next()
            o.phase += dt

            val extra = when (o.type) {
                ObstacleType.WHALE -> scroll * 0.35f
                ObstacleType.PIRATE -> -scroll * 0.25f
                else -> 0f
            }
            o.x -= (scroll + extra) * dt

            when (o.type) {
                ObstacleType.WHALE -> o.y = o.baseY + sin(o.phase * 1.6f) * viewH * 0.05f
                ObstacleType.PIRATE -> {
                    o.fireTimer -= dt
                    if (o.fireTimer <= 0f && o.x < viewW * 0.98f && o.x > viewW * 0.28f) {
                        o.fireTimer = 2.4f
                        bullets.add(
                            Bullet(
                                o.x - o.w * 0.5f, o.y,
                                -viewW * 0.42f, 0f,
                                viewH * 0.014f, true
                            )
                        )
                        sfx.shoot()
                    }
                }
                else -> {}
            }

            if (o.flash > 0f) o.flash -= dt * 3f
            if (o.x < -viewW * 0.30f) iterator.remove()
        }
    }

    private fun updateItems(dt: Float, scroll: Float) {
        val iterator = items.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            item.phase += dt
            item.x -= scroll * dt
            item.y = item.baseY + sin(item.phase * 2f) * viewH * 0.015f

            if (magnetTime > 0f && item.type == ItemType.BARREL) {
                val dx = shipCX - item.x
                val dy = shipCY - item.y
                val d = sqrt(dx * dx + dy * dy)
                if (d < viewH * 0.65f && d > 1f) {
                    item.x += dx / d * viewH * 1.0f * dt
                    item.y += dy / d * viewH * 1.0f * dt
                    item.baseY = item.y
                }
            }

            if (item.x < -viewW * 0.15f) iterator.remove()
        }
    }

    private fun updateBullets(dt: Float) {
        val iterator = bullets.iterator()
        while (iterator.hasNext()) {
            val b = iterator.next()
            b.x += b.vx * dt
            b.y += b.vy * dt
            if (b.x < -viewW * 0.2f || b.x > viewW * 1.2f ||
                b.y < -viewH * 0.2f || b.y > viewH * 1.2f
            ) {
                iterator.remove()
            }
        }
    }

    private fun updateBoss(dt: Float) {
        val b = boss ?: return
        b.phase += dt
        if (b.flash > 0f) b.flash = max(0f, b.flash - dt * 2.5f)

        if (b.entering) {
            b.x -= viewW * 0.30f * dt
            if (b.x <= b.targetX) {
                b.x = b.targetX
                b.entering = false
            }
        } else {
            b.x = b.targetX + sin(b.phase * 0.8f) * viewH * 0.015f
            b.y = b.baseY + sin(b.phase * 1.3f) * viewH * 0.09f

            b.fireTimer -= dt
            if (b.fireTimer <= 0f) {
                b.fireTimer = 2.0f
                fireBossBullet(b)
            }
        }
    }

    private fun fireBossBullet(b: Boss) {
        val originX = b.x - b.w * 0.5f
        val originY = b.y
        val dx = shipCX - originX
        val dy = shipCY - originY
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val speed = viewW * 0.50f
        bullets.add(
            Bullet(
                originX, originY,
                dx / len * speed, dy / len * speed,
                viewH * 0.018f, true
            )
        )
        sfx.shoot()
    }

    private fun updateParticles(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += p.gravity * dt
            p.life -= dt
            if (p.life <= 0f) iterator.remove()
        }
    }

    private fun collide() {
        val sx = shipCX
        val sy = shipCY
        val sw = shipW * 0.62f
        val sh = shipH * 0.72f

        val itemIterator = items.iterator()
        while (itemIterator.hasNext()) {
            val item = itemIterator.next()
            if (overlap(sx, sy, sw, sh, item.x, item.y, item.r * 1.8f, item.r * 1.8f)) {
                when (item.type) {
                    ItemType.BARREL -> {
                        barrels += 1
                        scoreF += 10f
                        score = scoreF.toInt()
                        sfx.coin()
                    }
                    ItemType.HEART -> {
                        lives = min(MAX_LIVES, lives + 1)
                        sfx.powerUp()
                    }
                    ItemType.SHIELD -> {
                        shieldTime = 5f
                        sfx.powerUp()
                    }
                    ItemType.MAGNET -> {
                        magnetTime = 10f
                        sfx.powerUp()
                    }
                    ItemType.TURBO -> {
                        turboTime = 3f
                        sfx.powerUp()
                    }
                }
                itemIterator.remove()
            }
        }

        val obstacleIterator = obstacles.iterator()
        while (obstacleIterator.hasNext()) {
            val o = obstacleIterator.next()
            if (overlap(sx, sy, sw, sh, o.x, o.y, o.w * 0.78f, o.h * 0.78f)) {
                if (shieldTime > 0f) {
                    burst(o.x, o.y, o.w * 0.4f)
                    scoreF += 15f
                    score = scoreF.toInt()
                    obstacleIterator.remove()
                    sfx.hit()
                } else if (invuln <= 0f) {
                    burst(o.x, o.y, o.w * 0.4f)
                    obstacleIterator.remove()
                    damage()
                }
            }
        }

        val b = boss
        if (b != null && !b.entering) {
            if (overlap(sx, sy, sw, sh, b.x, b.y, b.w * 0.70f, b.h * 0.70f)) {
                damage()
            }
        }

        val bulletIterator = bullets.iterator()
        while (bulletIterator.hasNext()) {
            val bullet = bulletIterator.next()
            if (bullet.enemy) {
                if (overlap(sx, sy, sw, sh, bullet.x, bullet.y, bullet.r * 2f, bullet.r * 2f)) {
                    bulletIterator.remove()
                    burst(bullet.x, bullet.y, viewH * 0.03f)
                    damage()
                }
            } else {
                val bb = boss
                if (bb != null && !bb.entering &&
                    overlap(
                        bullet.x, bullet.y, bullet.r * 2f, bullet.r * 2f,
                        bb.x, bb.y, bb.w * 0.80f, bb.h * 0.80f
                    )
                ) {
                    bulletIterator.remove()
                    bb.hp -= 1
                    bb.flash = 1f
                    bossHp = max(0, bb.hp)
                    burst(bullet.x, bullet.y, viewH * 0.05f)
                    scoreF += 25f
                    score = scoreF.toInt()
                    sfx.hit()
                    if (bb.hp <= 0) defeatBoss()
                }
            }
        }
    }

    private fun damage() {
        if (over) return
        if (invuln > 0f) return
        if (shieldTime > 0f) return

        lives -= 1
        invuln = 1.4f
        redFlash = 1f
        vibro.buzz()
        sfx.hit()

        if (lives <= 0) {
            lives = 0
            over = true
            sfx.gameOver()
            onGameOver?.invoke()
        }
    }

    private fun defeatBoss() {
        val b = boss ?: return
        burst(b.x, b.y, viewH * 0.30f)
        burst(b.x - b.w * 0.2f, b.y + b.h * 0.1f, viewH * 0.22f)
        boss = null
        bossActive = false
        bossHp = 0
        scoreF += 500f
        score = scoreF.toInt()
        lives = min(MAX_LIVES, lives + 1)
        bossDefeatTimer = 2.6f
        nextBossDistance += 1000f
        sfx.powerUp()
    }

    private fun tint(c: Color): Color {
        var r = lerp(c, Color(0xFF0A1420), dayNight * 0.50f)
        r = lerp(r, Color(0xFF1C2A33), stormLevel * 0.35f)
        return r
    }

    private fun waveY(x: Float): Float {
        val amp = viewH * (0.012f + stormLevel * 0.030f)
        return surfaceY +
                sin((x + worldScroll) * 0.0060f) * amp +
                sin((x + worldScroll * 1.7f) * 0.0130f + 1.3f) * amp * 0.55f
    }

    fun DrawScope.drawAll() {
        if (viewW <= 0f || viewH <= 0f) return
        drawSky()
        drawWater()
        drawSeabed()
        drawItems()
        drawObstacles()
        drawBossShip()
        drawBullets()
        drawPlayerShip()
        drawParticles()
        drawWeather()
        drawScreenEffects()
    }

    private fun DrawScope.drawSky() {
        val dn = dayNight
        var top = lerp(Color(0xFF64B5F6), Color(0xFF1A237E), dn)
        var bottom = lerp(Color(0xFFB3E5FC), Color(0xFF101A45), dn)
        top = lerp(top, Color(0xFF37474F), stormLevel * 0.75f)
        bottom = lerp(bottom, Color(0xFF263238), stormLevel * 0.75f)

        drawRect(
            brush = Brush.verticalGradient(listOf(top, bottom), 0f, surfaceY),
            topLeft = Offset.Zero,
            size = Size(viewW, surfaceY)
        )

        if (dn > 0.15f) {
            val alpha = ((dn - 0.15f) / 0.85f).coerceIn(0f, 1f) * (1f - stormLevel * 0.8f)
            for (star in STAR_FIELD) {
                val sx = star.first * viewW
                val sy = star.second * surfaceY * 0.92f
                val twinkle = 0.6f + 0.4f * sin(time * 2.5f + star.first * 30f)
                drawCircle(
                    Color.White.copy(alpha = alpha * twinkle),
                    viewH * 0.003f * star.third,
                    Offset(sx, sy)
                )
            }
        }

        val bodyX = viewW * 0.82f
        val bodyY = surfaceY * 0.42f
        val bodyR = viewH * 0.045f
        if (dn < 0.5f) {
            val sunColor = lerp(Color(0xFFFFEE58), Color(0xFFFFA726), dn * 2f)
            drawCircle(Color(0x33FFEB3B), bodyR * 1.9f, Offset(bodyX, bodyY))
            drawCircle(sunColor, bodyR, Offset(bodyX, bodyY))
        } else {
            val night = (dn - 0.5f) * 2f
            drawCircle(Color(0x33E1F5FE), bodyR * 1.8f, Offset(bodyX, bodyY))
            drawCircle(lerp(Color(0xFFFFF9C4), Color(0xFFE1F5FE), night), bodyR, Offset(bodyX, bodyY))
            drawCircle(
                Color(0xFF101A45).copy(alpha = night),
                bodyR * 0.85f,
                Offset(bodyX + bodyR * 0.42f, bodyY - bodyR * 0.30f)
            )
        }

        drawClouds()
    }

    private fun DrawScope.drawClouds() {
        val cloudAlpha = (0.55f * (1f - dayNight * 0.75f) + stormLevel * 0.25f).coerceIn(0f, 1f)
        val cloudColor = lerp(Color.White, Color(0xFF90A4AE), dayNight * 0.7f)
        val period = viewW * 1.6f
        val base = worldScroll * 0.12f
        val startIdx = floor(base / period).toInt() - 1
        for (k in startIdx..(startIdx + 3)) {
            val h1 = hash01(k * 13 + 3)
            val h2 = hash01(k * 29 + 7)
            val cx = k * period - base + h1 * period * 0.6f
            if (cx < -viewW * 0.4f || cx > viewW * 1.4f) continue
            val cy = surfaceY * (0.18f + h2 * 0.42f)
            val s = viewH * (0.030f + h1 * 0.030f)
            drawOval(
                cloudColor.copy(alpha = cloudAlpha * 0.8f),
                Offset(cx - s * 1.6f, cy - s * 0.45f),
                Size(s * 3.2f, s * 0.9f)
            )
            drawOval(
                cloudColor.copy(alpha = cloudAlpha * 0.9f),
                Offset(cx - s * 0.9f, cy - s * 0.95f),
                Size(s * 2.0f, s * 1.2f)
            )
            drawOval(
                cloudColor.copy(alpha = cloudAlpha * 0.85f),
                Offset(cx + s * 0.2f, cy - s * 0.70f),
                Size(s * 1.6f, s * 1.0f)
            )
        }
    }

    private fun DrawScope.drawWater() {
        val dn = dayNight
        var waterTop = lerp(Color(0xFF1976D2), Color(0xFF0A1E4A), dn)
        var waterBottom = lerp(Color(0xFF0D47A1), Color(0xFF050E22), dn)
        waterTop = lerp(waterTop, Color(0xFF2F4A55), stormLevel * 0.6f)
        waterBottom = lerp(waterBottom, Color(0xFF16232B), stormLevel * 0.6f)

        val bodyPath = Path()
        bodyPath.moveTo(0f, waveY(0f))
        var x = 0f
        while (x < viewW) {
            x += 14f
            val cx = if (x > viewW) viewW else x
            bodyPath.lineTo(cx, waveY(cx))
        }
        bodyPath.lineTo(viewW, viewH)
        bodyPath.lineTo(0f, viewH)
        bodyPath.close()

        drawPath(
            path = bodyPath,
            brush = Brush.verticalGradient(
                listOf(waterTop, waterBottom),
                startY = surfaceY,
                endY = viewH
            )
        )

        val surfacePath = Path()
        surfacePath.moveTo(0f, waveY(0f))
        x = 0f
        while (x < viewW) {
            x += 14f
            val cx = if (x > viewW) viewW else x
            surfacePath.lineTo(cx, waveY(cx))
        }
        drawPath(
            surfacePath,
            Color(0x77BBDEFB).copy(alpha = 0.45f * (1f - stormLevel * 0.4f)),
            style = Stroke(width = viewH * 0.008f)
        )

        for (k in 1..3) {
            val yy = surfaceY + (viewH - surfaceY) * (k * 0.17f)
            val wp = Path()
            var first = true
            var xx = 0f
            while (xx <= viewW) {
                val py = yy + sin((xx + worldScroll * 1.4f) * 0.008f + k * 1.7f) * viewH * 0.012f
                if (first) {
                    wp.moveTo(xx, py)
                    first = false
                } else {
                    wp.lineTo(xx, py)
                }
                xx += 22f
            }
            drawPath(
                wp,
                Color(0x22FFFFFF).copy(alpha = 0.14f * (1f - stormLevel * 0.5f)),
                style = Stroke(width = viewH * 0.005f)
            )
        }
    }

    private fun DrawScope.drawSeabed() {
        val sandTop = viewH * 0.94f
        val sand = tint(Color(0xFFC2A878))
        drawRect(sand, Offset(0f, sandTop), Size(viewW, viewH - sandTop))
        drawRect(
            tint(Color(0xFF8D7B57)),
            Offset(0f, sandTop),
            Size(viewW, viewH * 0.008f)
        )

        val period = viewW * 0.22f
        val startIdx = floor(worldScroll / period).toInt() - 1
        val endIdx = ((worldScroll + viewW) / period).toInt() + 1
        for (n in startIdx..endIdx) {
            val px = n * period - worldScroll + period * 0.4f
            if (px < -period || px > viewW + period) continue
            val h = viewH * (0.015f + hash01(n) * 0.030f)
            val w = viewH * (0.030f + hash01(n * 7 + 3) * 0.040f)
            val rockColor = tint(lerp(Color(0xFF6D6D6D), Color(0xFF4E4E4E), hash01(n * 11 + 5)))
            val p = Path()
            p.moveTo(px - w * 0.5f, sandTop + h * 0.30f)
            p.lineTo(px - w * 0.22f, sandTop - h * 0.60f)
            p.lineTo(px + w * 0.10f, sandTop - h * 0.95f)
            p.lineTo(px + w * 0.40f, sandTop - h * 0.35f)
            p.lineTo(px + w * 0.5f, sandTop + h * 0.30f)
            p.close()
            drawPath(p, rockColor)
        }
    }

    private fun DrawScope.drawPlayerShip() {
        val bob = sin(time * 2.2f) * viewH * 0.006f
        val cx = shipCX
        val cy = shipCY + bob
        val w = shipW
        val h = shipH

        drawOval(
            Color(0x55FFFFFF),
            Offset(cx - w * 0.60f, cy + h * 0.18f),
            Size(w * 1.20f, h * 0.34f)
        )

        val hull = Path()
        hull.moveTo(cx - w * 0.50f, cy - h * 0.16f)
        hull.lineTo(cx + w * 0.34f, cy - h * 0.16f)
        hull.lineTo(cx + w * 0.52f, cy + h * 0.14f)
        hull.lineTo(cx + w * 0.44f, cy + h * 0.42f)
        hull.lineTo(cx - w * 0.44f, cy + h * 0.42f)
        hull.lineTo(cx - w * 0.54f, cy + h * 0.10f)
        hull.close()
        drawPath(hull, tint(Color(0xFFB71C1C)))
        drawPath(hull, tint(Color(0xFF6D0F0F)), style = Stroke(width = h * 0.055f))

        drawRect(
            tint(Color(0xFF212121)),
            Offset(cx - w * 0.50f, cy + h * 0.20f),
            Size(w * 0.94f, h * 0.07f)
        )

        drawRect(
            tint(Color(0xFF7F1010)),
            Offset(cx - w * 0.50f, cy - h * 0.21f),
            Size(w * 0.86f, h * 0.06f)
        )

        for (i in 0..2) {
            val tx = cx - w * 0.36f + i * w * 0.235f
            val ty = cy - h * 0.50f
            val tw = w * 0.17f
            val th = h * 0.30f
            drawRoundRect(
                tint(Color(0xFF9E9E9E)),
                Offset(tx, ty),
                Size(tw, th),
                CornerRadius(th * 0.30f)
            )
            drawRect(
                tint(Color(0xFF616161)),
                Offset(tx, ty + th * 0.36f),
                Size(tw, th * 0.14f)
            )
        }

        drawRoundRect(
            tint(Color(0xFFECEFF1)),
            Offset(cx + w * 0.12f, cy - h * 0.44f),
            Size(w * 0.17f, h * 0.26f),
            CornerRadius(h * 0.04f)
        )
        drawRect(
            tint(Color(0xFF42A5F5)),
            Offset(cx + w * 0.15f, cy - h * 0.38f),
            Size(w * 0.10f, h * 0.08f)
        )

        drawRect(
            tint(Color(0xFF424242)),
            Offset(cx - w * 0.47f, cy - h * 0.64f),
            Size(w * 0.085f, h * 0.48f)
        )
        drawRect(
            tint(Color(0xFF212121)),
            Offset(cx - w * 0.485f, cy - h * 0.64f),
            Size(w * 0.115f, h * 0.075f)
        )

        if (shieldTime > 0f) {
            val pulse = 0.5f + 0.5f * sin(time * 8f)
            drawCircle(
                Color(0x5529B6F6).copy(alpha = 0.20f + pulse * 0.20f),
                w * 0.66f,
                Offset(cx, cy)
            )
            drawCircle(
                Color(0xAA29B6F6).copy(alpha = 0.55f + pulse * 0.35f),
                w * 0.66f,
                Offset(cx, cy),
                style = Stroke(width = w * 0.022f)
            )
        }
    }

    private fun DrawScope.drawObstacles() {
        for (o in obstacles) {
            when (o.type) {
                ObstacleType.ROCK -> drawRock(o)
                ObstacleType.MINE -> drawMine(o)
                ObstacleType.ICEBERG -> drawIceberg(o)
                ObstacleType.WHALE -> drawWhale(o)
                ObstacleType.PIRATE -> drawPirate(o)
            }
        }
    }

    private fun DrawScope.drawRock(o: Obstacle) {
        val r = o.w * 0.5f
        val path = Path()
        val n = 9
        for (i in 0 until n) {
            val a = (i.toFloat() / n) * PI.toFloat() * 2f
            val rr = r * (0.70f + 0.30f * abs(sin(i * 2.3f + o.phase * 0.15f)))
            val px = o.x + cos(a) * rr
            val py = o.y + sin(a) * rr * 0.92f
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        drawPath(path, tint(Color(0xFF757575)))
        drawPath(path, tint(Color(0xFF424242)), style = Stroke(width = r * 0.10f))
        drawCircle(
            tint(Color(0xFF9E9E9E)),
            r * 0.28f,
            Offset(o.x - r * 0.30f, o.y - r * 0.32f)
        )
        drawCircle(
            Color(0x33FFFFFF),
            r * 0.14f,
            Offset(o.x - r * 0.38f, o.y - r * 0.40f)
        )
    }

    private fun DrawScope.drawMine(o: Obstacle) {
        val r = o.w * 0.5f
        val spike = r * 0.55f
        for (i in 0 until 8) {
            val a = (i.toFloat() / 8f) * PI.toFloat() * 2f + o.phase * 0.4f
            val sx = o.x + cos(a) * r * 0.85f
            val sy = o.y + sin(a) * r * 0.85f
            val ex = o.x + cos(a) * (r * 0.85f + spike)
            val ey = o.y + sin(a) * (r * 0.85f + spike)
            drawLine(
                tint(Color(0xFF212121)),
                Offset(sx, sy),
                Offset(ex, ey),
                strokeWidth = r * 0.20f
            )
        }
        drawCircle(tint(Color(0xFF212121)), r * 0.85f, Offset(o.x, o.y))
        drawCircle(tint(Color(0xFF616161)), r * 0.45f, Offset(o.x, o.y))
        val blink = 0.5f + 0.5f * sin(time * 7f + o.phase)
        drawCircle(
            Color(0xFFFF1744).copy(alpha = 0.35f + blink * 0.65f),
            r * 0.20f,
            Offset(o.x, o.y)
        )
    }

    private fun DrawScope.drawIceberg(o: Obstacle) {
        val p = Path()
        p.moveTo(o.x, o.y - o.h * 0.50f)
        p.lineTo(o.x + o.w * 0.50f, o.y + o.h * 0.48f)
        p.lineTo(o.x - o.w * 0.50f, o.y + o.h * 0.48f)
        p.close()
        drawPath(p, tint(Color(0xFFE3F2FD)))
        drawPath(p, tint(Color(0xFF64B5F6)), style = Stroke(width = o.w * 0.035f))
        drawLine(
            tint(Color(0xFF90CAF9)),
            Offset(o.x, o.y - o.h * 0.50f),
            Offset(o.x - o.w * 0.16f, o.y + o.h * 0.48f),
            strokeWidth = o.w * 0.022f
        )
        drawLine(
            tint(Color(0xFF90CAF9)),
            Offset(o.x, o.y - o.h * 0.50f),
            Offset(o.x + o.w * 0.22f, o.y + o.h * 0.48f),
            strokeWidth = o.w * 0.018f
        )
    }

    private fun DrawScope.drawWhale(o: Obstacle) {
        val w = o.w
        val h = o.h
        val tail = Path()
        tail.moveTo(o.x - w * 0.42f, o.y)
        tail.lineTo(o.x - w * 0.62f, o.y - h * 0.55f)
        tail.lineTo(o.x - w * 0.62f, o.y + h * 0.55f)
        tail.close()
        drawPath(tail, tint(Color(0xFF0D3B66)))

        drawOval(
            tint(Color(0xFF0D3B66)),
            Offset(o.x - w * 0.50f, o.y - h * 0.50f),
            Size(w, h)
        )
        drawOval(
            tint(Color(0xFF4A7FA8)),
            Offset(o.x - w * 0.32f, o.y + h * 0.02f),
            Size(w * 0.62f, h * 0.36f)
        )
        drawCircle(Color.White, h * 0.09f, Offset(o.x + w * 0.28f, o.y - h * 0.14f))
        drawCircle(Color(0xFF0A0A0A), h * 0.05f, Offset(o.x + w * 0.30f, o.y - h * 0.14f))
        val fin = Path()
        fin.moveTo(o.x - w * 0.02f, o.y + h * 0.28f)
        fin.lineTo(o.x + w * 0.14f, o.y + h * 0.28f)
        fin.lineTo(o.x + w * 0.02f, o.y + h * 0.58f)
        fin.close()
        drawPath(fin, tint(Color(0xFF0A2E52)))

        val spoutPhase = (o.phase % 2.4f)
        if (spoutPhase < 1.1f) {
            val a = 1f - (spoutPhase / 1.1f)
            for (i in 0 until 6) {
                val ang = -PI.toFloat() * 0.5f + (i - 2.5f) * 0.22f
                val len = h * (0.35f + a * 0.45f)
                drawLine(
                    Color(0xCCE1F5FE).copy(alpha = a * 0.85f),
                    Offset(o.x + w * 0.16f, o.y - h * 0.45f),
                    Offset(
                        o.x + w * 0.16f + cos(ang) * len * 0.45f,
                        o.y - h * 0.45f + sin(ang) * len
                    ),
                    strokeWidth = h * 0.055f
                )
            }
        }
    }

    private fun DrawScope.drawPirate(o: Obstacle) {
        val w = o.w
        val h = o.h
        val cx = o.x
        val cy = o.y
        val flashColor = if (o.flash > 0f) Color(0xFFFF5252) else null

        val hull = Path()
        hull.moveTo(cx - w * 0.50f, cy - h * 0.10f)
        hull.lineTo(cx + w * 0.34f, cy - h * 0.10f)
        hull.lineTo(cx + w * 0.48f, cy + h * 0.12f)
        hull.lineTo(cx + w * 0.40f, cy + h * 0.36f)
        hull.lineTo(cx - w * 0.44f, cy + h * 0.36f)
        hull.lineTo(cx - w * 0.52f, cy + h * 0.08f)
        hull.close()
        drawPath(hull, flashColor ?: tint(Color(0xFF5D4037)))
        drawPath(hull, tint(Color(0xFF3E2723)), style = Stroke(width = h * 0.045f))

        drawRect(
            flashColor ?: tint(Color(0xFF4E342E)),
            Offset(cx - w * 0.48f, cy - h * 0.16f),
            Size(w * 0.86f, h * 0.07f)
        )

        drawLine(
            tint(Color(0xFF3E2723)),
            Offset(cx - w * 0.10f, cy - h * 0.14f),
            Offset(cx - w * 0.10f, cy - h * 0.86f),
            strokeWidth = h * 0.045f
        )
        val sail = Path()
        sail.moveTo(cx - w * 0.06f, cy - h * 0.80f)
        sail.lineTo(cx + w * 0.22f, cy - h * 0.42f)
        sail.lineTo(cx - w * 0.06f, cy - h * 0.30f)
        sail.close()
        drawPath(sail, flashColor ?: tint(Color(0xFFECEFF1)))

        val flag = Path()
        flag.moveTo(cx - w * 0.10f, cy - h * 0.86f)
        flag.lineTo(cx + w * 0.14f, cy - h * 0.78f)
        flag.lineTo(cx - w * 0.10f, cy - h * 0.68f)
        flag.close()
        drawPath(flag, Color(0xFFD32F2F))

        drawRect(
            flashColor ?: tint(Color(0xFF212121)),
            Offset(cx - w * 0.60f, cy - h * 0.02f),
            Size(w * 0.20f, h * 0.10f)
        )
    }

    private fun DrawScope.drawBossShip() {
        val b = boss ?: return
        val w = b.w
        val h = b.h
        val cx = b.x
        val cy = b.y
        val flashColor = if (b.flash > 0f) Color(0xFFFF5252) else null

        val hull = Path()
        hull.moveTo(cx - w * 0.50f, cy - h * 0.18f)
        hull.lineTo(cx + w * 0.30f, cy - h * 0.18f)
        hull.lineTo(cx + w * 0.46f, cy + h * 0.06f)
        hull.lineTo(cx + w * 0.36f, cy + h * 0.42f)
        hull.lineTo(cx - w * 0.40f, cy + h * 0.42f)
        hull.lineTo(cx - w * 0.52f, cy + h * 0.08f)
        hull.close()
        drawPath(hull, flashColor ?: tint(Color(0xFF546E7A)))
        drawPath(hull, tint(Color(0xFF263238)), style = Stroke(width = h * 0.04f))

        drawRect(
            flashColor ?: tint(Color(0xFF37474F)),
            Offset(cx - w * 0.28f, cy - h * 0.52f),
            Size(w * 0.46f, h * 0.34f)
        )
        for (i in 0..3) {
            drawRect(
                tint(Color(0xFFFFEB3B)),
                Offset(cx - w * 0.24f + i * w * 0.10f, cy - h * 0.46f),
                Size(w * 0.06f, h * 0.07f)
            )
        }

        drawCircle(
            flashColor ?: tint(Color(0xFF455A64)),
            h * 0.14f,
            Offset(cx + w * 0.12f, cy - h * 0.30f)
        )
        drawRect(
            flashColor ?: tint(Color(0xFF455A64)),
            Offset(cx - w * 0.10f, cy - h * 0.36f),
            Size(w * 0.24f, h * 0.07f)
        )

        drawRect(
            flashColor ?: tint(Color(0xFF212121)),
            Offset(cx - w * 0.22f, cy - h * 0.78f),
            Size(w * 0.10f, h * 0.28f)
        )

        drawLine(
            tint(Color(0xFF263238)),
            Offset(cx + w * 0.02f, cy - h * 0.52f),
            Offset(cx + w * 0.02f, cy - h * 0.98f),
            strokeWidth = h * 0.030f
        )
        val flag = Path()
        flag.moveTo(cx + w * 0.02f, cy - h * 0.98f)
        flag.lineTo(cx + w * 0.22f, cy - h * 0.90f)
        flag.lineTo(cx + w * 0.02f, cy - h * 0.80f)
        flag.close()
        drawPath(flag, Color(0xFFD32F2F))

        val barW = w * 0.80f
        val barH = h * 0.055f
        val barX = cx - barW * 0.5f
        val barY = cy - h * 0.86f
        drawRoundRect(
            Color(0xAA000000),
            Offset(barX, barY),
            Size(barW, barH),
            CornerRadius(barH * 0.5f)
        )
        val hpFrac = (b.hp.toFloat() / 5f).coerceIn(0f, 1f)
        if (hpFrac > 0f) {
            drawRoundRect(
                Color(0xFFE53935),
                Offset(barX, barY),
                Size(barW * hpFrac, barH),
                CornerRadius(barH * 0.5f)
            )
        }
        for (i in 1..4) {
            val sx = barX + barW * (i / 5f)
            drawLine(
                Color(0x66000000),
                Offset(sx, barY),
                Offset(sx, barY + barH),
                strokeWidth = barH * 0.14f
            )
        }
    }

    private fun DrawScope.drawBullets() {
        for (b in bullets) {
            if (b.enemy) {
                drawCircle(Color(0xFFFF7043), b.r * 1.4f, Offset(b.x, b.y))
                drawCircle(Color(0xFFD84315), b.r, Offset(b.x, b.y))
                drawCircle(Color(0xFFFFE0B2), b.r * 0.42f, Offset(b.x, b.y))
            } else {
                drawCircle(Color(0x66FFEB3B), b.r * 1.8f, Offset(b.x, b.y))
                drawCircle(Color(0xFFFFC107), b.r, Offset(b.x, b.y))
                drawCircle(Color(0xFFFFFFFF), b.r * 0.40f, Offset(b.x, b.y))
            }
        }
    }

    private fun DrawScope.drawItems() {
        for (item in items) {
            val r = item.r
            when (item.type) {
                ItemType.BARREL -> {
                    drawRoundRect(
                        Color(0xFFFFC107),
                        Offset(item.x - r * 0.68f, item.y - r),
                        Size(r * 1.36f, r * 2f),
                        CornerRadius(r * 0.28f)
                    )
                    drawRect(
                        Color(0xFF212121),
                        Offset(item.x - r * 0.68f, item.y - r * 0.55f),
                        Size(r * 1.36f, r * 0.18f)
                    )
                    drawRect(
                        Color(0xFF212121),
                        Offset(item.x - r * 0.68f, item.y + r * 0.30f),
                        Size(r * 1.36f, r * 0.18f)
                    )
                    drawCircle(
                        Color(0x55FFFFFF),
                        r * 0.34f,
                        Offset(item.x - r * 0.22f, item.y - r * 0.62f)
                    )
                }

                ItemType.HEART -> {
                    val hp = Path()
                    hp.moveTo(item.x, item.y + r * 0.82f)
                    hp.cubicTo(
                        item.x - r * 1.55f, item.y - r * 0.25f,
                        item.x - r * 0.60f, item.y - r * 1.20f,
                        item.x, item.y - r * 0.36f
                    )
                    hp.cubicTo(
                        item.x + r * 0.60f, item.y - r * 1.20f,
                        item.x + r * 1.55f, item.y - r * 0.25f,
                        item.x, item.y + r * 0.82f
                    )
                    hp.close()
                    drawPath(hp, Color(0xFFFF1744))
                    drawPath(hp, Color(0xFF8E0000), style = Stroke(width = r * 0.14f))
                }

                ItemType.SHIELD -> {
                    drawCircle(Color(0xFF29B6F6), r, Offset(item.x, item.y))
                    drawCircle(Color(0xFF0D47A1), r, Offset(item.x, item.y), style = Stroke(width = r * 0.16f))
                    val star = Path()
                    for (i in 0 until 8) {
                        val ang = -PI.toFloat() * 0.5f + i * PI.toFloat() * 0.25f
                        val rad = if (i % 2 == 0) r * 0.62f else r * 0.20f
                        val px = item.x + cos(ang) * rad
                        val py = item.y + sin(ang) * rad
                        if (i == 0) star.moveTo(px, py) else star.lineTo(px, py)
                    }
                    star.close()
                    drawPath(star, Color.White)
                }

                ItemType.MAGNET -> {
                    drawCircle(Color(0xFFAB47BC), r, Offset(item.x, item.y))
                    drawCircle(Color(0xFF4A148C), r, Offset(item.x, item.y), style = Stroke(width = r * 0.16f))
                    drawArc(
                        color = Color.White,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(item.x - r * 0.48f, item.y - r * 0.55f),
                        size = Size(r * 0.96f, r * 0.96f),
                        style = Stroke(width = r * 0.26f)
                    )
                    drawRect(
                        Color.White,
                        Offset(item.x - r * 0.48f - r * 0.13f, item.y - r * 0.05f),
                        Size(r * 0.26f, r * 0.45f)
                    )
                    drawRect(
                        Color.White,
                        Offset(item.x + r * 0.48f - r * 0.13f, item.y - r * 0.05f),
                        Size(r * 0.26f, r * 0.45f)
                    )
                }

                ItemType.TURBO -> {
                    drawCircle(Color(0xFFFF7043), r, Offset(item.x, item.y))
                    drawCircle(Color(0xFFBF360C), r, Offset(item.x, item.y), style = Stroke(width = r * 0.16f))
                    val bolt = Path()
                    bolt.moveTo(item.x + r * 0.16f, item.y - r * 0.78f)
                    bolt.lineTo(item.x - r * 0.52f, item.y + r * 0.10f)
                    bolt.lineTo(item.x - r * 0.04f, item.y + r * 0.10f)
                    bolt.lineTo(item.x - r * 0.18f, item.y + r * 0.80f)
                    bolt.lineTo(item.x + r * 0.54f, item.y - r * 0.14f)
                    bolt.lineTo(item.x + r * 0.06f, item.y - r * 0.14f)
                    bolt.close()
                    drawPath(bolt, Color(0xFFFFF176))
                }
            }
        }
    }

    private fun DrawScope.drawParticles() {
        for (p in particles) {
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            drawCircle(p.color.copy(alpha = a * p.color.alpha), p.size * (0.4f + a * 0.9f), Offset(p.x, p.y))
        }
    }

    private fun DrawScope.drawWeather() {
        if (stormLevel > 0.02f) {
            val count = (70 * stormLevel).toInt()
            for (k in 0 until count) {
                val seedX = hash01(k * 31 + 11) * viewW
                val speed = 0.7f + hash01(k * 17 + 5) * 0.9f
                val fallRange = viewH + viewH * 0.2f
                val y = ((hash01(k * 7 + 2) * fallRange) + time * speed * viewH * 1.5f) % fallRange - viewH * 0.1f
                var x = (seedX - time * viewH * 0.30f) % viewW
                if (x < 0f) x += viewW
                drawLine(
                    Color(0x66B3E5FC),
                    Offset(x, y),
                    Offset(x - viewH * 0.022f, y + viewH * 0.055f),
                    strokeWidth = viewH * 0.0035f
                )
            }
        }
        if (whiteFlash > 0f) {
            drawRect(
                Color.White.copy(alpha = (whiteFlash * 0.55f).coerceIn(0f, 0.55f)),
                Offset.Zero,
                Size(viewW, viewH)
            )
        }
    }

    private fun DrawScope.drawScreenEffects() {
        if (redFlash > 0f) {
            drawRect(
                Color(0xFFFF1744).copy(alpha = (redFlash * 0.35f).coerceIn(0f, 0.35f)),
                Offset.Zero,
                Size(viewW, viewH)
            )
        }
        if (invuln > 0f && !over) {
            val blink = (sin(time * 26f) + 1f) * 0.5f
            drawRect(
                Color(0x33FFFFFF).copy(alpha = blink * 0.12f),
                Offset.Zero,
                Size(viewW, viewH)
            )
        }
    }
}

// ============================================================================
//  COMPOSABLES
// ============================================================================

@Composable
private fun OilTankerApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val sfx = remember { Sfx() }
    val vibro = remember { Vibro(context) }
    val world = remember { GameWorld(sfx, vibro) }

    var screen by remember { mutableStateOf(Screen.SPLASH) }
    var newRecord by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { sfx.release() }
    }

    LaunchedEffect(prefs.sound) { sfx.enabled = prefs.sound }
    LaunchedEffect(prefs.vibration) { vibro.enabled = prefs.vibration }

    LaunchedEffect(Unit) {
        delay(2000)
        if (screen == Screen.SPLASH) screen = Screen.MENU
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
        ) {
            when (screen) {

                Screen.SPLASH -> SplashScreen()

                Screen.MENU -> MainMenuScreen(
                    best = prefs.best,
                    onStart = {
                        world.reset()
                        world.onGameOver = {
                            val isRecord = world.score > prefs.best
                            prefs.saveBest(world.score)
                            newRecord = isRecord
                            screen = Screen.GAMEOVER
                        }
                        screen = Screen.PLAYING
                    },
                    onSettings = { screen = Screen.SETTINGS },
                    onAbout = { screen = Screen.ABOUT }
                )

                Screen.SETTINGS -> SettingsScreen(
                    prefs = prefs,
                    onBack = { screen = Screen.MENU }
                )

                Screen.ABOUT -> AboutScreen(onBack = { screen = Screen.MENU })

                Screen.PLAYING -> GameScreen(
                    world = world,
                    onPause = { screen = Screen.PAUSED }
                )

                Screen.PAUSED -> {
                    GameScreen(world = world, onPause = {})
                    PauseOverlay(
                        onResume = { screen = Screen.PLAYING },
                        onRestart = {
                            world.reset()
                            world.onGameOver = {
                                val isRecord = world.score > prefs.best
                                prefs.saveBest(world.score)
                                newRecord = isRecord
                                screen = Screen.GAMEOVER
                            }
                            screen = Screen.PLAYING
                        },
                        onMenu = { screen = Screen.MENU }
                    )
                }

                Screen.GAMEOVER -> GameOverScreen(
                    score = world.score,
                    distance = world.distance.toInt(),
                    barrels = world.barrels,
                    best = prefs.best,
                    isNewRecord = newRecord,
                    onRetry = {
                        world.reset()
                        world.onGameOver = {
                            val isRecord = world.score > prefs.best
                            prefs.saveBest(world.score)
                            newRecord = isRecord
                            screen = Screen.GAMEOVER
                        }
                        screen = Screen.PLAYING
                    },
                    onMenu = { screen = Screen.MENU }
                )
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D47A1), Color(0xFF64B5F6))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(160.dp, 80.dp)) {
                val w = size.width
                val h = size.height
                val hull = Path()
                hull.moveTo(w * 0.02f, h * 0.42f)
                hull.lineTo(w * 0.78f, h * 0.42f)
                hull.lineTo(w * 0.96f, h * 0.62f)
                hull.lineTo(w * 0.86f, h * 0.92f)
                hull.lineTo(w * 0.10f, h * 0.92f)
                hull.lineTo(0f, h * 0.66f)
                hull.close()
                drawPath(hull, Color(0xFFB71C1C))
                for (i in 0..2) {
                    drawRoundRect(
                        Color(0xFF9E9E9E),
                        Offset(w * (0.12f + i * 0.22f), h * 0.10f),
                        Size(w * 0.16f, h * 0.28f),
                        CornerRadius(h * 0.08f)
                    )
                }
                drawRect(Color(0xFF424242), Offset(w * 0.05f, 0f), Size(w * 0.08f, h * 0.44f))
                drawLine(
                    Color(0xFFFFFFFF),
                    Offset(0f, h * 0.72f),
                    Offset(w, h * 0.72f),
                    strokeWidth = h * 0.05f
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "نفتکش",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Oil Tanker",
                fontSize = 18.sp,
                color = Color(0xCCFFFFFF)
            )
        }
    }
}

@Composable
private fun MainMenuScreen(
    best: Int,
    onStart: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF64B5F6))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "نفتکش",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "بهترین رکورد: $best",
                    fontSize = 20.sp,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Bold
                )
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MenuButton("شروع بازی", Color(0xFF2E7D32), onStart)
                Spacer(Modifier.height(14.dp))
                MenuButton("تنظیمات", Color(0xFF1565C0), onSettings)
                Spacer(Modifier.height(14.dp))
                MenuButton("درباره", Color(0xFF6A1B9A), onAbout)
            }
        }
    }
}

@Composable
private fun MenuButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(width = 210.dp, height = 56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun SettingsScreen(prefs: Prefs, onBack: () -> Unit) {
    BackHandler { onBack() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF102027)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "تنظیمات",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(24.dp))

            Row(
                Modifier.width(340.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("صدا", fontSize = 20.sp, color = Color.White)
                Switch(
                    checked = prefs.sound,
                    onCheckedChange = { prefs.saveSound(it) }
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.width(340.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("لرزش", fontSize = 20.sp, color = Color.White)
                Switch(
                    checked = prefs.vibration,
                    onCheckedChange = { prefs.saveVibration(it) }
                )
            }

            Spacer(Modifier.height(34.dp))
            MenuButton("بازگشت", Color(0xFF37474F), onBack)
        }
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF102027)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "درباره بازی",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "نفتکش یک بازی دوانی بی‌پایان است.\n" +
                        "کشتی را با کشیدن انگشت بالا و پایین ببر،\n" +
                        "از صخره‌ها، مین‌ها، کوه یخ، دزدان دریایی و نهنگ‌ها دوری کن،\n" +
                        "بشکه‌های نفت را جمع کن و در نبرد با کشتی جنگی پیروز شو.",
                fontSize = 17.sp,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "نسخه ۱.۰",
                fontSize = 15.sp,
                color = Color(0xFF78909C)
            )
            Spacer(Modifier.height(30.dp))
            MenuButton("بازگشت", Color(0xFF37474F), onBack)
        }
    }
}

@Composable
private fun GameScreen(world: GameWorld, onPause: () -> Unit) {
    LaunchedEffect(Unit) {
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    world.update(dt)
                }
                last = now
                world.frame++
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF06132B))
            .onSizeChanged {
                world.resize(it.width.toFloat(), it.height.toFloat())
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    world.onDrag(dragAmount.y)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            world.frame.let { world.drawAll() }
        }

        HudBar(world = world, onPause = onPause)

        if (world.bossActive) {
            Button(
                onClick = { world.fire() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 30.dp, bottom = 18.dp)
                    .size(width = 120.dp, height = 58.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315))
            ) {
                Text(
                    "شلیک",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun HudBar(world: GameWorld, onPause: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(MAX_LIVES) { i ->
                Text(
                    text = if (i < world.lives) "❤" else "♡",
                    fontSize = 20.sp,
                    color = if (i < world.lives) Color(0xFFFF5252) else Color(0x55FFFFFF)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "🛢 ${world.barrels}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F)
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "${world.distance.toInt()} متر",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (world.shieldTime > 0f) PowerChip(Color(0xFF29B6F6), world.shieldTime / 5f)
            if (world.magnetTime > 0f) PowerChip(Color(0xFFAB47BC), world.magnetTime / 10f)
            if (world.turboTime > 0f) PowerChip(Color(0xFFFF7043), world.turboTime / 3f)

            Spacer(Modifier.width(12.dp))
            Text(
                text = "${world.score}",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "II",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onPause() }
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun PowerChip(color: Color, progress: Float) {
    Box(
        Modifier
            .padding(horizontal = 3.dp)
            .size(width = 38.dp, height = 10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0x66000000))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(5.dp))
                .background(color)
        )
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    BackHandler { onResume() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "توقف بازی",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(22.dp))
            MenuButton("ادامه", Color(0xFF2E7D32), onResume)
            Spacer(Modifier.height(12.dp))
            MenuButton("شروع مجدد", Color(0xFFEF6C00), onRestart)
            Spacer(Modifier.height(12.dp))
            MenuButton("منوی اصلی", Color(0xFF37474F), onMenu)
        }
    }
}

@Composable
private fun GameOverScreen(
    score: Int,
    distance: Int,
    barrels: Int,
    best: Int,
    isNewRecord: Boolean,
    onRetry: () -> Unit,
    onMenu: () -> Unit
) {
    BackHandler { onMenu() }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A237E), Color(0xFF0D47A1))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "پایان بازی",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5252)
                )
                Spacer(Modifier.height(14.dp))
                Text("امتیاز: $score", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("مسافت: $distance متر", fontSize = 18.sp, color = Color(0xFFB0BEC5))
                Spacer(Modifier.height(6.dp))
                Text("بشکه‌ها: $barrels", fontSize = 18.sp, color = Color(0xFFFFD54F))
                Spacer(Modifier.height(10.dp))
                Text("بهترین رکورد: $best", fontSize = 20.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                if (isNewRecord) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "رکورد جدید!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFEB3B)
                    )
                }
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MenuButton("تلاش مجدد", Color(0xFF2E7D32), onRetry)
                Spacer(Modifier.height(14.dp))
                MenuButton("منوی اصلی", Color(0xFF37474F), onMenu)
            }
        }
    }
}

// ============================================================================
//  ACTIVITY
// ============================================================================

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        setContent {
            OilTankerApp()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
