package com.oiltanker

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
//  CONSTANTS
// ============================================================================

const val V2_MAX_LIVES = 3
const val V2_BASE_SPEED = 30f
const val CH1_TARGET_METERS = 3000f
const val CH3_TARGET_ENEMIES = 20
const val SAVE_KEY = "oil_tanker_v2_save"

// ============================================================================
//  ENUMS
// ============================================================================

enum class Chapter { INTRO, CH1, CH2, CH3, VICTORY, DEFEAT }
enum class BossType { LEVIATHAN, KRAKEN }
enum class SkyEnemyType { DRONE, JET, HELI, BOMBER }
enum class ObstacleKind { ROCK, MINE, ICEBERG, WHALE, PIRATE }
enum class PickupKind { BARREL, HEART, SHIELD, MAGNET, TURBO }

// ============================================================================
//  ENTITIES
// ============================================================================

class V2Ship {
    var yFrac: Float = 0.5f
    var targetYFrac: Float = 0.5f
    var invuln: Float = 0f
    var shield: Float = 0f
    var magnet: Float = 0f
    var turbo: Float = 0f
}

class V2Obstacle(
    var kind: ObstacleKind,
    var x: Float,
    var y: Float,
    var w: Float,
    var h: Float,
    var phase: Float
) {
    var baseY: Float = y
    var fireTimer: Float = 2f
    var flash: Float = 0f
}

class V2Pickup(
    var kind: PickupKind,
    var x: Float,
    var y: Float,
    var baseY: Float,
    var phase: Float,
    var r: Float
)

class V2Bullet(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var r: Float,
    var enemy: Boolean
)

class V2Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var maxLife: Float,
    var size: Float,
    var color: Long,
    var gravity: Float = 0f
)

class V2Boss(
    var type: BossType,
    var x: Float,
    var y: Float,
    var w: Float,
    var h: Float
) {
    var hp: Int = when (type) {
        BossType.LEVIATHAN -> 15
        BossType.KRAKEN -> 20
    }
    var maxHp: Int = hp
    var baseY: Float = y
    var targetX: Float = x
    var flash: Float = 0f
    var fireTimer: Float = 1.8f
    var entering: Boolean = true
    var phase: Float = 0f
    var attackPattern: Int = 0
    var patternTimer: Float = 0f
    var submerged: Float = 0f
}

class V2Plane {
    var xFrac: Float = 0.25f
    var yFrac: Float = 0.5f
    var targetYFrac: Float = 0.5f
    var invuln: Float = 0f
    var fireCd: Float = 0f
    var missileCd: Float = 0f
}

class V2SkyEnemy(
    var type: SkyEnemyType,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var w: Float,
    var h: Float
) {
    var hp: Int = when (type) {
        SkyEnemyType.DRONE -> 1
        SkyEnemyType.JET -> 3
        SkyEnemyType.HELI -> 2
        SkyEnemyType.BOMBER -> 5
    }
    var fireTimer: Float = 1.5f + Random.nextFloat() * 1.5f
    var flash: Float = 0f
    var phase: Float = Random.nextFloat() * 6f
}

class V2Missile(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var r: Float,
    var enemy: Boolean
) {
    var life: Float = 3f
}

// ============================================================================
//  SAVE DATA
// ============================================================================

class SaveData(context: Context) {
    private val sp = context.getSharedPreferences(SAVE_KEY, Context.MODE_PRIVATE)

    var bestScore by mutableStateOf(sp.getInt("bestScore", 0))
    var highestChapter by mutableStateOf(sp.getInt("highestChapter", 1))
    var totalKills by mutableStateOf(sp.getInt("totalKills", 0))
    var totalDistance by mutableStateOf(sp.getInt("totalDistance", 0))
    var storySeen by mutableStateOf(sp.getBoolean("storySeen", false))
    var soundOn by mutableStateOf(sp.getBoolean("soundOn", true))
    var musicOn by mutableStateOf(sp.getBoolean("musicOn", true))
    var musicVol by mutableStateOf(sp.getFloat("musicVol", 0.5f))
    var sfxOn by mutableStateOf(sp.getBoolean("sfxOn", true))
    var vibrationOn by mutableStateOf(sp.getBoolean("vibrationOn", true))
    var sensitivity by mutableStateOf(sp.getFloat("sensitivity", 1f))

    fun persistScore(score: Int) {
        if (score > bestScore) {
            bestScore = score
            sp.edit().putInt("bestScore", score).apply()
        }
    }

    fun persistChapter(c: Int) {
        if (c > highestChapter) {
            highestChapter = c
            sp.edit().putInt("highestChapter", c).apply()
        }
    }

    fun persistStorySeen() {
        storySeen = true
        sp.edit().putBoolean("storySeen", true).apply()
    }

    fun persistAll() {
        sp.edit()
            .putBoolean("soundOn", soundOn)
            .putBoolean("musicOn", musicOn)
            .putFloat("musicVol", musicVol)
            .putBoolean("sfxOn", sfxOn)
            .putBoolean("vibrationOn", vibrationOn)
            .putFloat("sensitivity", sensitivity)
            .apply()
    }

    fun addKills(n: Int) {
        totalKills += n
        sp.edit().putInt("totalKills", totalKills).apply()
    }

    fun addDistance(m: Int) {
        totalDistance += m
        sp.edit().putInt("totalDistance", totalDistance).apply()
    }

    fun reset() {
        sp.edit().clear().apply()
        bestScore = 0
        highestChapter = 1
        totalKills = 0
        totalDistance = 0
        storySeen = false
    }
}

// ============================================================================
//  GAME WORLD v2
// ============================================================================

class V2GameWorld(
    private val music: MusicEngine,
    val save: SaveData
) {
    var viewW = 0f
    var viewH = 0f

    // Reactive state (used by Compose UI)
    var frame by mutableStateOf(0)
    var score by mutableStateOf(0)
    var lives by mutableStateOf(V2_MAX_LIVES)
    var distance by mutableStateOf(0f)
    var barrels by mutableStateOf(0)
    var kills by mutableStateOf(0)
    var chapter by mutableStateOf(Chapter.CH1)
    var bossActive by mutableStateOf(false)
    var bossHp by mutableStateOf(0)
    var bossMaxHp by mutableStateOf(0)
    var bossType by mutableStateOf(BossType.LEVIATHAN)
    var fighterMode by mutableStateOf(false)
    var currentLine by mutableStateOf<DialogueLine?>(null)
    var dialogueVisible by mutableStateOf(false)
    var missionText by mutableStateOf(Story.missionText(1))
    var objectiveText by mutableStateOf(Story.objectiveText(1))

    var onChapterComplete: ((Chapter) -> Unit)? = null
    var onGameOver: (() -> Unit)? = null

    // Internal
    private var scoreF = 0f
    private var time = 0f
    private var worldScroll = 0f
    private var over = false
    private var chapterSwitching = false
    private var dialogueTimer = 0f
    private var chatterIndex = 0
    private var chatterMeterStep = 0

    val ship = V2Ship()
    val plane = V2Plane()

    val obstacles = mutableListOf<V2Obstacle>()
    val pickups = mutableListOf<V2Pickup>()
    val bullets = mutableListOf<V2Bullet>()
    val particles = mutableListOf<V2Particle>()
    val skyEnemies = mutableListOf<V2SkyEnemy>()
    val missiles = mutableListOf<V2Missile>()
    var boss: V2Boss? = null

    private var spawnTimer = 1.6f
    private var pickupTimer = 2.5f
    private var skySpawnTimer = 1.5f
    private var smokeTimer = 0f
    private var redFlash = 0f
    private var whiteFlash = 0f

    private var stormTime = 0f
    private var stormLevel = 0f
    private var lightningTimer = 1f
    private var nextStormAt = 3000f

    private var dayNight = 0f
    private var dayNightFrom = 0f
    private var dayNightTarget = 0f
    private var dayNightLerp = 1f
    private var dayNightIdx = 0

    private var planeFireTimer = 0f

    // ------------------------------------------------------------------
    //  Layout helpers
    // ------------------------------------------------------------------
    val surfaceY: Float get() = viewH * 0.10f
    val shipW: Float get() = viewW * 0.16f
    val shipH: Float get() = shipW * 0.55f
    val shipCX: Float get() = viewW * 0.20f
    val shipCY: Float get() = ship.yFrac * viewH
    val planeW: Float get() = viewW * 0.20f
    val planeH: Float get() = planeW * 0.35f
    val planeCX: Float get() = plane.xFrac * viewW
    val planeCY: Float get() = plane.yFrac * viewH

    fun resize(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        if (viewW == w && viewH == h) return
        val first = viewW == 0f
        viewW = w
        viewH = h
        if (first) {
            ship.yFrac = 0.5f
            ship.targetYFrac = 0.5f
            plane.yFrac = 0.5f
            plane.targetYFrac = 0.5f
        }
    }

    // ------------------------------------------------------------------
    //  RESET / INIT
    // ------------------------------------------------------------------
    fun startNewGame() {
        score = 0
        scoreF = 0f
        lives = V2_MAX_LIVES
        distance = 0f
        barrels = 0
        kills = 0
        over = false
        time = 0f
        worldScroll = 0f
        chapterSwitching = false
        dialogueTimer = 0f
        chatterIndex = 0
        chatterMeterStep = 0

        ship.yFrac = 0.5f
        ship.targetYFrac = 0.5f
        ship.invuln = 0f
        ship.shield = 0f
        ship.magnet = 0f
        ship.turbo = 0f

        plane.xFrac = 0.25f
        plane.yFrac = 0.5f
        plane.targetYFrac = 0.5f
        plane.invuln = 0f
        plane.fireCd = 0f
        plane.missileCd = 0f

        obstacles.clear()
        pickups.clear()
        bullets.clear()
        particles.clear()
        skyEnemies.clear()
        missiles.clear()
        boss = null

        spawnTimer = 1.6f
        pickupTimer = 2.5f
        skySpawnTimer = 1.5f
        smokeTimer = 0f
        redFlash = 0f
        whiteFlash = 0f
        stormTime = 0f
        stormLevel = 0f
        lightningTimer = 1f
        nextStormAt = 3000f
        dayNight = 0f
        dayNightFrom = 0f
        dayNightTarget = 0f
        dayNightLerp = 1f
        dayNightIdx = 0
        fighterMode = false
        bossActive = false
        bossHp = 0
        bossMaxHp = 0

        setChapter(Chapter.CH1)
    }

    fun setChapter(c: Chapter) {
        chapter = c
        missionText = Story.missionText(
            when (c) {
                Chapter.CH1 -> 1
                Chapter.CH2 -> 2
                Chapter.CH3 -> 3
                else -> 1
            }
        )
        objectiveText = Story.objectiveText(
            when (c) {
                Chapter.CH1 -> 1
                Chapter.CH2 -> 2
                Chapter.CH3 -> 3
                else -> 1
            }
        )
        fighterMode = (c == Chapter.CH3)
        when (c) {
            Chapter.CH1 -> music.play(MusicTrack.LEVEL)
            Chapter.CH2 -> music.play(MusicTrack.BOSS)
            Chapter.CH3 -> music.play(MusicTrack.LEVEL)
            Chapter.VICTORY -> music.play(MusicTrack.VICTORY)
            Chapter.DEFEAT -> music.play(MusicTrack.GAMEOVER)
            Chapter.INTRO -> music.play(MusicTrack.STORY)
        }
    }

    // ------------------------------------------------------------------
    //  INPUT
    // ------------------------------------------------------------------
    fun onDrag(dy: Float) {
        if (viewH <= 0f) return
        val sens = save.sensitivity
        if (fighterMode) {
            plane.targetYFrac = (plane.targetYFrac + dy / viewH * sens).coerceIn(0.10f, 0.90f)
        } else {
            ship.targetYFrac = (ship.targetYFrac + dy / viewH * sens).coerceIn(0.15f, 0.85f)
        }
    }

    fun firePlayer() {
        if (over) return
        if (fighterMode) {
            if (plane.fireCd > 0f) return
            plane.fireCd = 0.14f
            bullets.add(
                V2Bullet(planeCX + planeW * 0.45f, planeCY, viewW * 1.1f, 0f, viewH * 0.011f, false)
            )
            bullets.add(
                V2Bullet(planeCX + planeW * 0.45f, planeCY - planeH * 0.22f, viewW * 1.1f, -viewH * 0.15f, viewH * 0.010f, false)
            )
            bullets.add(
                V2Bullet(planeCX + planeW * 0.45f, planeCY + planeH * 0.22f, viewW * 1.1f, viewH * 0.15f, viewH * 0.010f, false)
            )
        } else {
            if (boss == null) return
            if (plane.fireCd > 0f) return
            plane.fireCd = 0.22f
            bullets.add(
                V2Bullet(shipCX + shipW * 0.52f, shipCY, viewW * 0.95f, 0f, viewH * 0.013f, false)
            )
        }
    }

    fun fireMissile() {
        if (!fighterMode || over) return
        if (plane.missileCd > 0f) return
        plane.missileCd = 1.4f
        missiles.add(
            V2Missile(planeCX + planeW * 0.4f, planeCY, viewW * 0.85f, 0f, viewH * 0.020f, false)
        )
    }

    // ------------------------------------------------------------------
    //  UPDATE
    // ------------------------------------------------------------------
    fun update(dtRaw: Float) {
        if (viewW <= 0f || viewH <= 0f) return
        val dt = dtRaw.coerceIn(0f, 0.05f)
        time += dt

        // dialogue timer
        if (dialogueVisible) {
            dialogueTimer -= dt
            if (dialogueTimer <= 0f) {
                dialogueVisible = false
                currentLine = null
            }
        }

        // damage flash
        if (redFlash > 0f) redFlash = max(0f, redFlash - dt * 1.5f)
        if (whiteFlash > 0f) whiteFlash = max(0f, whiteFlash - dt * 2.5f)

        if (over) {
            updateParticles(dt)
            return
        }

        // timers
        if (ship.invuln > 0f) ship.invuln = max(0f, ship.invuln - dt)
        if (plane.invuln > 0f) plane.invuln = max(0f, plane.invuln - dt)
        if (ship.shield > 0f) ship.shield = max(0f, ship.shield - dt)
        if (ship.magnet > 0f) ship.magnet = max(0f, ship.magnet - dt)
        if (ship.turbo > 0f) ship.turbo = max(0f, ship.turbo - dt)
        if (plane.fireCd > 0f) plane.fireCd = max(0f, plane.fireCd - dt)
        if (plane.missileCd > 0f) plane.missileCd = max(0f, plane.missileCd - dt)

        // movement lerp
        if (fighterMode) {
            plane.yFrac += (plane.targetYFrac - plane.yFrac) * min(1f, dt * 10f)
        } else {
            ship.yFrac += (ship.targetYFrac - ship.yFrac) * min(1f, dt * 9f)
        }

        // scroll & score
        val mps = metersPerSec()
        if (!chapterSwitching) {
            distance += mps * dt
            scoreF += mps * dt * 0.5f
        }
        score = scoreF.toInt()

        updateDayNight(dt)
        updateStorm(dt)

        when (chapter) {
            Chapter.CH1 -> updateChapter1(dt, mps)
            Chapter.CH2 -> updateChapter2(dt, mps)
            Chapter.CH3 -> updateChapter3(dt, mps)
            else -> {}
        }

        updateObstacles(dt)
        updatePickups(dt)
        updateBullets(dt)
        updateMissiles(dt)
        updateSkyEnemies(dt)
        updateBoss(dt)
        updateParticles(dt)

        if (!chapterSwitching) {
            collide()
        }
    }

    private fun metersPerSec(): Float {
        val step = floor(distance / 100f)
        val base = V2_BASE_SPEED * (1f + 0.05f * step).coerceAtMost(3.4f)
        return base * (if (ship.turbo > 0f) 2f else 1f)
    }

    private fun scrollSpeedPx(): Float = metersPerSec() * (viewH / 100f)

    // ------------------------------------------------------------------
    //  CHAPTERS
    // ------------------------------------------------------------------
    private fun updateChapter1(dt: Float, mps: Float) {
        // spawn obstacles
        if (boss == null) {
            spawnTimer -= dt
            if (spawnTimer <= 0f) {
                spawnObstacle()
                val base = (1.75f * 0.9f.pow(distance / 500f)).coerceAtLeast(0.5f)
                spawnTimer = base * (0.75f + Random.nextFloat() * 0.5f)
            }
        }

        // spawn pickups
        pickupTimer -= dt
        if (pickupTimer <= 0f) {
            spawnPickup()
            pickupTimer = 2.2f + Random.nextFloat() * 3.0f
        }

        // smoke
        if (!fighterMode) {
            smokeTimer -= dt
            if (smokeTimer <= 0f) {
                smokeTimer = 0.10f
                spawnSmoke()
            }
        }

        // radio chatter
        val step = floor(distance / 150f).toInt()
        if (step > chatterMeterStep && chatterIndex < Story.radioChatter.size) {
            chatterMeterStep = step
            showLine(Story.radioChatter[chatterIndex].first())
            chatterIndex += 1
        }

        // boss trigger at end of CH1 (Leviathan)
        if (distance >= CH1_TARGET_METERS && boss == null && !chapterSwitching) {
            spawnBoss(BossType.LEVIATHAN)
            showLine(Story.leviathanIntro.first())
            music.play(MusicTrack.BOSS)
        }
    }

    private fun updateChapter2(dt: Float, mps: Float) {
        // only boss fight
        if (boss == null && !chapterSwitching) {
            // if boss defeated, move to next chapter
            chapterSwitching = true
        }
    }

    private fun updateChapter3(dt: Float, mps: Float) {
        // sky enemies
        skySpawnTimer -= dt
        if (skySpawnTimer <= 0f) {
            spawnSkyEnemy()
            skySpawnTimer = (0.9f - (kills * 0.015f)).coerceAtLeast(0.4f) + Random.nextFloat() * 0.6f
        }

        // spawn some pickups
        pickupTimer -= dt
        if (pickupTimer <= 0f) {
            spawnPickup()
            pickupTimer = 3.5f + Random.nextFloat() * 2.0f
        }
    }

    // ------------------------------------------------------------------
    //  SPAWNERS
    // ------------------------------------------------------------------
    private fun spawnObstacle() {
        val r = Random.nextFloat()
        val kind = when {
            r < 0.28f -> ObstacleKind.ROCK
            r < 0.50f -> ObstacleKind.MINE
            r < 0.68f -> ObstacleKind.ICEBERG
            r < 0.86f -> ObstacleKind.WHALE
            else -> ObstacleKind.PIRATE
        }
        val y = viewH * (0.22f + Random.nextFloat() * 0.60f)
        val w: Float
        val h: Float
        when (kind) {
            ObstacleKind.ROCK -> { w = viewH * 0.20f; h = viewH * 0.20f }
            ObstacleKind.MINE -> { w = viewH * 0.11f; h = viewH * 0.11f }
            ObstacleKind.ICEBERG -> { w = viewH * 0.24f; h = viewH * 0.26f }
            ObstacleKind.WHALE -> { w = viewH * 0.34f; h = viewH * 0.18f }
            ObstacleKind.PIRATE -> { w = viewH * 0.32f; h = viewH * 0.26f }
        }
        obstacles.add(V2Obstacle(kind, viewW + w, y, w, h, Random.nextFloat() * 10f))
    }

    private fun spawnPickup() {
        val r = Random.nextFloat()
        val kind = when {
            r < 0.55f -> PickupKind.BARREL
            r < 0.68f -> PickupKind.HEART
            r < 0.80f -> PickupKind.SHIELD
            r < 0.92f -> PickupKind.MAGNET
            else -> PickupKind.TURBO
        }
        val y = viewH * (0.22f + Random.nextFloat() * 0.58f)
        pickups.add(
            V2Pickup(kind, viewW + viewH * 0.08f, y, y, Random.nextFloat() * 6f, viewH * 0.038f)
        )
    }

    private fun spawnSmoke() {
        val fx = shipCX - shipW * 0.44f
        val fy = shipCY - shipH * 0.62f
        particles.add(
            V2Particle(
                fx, fy,
                -viewW * 0.010f - Random.nextFloat() * viewW * 0.012f,
                -viewH * 0.055f - Random.nextFloat() * viewH * 0.03f,
                1.4f, 1.4f,
                viewW * 0.010f + Random.nextFloat() * viewW * 0.006f,
                0x889E9E9E.toInt()
            )
        )
        capParticles()
    }

    private fun spawnBoss(type: BossType) {
        val w = viewH * 0.62f
        val h = viewH * 0.46f
        val b = V2Boss(type, viewW + w * 0.85f, viewH * 0.5f, w, h)
        b.baseY = b.y
        b.targetX = viewW * 0.78f
        boss = b
        bossActive = true
        bossHp = b.hp
        bossMaxHp = b.maxHp
        bossType = type
    }

    private fun spawnSkyEnemy() {
        val r = Random.nextFloat()
        val type = when {
            r < 0.45f -> SkyEnemyType.DRONE
            r < 0.70f -> SkyEnemyType.HELI
            r < 0.90f -> SkyEnemyType.JET
            else -> SkyEnemyType.BOMBER
        }
        val y = viewH * (0.10f + Random.nextFloat() * 0.80f)
        val w = when (type) {
            SkyEnemyType.DRONE -> viewH * 0.08f
            SkyEnemyType.HELI -> viewH * 0.13f
            SkyEnemyType.JET -> viewH * 0.14f
            SkyEnemyType.BOMBER -> viewH * 0.20f
        }
        val h = when (type) {
            SkyEnemyType.DRONE -> viewH * 0.05f
            SkyEnemyType.HELI -> viewH * 0.08f
            SkyEnemyType.JET -> viewH * 0.05f
            SkyEnemyType.BOMBER -> viewH * 0.10f
        }
        val vx = when (type) {
            SkyEnemyType.DRONE -> -viewW * 0.22f
            SkyEnemyType.HELI -> -viewW * 0.18f
            SkyEnemyType.JET -> -viewW * 0.42f
            SkyEnemyType.BOMBER -> -viewW * 0.14f
        }
        skyEnemies.add(V2SkyEnemy(type, viewW + w, y, vx, 0f, w, h))
    }

    private fun capParticles() {
        while (particles.size > 420) particles.removeAt(0)
    }

    private fun burst(x: Float, y: Float, radius: Float, colorA: Long = 0xFFFFB300, colorB: Long = 0xFFFF5722) {
        for (i in 0 until 22) {
            val a = Random.nextFloat() * PI.toFloat() * 2f
            val sp = radius * (0.8f + Random.nextFloat() * 2.4f)
            particles.add(
                V2Particle(
                    x, y,
                    cos(a) * sp, sin(a) * sp,
                    0.55f + Random.nextFloat() * 0.5f, 1.05f,
                    radius * (0.10f + Random.nextFloat() * 0.16f),
                    (if (i % 2 == 0) colorA else colorB).toInt()
                )
            )
        }
        capParticles()
    }

    // ------------------------------------------------------------------
    //  UPDATERS
    // ------------------------------------------------------------------
    private fun updateDayNight(dt: Float) {
        val idx = floor(distance / 2000f).toInt()
        if (idx != dayNightIdx) {
            dayNightIdx = idx
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
        if (distance >= nextStormAt) {
            nextStormAt += 3000f
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

    private fun updateObstacles(dt: Float) {
        val scroll = scrollSpeedPx()
        val it = obstacles.iterator()
        while (it.hasNext()) {
            val o = it.next()
            o.phase += dt
            val extra = when (o.kind) {
                ObstacleKind.WHALE -> scroll * 0.35f
                ObstacleKind.PIRATE -> -scroll * 0.25f
                else -> 0f
            }
            o.x -= (scroll + extra) * dt

            when (o.kind) {
                ObstacleKind.WHALE -> o.y = o.baseY + sin(o.phase * 1.6f) * viewH * 0.05f
                ObstacleKind.PIRATE -> {
                    o.fireTimer -= dt
                    if (o.fireTimer <= 0f && o.x < viewW * 0.98f && o.x > viewW * 0.28f) {
                        o.fireTimer = 2.4f
                        bullets.add(
                            V2Bullet(o.x - o.w * 0.5f, o.y, -viewW * 0.42f, 0f, viewH * 0.014f, true)
                        )
                    }
                }
                else -> {}
            }

            if (o.flash > 0f) o.flash -= dt * 3f
            if (o.x < -viewW * 0.30f) it.remove()
        }
    }

    private fun updatePickups(dt: Float) {
        val scroll = scrollSpeedPx()
        val it = pickups.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.phase += dt
            p.x -= scroll * dt
            p.y = p.baseY + sin(p.phase * 2f) * viewH * 0.015f

            if (ship.magnet > 0f && p.kind == PickupKind.BARREL && !fighterMode) {
                val dx = shipCX - p.x
                val dy = shipCY - p.y
                val d = sqrt(dx * dx + dy * dy)
                if (d < viewH * 0.65f && d > 1f) {
                    p.x += dx / d * viewH * 1.0f * dt
                    p.y += dy / d * viewH * 1.0f * dt
                    p.baseY = p.y
                }
            }

            if (p.x < -viewW * 0.15f) it.remove()
        }
    }

    private fun updateBullets(dt: Float) {
        val it = bullets.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.x += b.vx * dt
            b.y += b.vy * dt
            if (b.x < -viewW * 0.2f || b.x > viewW * 1.2f ||
                b.y < -viewH * 0.2f || b.y > viewH * 1.2f
            ) it.remove()
        }
    }

    private fun updateMissiles(dt: Float) {
        val it = missiles.iterator()
        while (it.hasNext()) {
            val m = it.next()
            m.life -= dt
            m.x += m.vx * dt
            m.y += m.vy * dt
            if (m.life <= 0f || m.x > viewW * 1.3f) it.remove()
        }
    }

    private fun updateSkyEnemies(dt: Float) {
        val it = skyEnemies.iterator()
        while (it.hasNext()) {
            val e = it.next()
            e.phase += dt
            e.x += e.vx * dt
            if (e.type == SkyEnemyType.HELI || e.type == SkyEnemyType.BOMBER) {
                e.y += sin(e.phase * 1.4f) * viewH * 0.05f * dt * 8f
            }
            if (e.flash > 0f) e.flash = max(0f, e.flash - dt * 3f)

            // fire at plane
            if (!e.x.let { it < -viewW * 0.2f || it > viewW * 1.2f }) {
                e.fireTimer -= dt
                if (e.fireTimer <= 0f) {
                    e.fireTimer = 1.6f + Random.nextFloat() * 1.6f
                    val dx = planeCX - e.x
                    val dy = planeCY - e.y
                    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    val sp = viewW * 0.40f
                    bullets.add(
                        V2Bullet(e.x, e.y, dx / len * sp, dy / len * sp, viewH * 0.014f, true)
                    )
                }
            }

            if (e.x < -viewW * 0.30f) it.remove()
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
            return
        }

        b.patternTimer -= dt
        if (b.patternTimer <= 0f) {
            b.attackPattern = (b.attackPattern + 1) % 3
            b.patternTimer = 3f
        }

        when (b.type) {
            BossType.LEVIATHAN -> {
                b.x = b.targetX + sin(b.phase * 0.8f) * viewH * 0.02f
                b.y = b.baseY + sin(b.phase * 1.3f) * viewH * 0.09f
                b.fireTimer -= dt
                if (b.fireTimer <= 0f) {
                    b.fireTimer = when (b.attackPattern) {
                        0 -> 1.4f
                        1 -> 1.0f
                        else -> 2.0f
                    }
                    fireBossAttack(b)
                }
            }
            BossType.KRAKEN -> {
                b.x = b.targetX + sin(b.phase * 0.5f) * viewH * 0.03f
                b.y = b.baseY + sin(b.phase * 1.0f) * viewH * 0.06f
                b.fireTimer -= dt
                if (b.fireTimer <= 0f) {
                    b.fireTimer = 0.9f
                    fireBossAttack(b)
                }
            }
        }
    }

    private fun fireBossAttack(b: V2Boss) {
        when (b.attackPattern) {
            0 -> {
                // single shot at player
                val originX = b.x - b.w * 0.5f
                val originY = b.y
                val tx = if (fighterMode) planeCX else shipCX
                val ty = if (fighterMode) planeCY else shipCY
                val dx = tx - originX
                val dy = ty - originY
                val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val sp = viewW * 0.50f
                bullets.add(V2Bullet(originX, originY, dx / len * sp, dy / len * sp, viewH * 0.018f, true))
            }
            1 -> {
                // spread of 3
                val originX = b.x - b.w * 0.5f
                val originY = b.y
                for (i in -1..1) {
                    val ang = i * 0.25f
                    val sp = viewW * 0.45f
                    bullets.add(V2Bullet(originX, originY, -sp, sp * sin(ang), viewH * 0.016f, true))
                }
            }
            else -> {
                // rapid fire small bullets
                for (i in 0 until 3) {
                    val originX = b.x - b.w * 0.5f
                    val originY = b.y + (i - 1) * b.h * 0.18f
                    bullets.add(V2Bullet(originX, originY, -viewW * 0.60f, 0f, viewH * 0.012f, true))
                }
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += p.gravity * dt
            p.life -= dt
            if (p.life <= 0f) it.remove()
        }
    }

    // ------------------------------------------------------------------
    //  COLLISION
    // ------------------------------------------------------------------
    private fun collide() {
        if (fighterMode) collideFighter()
        else collideShip()
    }

    private fun collideShip() {
        val sx = shipCX
        val sy = shipCY
        val sw = shipW * 0.62f
        val sh = shipH * 0.72f

        val pit = pickups.iterator()
        while (pit.hasNext()) {
            val p = pit.next()
            if (overlapRect(sx, sy, sw, sh, p.x, p.y, p.r * 1.8f, p.r * 1.8f)) {
                applyPickup(p.kind)
                pit.remove()
            }
        }

        val oit = obstacles.iterator()
        while (oit.hasNext()) {
            val o = oit.next()
            if (overlapRect(sx, sy, sw, sh, o.x, o.y, o.w * 0.78f, o.h * 0.78f)) {
                if (ship.shield > 0f) {
                    burst(o.x, o.y, o.w * 0.4f)
                    scoreF += 15f
                    score = scoreF.toInt()
                    oit.remove()
                } else if (ship.invuln <= 0f) {
                    burst(o.x, o.y, o.w * 0.4f)
                    oit.remove()
                    damageShip()
                }
            }
        }

        val b = boss
        if (b != null && !b.entering) {
            if (overlapRect(sx, sy, sw, sh, b.x, b.y, b.w * 0.70f, b.h * 0.70f)) {
                damageShip()
            }
        }

        val bit = bullets.iterator()
        while (bit.hasNext()) {
            val bullet = bit.next()
            if (bullet.enemy) {
                if (overlapRect(sx, sy, sw, sh, bullet.x, bullet.y, bullet.r * 2f, bullet.r * 2f)) {
                    bit.remove()
                    burst(bullet.x, bullet.y, viewH * 0.03f)
                    damageShip()
                }
            } else {
                val bb = boss
                if (bb != null && !bb.entering &&
                    overlapRect(bullet.x, bullet.y, bullet.r * 2f, bullet.r * 2f,
                        bb.x, bb.y, bb.w * 0.80f, bb.h * 0.80f)
                ) {
                    bit.remove()
                    hitBoss(bb)
                }
            }
        }

        val mit = missiles.iterator()
        while (mit.hasNext()) {
            val m = mit.next()
            if (!m.enemy) {
                val bb = boss
                if (bb != null && !bb.entering &&
                    overlapRect(m.x, m.y, m.r * 3f, m.r * 3f,
                        bb.x, bb.y, bb.w * 0.90f, bb.h * 0.90f)
                ) {
                    mit.remove()
                    hitBoss(bb, damage = 3)
                }
            }
        }
    }

    private fun collideFighter() {
        val px = planeCX
        val py = planeCY
        val pw = planeW * 0.65f
        val ph = planeH * 0.60f

        val pit = pickups.iterator()
        while (pit.hasNext()) {
            val p = pit.next()
            if (overlapRect(px, py, pw, ph, p.x, p.y, p.r * 1.8f, p.r * 1.8f)) {
                applyPickup(p.kind)
                pit.remove()
            }
        }

        val eit = skyEnemies.iterator()
        while (eit.hasNext()) {
            val e = eit.next()
            if (overlapRect(px, py, pw, ph, e.x, e.y, e.w * 0.80f, e.h * 0.80f)) {
                if (plane.invuln <= 0f) damagePlane()
            }
        }

        val bit = bullets.iterator()
        while (bit.hasNext()) {
            val bullet = bit.next()
            if (bullet.enemy) {
                if (overlapRect(px, py, pw, ph, bullet.x, bullet.y, bullet.r * 2f, bullet.r * 2f)) {
                    bit.remove()
                    burst(bullet.x, bullet.y, viewH * 0.03f)
                    damagePlane()
                }
            } else {
                val eit2 = skyEnemies.iterator()
                while (eit2.hasNext()) {
                    val e = eit2.next()
                    if (overlapRect(bullet.x, bullet.y, bullet.r * 2f, bullet.r * 2f,
                            e.x, e.y, e.w * 0.90f, e.h * 0.90f)
                    ) {
                        bit.remove()
                        e.hp -= 1
                        e.flash = 1f
                        if (e.hp <= 0) {
                            burst(e.x, e.y, viewH * 0.06f)
                            kills += 1
                            scoreF += 50f
                            score = scoreF.toInt()
                            eit2.remove()
                            if (kills >= CH3_TARGET_ENEMIES && !chapterSwitching) {
                                completeChapter3()
                            }
                        }
                        break
                    }
                }
            }
        }

        val mit = missiles.iterator()
        while (mit.hasNext()) {
            val m = mit.next()
            if (!m.enemy) {
                val eit2 = skyEnemies.iterator()
                var hit = false
                while (eit2.hasNext()) {
                    val e = eit2.next()
                    if (overlapRect(m.x, m.y, m.r * 3f, m.r * 3f,
                            e.x, e.y, e.w * 1.5f, e.h * 1.5f)
                    ) {
                        burst(e.x, e.y, viewH * 0.08f)
                        kills += 1
                        scoreF += 50f
                        score = scoreF.toInt()
                        eit2.remove()
                        hit = true
                        if (kills >= CH3_TARGET_ENEMIES && !chapterSwitching) {
                            completeChapter3()
                        }
                        break
                    }
                }
                if (hit) mit.remove()
            }
        }
    }

    private fun hitBoss(b: V2Boss, damage: Int = 1) {
        b.hp -= damage
        b.flash = 1f
        bossHp = max(0, b.hp)
        burst(b.x + (Random.nextFloat() - 0.5f) * b.w * 0.5f, b.y + (Random.nextFloat() - 0.5f) * b.h * 0.4f, viewH * 0.05f)
        scoreF += 25f * damage
        score = scoreF.toInt()
        if (b.hp <= 0) defeatBoss()
    }

    private fun damageShip() {
        if (over || ship.invuln > 0f || ship.shield > 0f) return
        lives -= 1
        ship.invuln = 1.4f
        redFlash = 1f
        if (lives <= 0) {
            lives = 0
            triggerGameOver()
        }
    }

    private fun damagePlane() {
        if (over || plane.invuln > 0f) return
        lives -= 1
        plane.invuln = 1.4f
        redFlash = 1f
        if (lives <= 0) {
            lives = 0
            triggerGameOver()
        }
    }

    private fun applyPickup(kind: PickupKind) {
        when (kind) {
            PickupKind.BARREL -> {
                barrels += 1
                scoreF += 10f
                score = scoreF.toInt()
            }
            PickupKind.HEART -> {
                lives = min(V2_MAX_LIVES, lives + 1)
            }
            PickupKind.SHIELD -> ship.shield = 5f
            PickupKind.MAGNET -> ship.magnet = 10f
            PickupKind.TURBO -> ship.turbo = 3f
        }
    }

    private fun defeatBoss() {
        val b = boss ?: return
        burst(b.x, b.y, viewH * 0.30f)
        burst(b.x - b.w * 0.2f, b.y + b.h * 0.1f, viewH * 0.22f)
        boss = null
        bossActive = false
        bossHp = 0
        bossMaxHp = 0
        scoreF += 500f
        score = scoreF.toInt()

        when (chapter) {
            Chapter.CH1, Chapter.CH2 -> {
                // transition to CH3 (fighter mode)
                showLine(Story.leviathanDefeat.first())
                chapterSwitching = true
                setChapter(Chapter.CH3)
                chapterSwitching = false
                showLine(Story.krakenAttack.first())
                fighterMode = true
                // clear water obstacles
                obstacles.clear()
                bullets.clear()
                kills = 0
            }
            else -> {}
        }
    }

    private fun completeChapter3() {
        chapterSwitching = true
        setChapter(Chapter.VICTORY)
        showLine(Story.outro.first())
    }

    private fun triggerGameOver() {
        over = true
        setChapter(Chapter.DEFEAT)
        onGameOver?.invoke()
    }

    // ------------------------------------------------------------------
    //  DIALOGUE
    // ------------------------------------------------------------------
    private fun showLine(line: DialogueLine) {
        currentLine = line
        dialogueTimer = line.duration
        dialogueVisible = true
    }

    fun skipDialogue() {
        dialogueVisible = false
        currentLine = null
        dialogueTimer = 0f
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    private fun overlapRect(
        ax: Float, ay: Float, aw: Float, ah: Float,
        bx: Float, by: Float, bw: Float, bh: Float
    ): Boolean = abs(ax - bx) * 2f < (aw + bw) && abs(ay - by) * 2f < (ah + bh)

    fun tintInt(c: Long): Long {
        val dayMul = 1f - dayNight * 0.5f
        val stormMul = 1f - stormLevel * 0.35f
        val mul = (dayMul * stormMul).coerceIn(0.2f, 1f)
        val r = ((c shr 16) and 0xFF) * mul
        val g = ((c shr 8) and 0xFF) * mul
        val b = (c and 0xFF) * mul
        return (0xFFL shl 24) or (r.toLong().coerceIn(0, 255) shl 16) or
                (g.toLong().coerceIn(0, 255) shl 8) or b.toLong().coerceIn(0, 255)
    }

    fun waveY(x: Float): Float {
        val amp = viewH * (0.012f + stormLevel * 0.030f)
        return surfaceY +
                sin((x + worldScroll) * 0.0060f) * amp +
                sin((x + worldScroll * 1.7f) * 0.0130f + 1.3f) * amp * 0.55f
    }

    fun getWorldScroll(): Float = worldScroll
    fun getDayNight(): Float = dayNight
    fun getStormLevel(): Float = stormLevel
    fun getWhiteFlash(): Float = whiteFlash
    fun getRedFlash(): Float = redFlash
    fun getTime(): Float = time
    fun isOver(): Boolean = over
}
