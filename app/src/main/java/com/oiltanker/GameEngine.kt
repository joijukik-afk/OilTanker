package com.oiltanker

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// ============================================================================
//  MASTER RENDER
// ============================================================================

fun DrawScope.renderV2(world: V2GameWorld) {
    if (world.viewW <= 0f || world.viewH <= 0f) return
    drawV2Sky(world)
    drawV2Water(world)
    drawV2Seabed(world)
    drawV2Pickups(world)
    drawV2Obstacles(world)
    if (world.boss != null) drawV2Boss(world)
    drawV2SkyEnemies(world)
    drawV2Bullets(world)
    drawV2Missiles(world)
    if (world.fighterMode) drawV2Plane(world) else drawV2Ship(world)
    drawV2Particles(world)
    drawV2Weather(world)
    drawV2ScreenEffects(world)
}

private fun v2Tint(world: V2GameWorld, color: Color): Color {
    val dayMul = 1f - world.getDayNight() * 0.5f
    val stormMul = 1f - world.getStormLevel() * 0.35f
    val m = (dayMul * stormMul).coerceIn(0.2f, 1f)
    return Color(
        red = color.red * m,
        green = color.green * m,
        blue = color.blue * m,
        alpha = color.alpha
    )
}

// ============================================================================
//  SKY
// ============================================================================

private fun DrawScope.drawV2Sky(world: V2GameWorld) {
    val dn = world.getDayNight()
    val storm = world.getStormLevel()
    val sy = world.surfaceY

    var top = lerp(Color(0xFF64B5F6), Color(0xFF1A237E), dn)
    var bottom = lerp(Color(0xFFB3E5FC), Color(0xFF101A45), dn)
    top = lerp(top, Color(0xFF37474F), storm * 0.75f)
    bottom = lerp(bottom, Color(0xFF263238), storm * 0.75f)

    drawRect(
        brush = Brush.verticalGradient(listOf(top, bottom), 0f, sy),
        topLeft = Offset.Zero,
        size = Size(world.viewW, sy)
    )

    // Stars: reduced from 70 to 35 for performance
    if (dn > 0.15f) {
        val alpha = ((dn - 0.15f) / 0.85f).coerceIn(0f, 1f) * (1f - storm * 0.8f)
        val rnd = java.util.Random(42L)
        for (i in 0 until 35) {
            val sx = rnd.nextFloat() * world.viewW
            val syS = rnd.nextFloat() * sy * 0.92f
            val twinkle = 0.6f + 0.4f * sin(world.getTime() * 2.5f + i)
            drawCircle(
                Color.White.copy(alpha = alpha * twinkle),
                world.viewH * 0.003f,
                Offset(sx, syS)
            )
        }
    }

    val bodyX = world.viewW * 0.82f
    val bodyY = sy * 0.42f
    val bodyR = world.viewH * 0.045f
    if (dn < 0.5f) {
        val sunColor = lerp(Color(0xFFFFEE58), Color(0xFFFFA726), dn * 2f)
        drawCircle(Color(0x33FFEB3B), bodyR * 1.9f, Offset(bodyX, bodyY))
        drawCircle(sunColor, bodyR, Offset(bodyX, bodyY))
    } else {
        val night = (dn - 0.5f) * 2f
        drawCircle(Color(0x33E1F5FE), bodyR * 1.8f, Offset(bodyX, bodyY))
        drawCircle(
            lerp(Color(0xFFFFF9C4), Color(0xFFE1F5FE), night),
            bodyR,
            Offset(bodyX, bodyY)
        )
        drawCircle(
            Color(0xFF101A45).copy(alpha = night),
            bodyR * 0.85f,
            Offset(bodyX + bodyR * 0.42f, bodyY - bodyR * 0.30f)
        )
    }

    // clouds
    val cloudAlpha = (0.55f * (1f - dn * 0.75f) + storm * 0.25f).coerceIn(0f, 1f)
    val cloudColor = lerp(Color.White, Color(0xFF90A4AE), dn * 0.7f)
    val period = world.viewW * 1.6f
    val base = world.getWorldScroll() * 0.12f
    val startIdx = floor(base / period).toInt() - 1
    for (k in startIdx..(startIdx + 3)) {
        val h1 = (abs(k * 73856093) % 1000) / 1000f
        val h2 = (abs(k * 19349663) % 1000) / 1000f
        val cx = k * period - base + h1 * period * 0.6f
        if (cx < -world.viewW * 0.4f || cx > world.viewW * 1.4f) continue
        val cy = sy * (0.18f + h2 * 0.42f)
        val s = world.viewH * (0.030f + h1 * 0.030f)
        drawOval(
            cloudColor.copy(alpha = cloudAlpha * 0.80f),
            Offset(cx - s * 1.6f, cy - s * 0.45f),
            Size(s * 3.2f, s * 0.9f)
        )
        drawOval(
            cloudColor.copy(alpha = cloudAlpha * 0.90f),
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

// ============================================================================
//  WATER
// ============================================================================

private fun DrawScope.drawV2Water(world: V2GameWorld) {
    val dn = world.getDayNight()
    val storm = world.getStormLevel()
    val sy = world.surfaceY

    var waterTop = lerp(Color(0xFF1976D2), Color(0xFF0A1E4A), dn)
    var waterBottom = lerp(Color(0xFF0D47A1), Color(0xFF050E22), dn)
    waterTop = lerp(waterTop, Color(0xFF2F4A55), storm * 0.6f)
    waterBottom = lerp(waterBottom, Color(0xFF16232B), storm * 0.6f)

    val body = Path()
    body.moveTo(0f, world.waveY(0f))
    var x = 0f
    while (x < world.viewW) {
        x += 14f
        val cx = if (x > world.viewW) world.viewW else x
        body.lineTo(cx, world.waveY(cx))
    }
    body.lineTo(world.viewW, world.viewH)
    body.lineTo(0f, world.viewH)
    body.close()

    drawPath(
        path = body,
        brush = Brush.verticalGradient(
            listOf(waterTop, waterBottom),
            startY = sy,
            endY = world.viewH
        )
    )

    val surface = Path()
    surface.moveTo(0f, world.waveY(0f))
    x = 0f
    while (x < world.viewW) {
        x += 14f
        val cx = if (x > world.viewW) world.viewW else x
        surface.lineTo(cx, world.waveY(cx))
    }
    drawPath(
        surface,
        Color(0x77BBDEFB).copy(alpha = 0.45f * (1f - storm * 0.4f)),
        style = Stroke(width = world.viewH * 0.008f)
    )

    val scroll = world.getWorldScroll()
    for (k in 1..3) {
        val yy = sy + (world.viewH - sy) * (k * 0.17f)
        val wp = Path()
        var first = true
        var xx = 0f
        while (xx <= world.viewW) {
            val py = yy + sin((xx + scroll * 1.4f) * 0.008f + k * 1.7f) * world.viewH * 0.012f
            if (first) {
                wp.moveTo(xx, py); first = false
            } else wp.lineTo(xx, py)
            xx += 22f
        }
        drawPath(
            wp,
            Color(0x22FFFFFF).copy(alpha = 0.14f * (1f - storm * 0.5f)),
            style = Stroke(width = world.viewH * 0.005f)
        )
    }
}

// ============================================================================
//  SEABED
// ============================================================================

private fun DrawScope.drawV2Seabed(world: V2GameWorld) {
    val sandTop = world.viewH * 0.94f
    val sand = v2Tint(world, Color(0xFFC2A878))
    drawRect(sand, Offset(0f, sandTop), Size(world.viewW, world.viewH - sandTop))
    drawRect(
        v2Tint(world, Color(0xFF8D7B57)),
        Offset(0f, sandTop),
        Size(world.viewW, world.viewH * 0.008f)
    )

    val period = world.viewW * 0.22f
    val scroll = world.getWorldScroll()
    val startIdx = floor(scroll / period).toInt() - 1
    val endIdx = ((scroll + world.viewW) / period).toInt() + 1
    for (n in startIdx..endIdx) {
        val px = n * period - scroll + period * 0.4f
        if (px < -period || px > world.viewW + period) continue
        val h1 = (abs(n * 73856093) % 1000) / 1000f
        val h2 = (abs(n * 19349663) % 1000) / 1000f
        val h = world.viewH * (0.015f + h1 * 0.030f)
        val w = world.viewH * (0.030f + h2 * 0.040f)
        val rockColor = v2Tint(world, lerp(Color(0xFF6D6D6D), Color(0xFF4E4E4E), h1))
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

// ============================================================================
//  PLAYER SHIP (oil tanker)
// ============================================================================

private fun DrawScope.drawV2Ship(world: V2GameWorld) {
    val t = world.getTime()
    val bob = sin(t * 2.2f) * world.viewH * 0.006f
    val cx = world.shipCX
    val cy = world.shipCY + bob
    val w = world.shipW
    val h = world.shipH

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
    drawPath(hull, v2Tint(world, Color(0xFFB71C1C)))
    drawPath(hull, v2Tint(world, Color(0xFF6D0F0F)), style = Stroke(width = h * 0.055f))

    drawRect(
        v2Tint(world, Color(0xFF212121)),
        Offset(cx - w * 0.50f, cy + h * 0.20f),
        Size(w * 0.94f, h * 0.07f)
    )
    drawRect(
        v2Tint(world, Color(0xFF7F1010)),
        Offset(cx - w * 0.50f, cy - h * 0.21f),
        Size(w * 0.86f, h * 0.06f)
    )

    for (i in 0..2) {
        val tx = cx - w * 0.36f + i * w * 0.235f
        val ty = cy - h * 0.50f
        val tw = w * 0.17f
        val th = h * 0.30f
        drawRoundRect(
            v2Tint(world, Color(0xFF9E9E9E)),
            Offset(tx, ty),
            Size(tw, th),
            CornerRadius(th * 0.30f)
        )
        drawRect(
            v2Tint(world, Color(0xFF616161)),
            Offset(tx, ty + th * 0.36f),
            Size(tw, th * 0.14f)
        )
    }

    drawRoundRect(
        v2Tint(world, Color(0xFFECEFF1)),
        Offset(cx + w * 0.12f, cy - h * 0.44f),
        Size(w * 0.17f, h * 0.26f),
        CornerRadius(h * 0.04f)
    )
    drawRect(
        v2Tint(world, Color(0xFF42A5F5)),
        Offset(cx + w * 0.15f, cy - h * 0.38f),
        Size(w * 0.10f, h * 0.08f)
    )

    drawRect(
        v2Tint(world, Color(0xFF424242)),
        Offset(cx - w * 0.47f, cy - h * 0.64f),
        Size(w * 0.085f, h * 0.48f)
    )
    drawRect(
        v2Tint(world, Color(0xFF212121)),
        Offset(cx - w * 0.485f, cy - h * 0.64f),
        Size(w * 0.115f, h * 0.075f)
    )

    if (world.ship.shield > 0f) {
        val pulse = 0.5f + 0.5f * sin(t * 8f)
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

    if (world.ship.magnet > 0f) {
        val pulse = 0.5f + 0.5f * sin(t * 5f)
        drawCircle(
            Color(0x33AB47BC).copy(alpha = 0.25f + pulse * 0.15f),
            w * 1.0f,
            Offset(cx, cy)
        )
    }

    if (world.ship.turbo > 0f) {
        for (i in 0 until 5) {
            val a = (1f - i / 5f) * 0.8f
            drawCircle(
                Color(0xFFFF7043).copy(alpha = a * 0.6f),
                w * 0.05f * (1f + i * 0.3f),
                Offset(cx - w * 0.5f - i * world.viewW * 0.02f, cy + h * 0.15f)
            )
        }
    }
}

// ============================================================================
//  FIGHTER PLANE (F-35)
// ============================================================================

private fun DrawScope.drawV2Plane(world: V2GameWorld) {
    val t = world.getTime()
    val cx = world.planeCX
    val cy = world.planeCY + sin(t * 3f) * world.viewH * 0.004f
    val w = world.planeW
    val h = world.planeH

    drawOval(
        Color(0xFFFF7043).copy(alpha = 0.7f),
        Offset(cx - w * 0.62f, cy - h * 0.10f),
        Size(w * 0.25f, h * 0.20f)
    )
    drawOval(
        Color(0xFFFFEB3B).copy(alpha = 0.9f),
        Offset(cx - w * 0.55f, cy - h * 0.06f),
        Size(w * 0.14f, h * 0.12f)
    )

    val body = Path()
    body.moveTo(cx + w * 0.50f, cy)
    body.lineTo(cx + w * 0.20f, cy - h * 0.22f)
    body.lineTo(cx - w * 0.30f, cy - h * 0.22f)
    body.lineTo(cx - w * 0.52f, cy - h * 0.10f)
    body.lineTo(cx - w * 0.52f, cy + h * 0.10f)
    body.lineTo(cx - w * 0.30f, cy + h * 0.22f)
    body.lineTo(cx + w * 0.20f, cy + h * 0.22f)
    body.close()
    drawPath(body, Color(0xFF546E7A))
    drawPath(body, Color(0xFF263238), style = Stroke(width = h * 0.05f))

    val wing = Path()
    wing.moveTo(cx - w * 0.05f, cy - h * 0.18f)
    wing.lineTo(cx + w * 0.15f, cy - h * 0.60f)
    wing.lineTo(cx - w * 0.20f, cy - h * 0.60f)
    wing.lineTo(cx - w * 0.30f, cy - h * 0.18f)
    wing.close()
    drawPath(wing, Color(0xFF455A64))
    drawPath(wing, Color(0xFF263238), style = Stroke(width = h * 0.04f))

    val wing2 = Path()
    wing2.moveTo(cx - w * 0.05f, cy + h * 0.18f)
    wing2.lineTo(cx + w * 0.15f, cy + h * 0.60f)
    wing2.lineTo(cx - w * 0.20f, cy + h * 0.60f)
    wing2.lineTo(cx - w * 0.30f, cy + h * 0.18f)
    wing2.close()
    drawPath(wing2, Color(0xFF455A64))
    drawPath(wing2, Color(0xFF263238), style = Stroke(width = h * 0.04f))

    drawOval(
        Color(0xFF29B6F6),
        Offset(cx + w * 0.05f, cy - h * 0.15f),
        Size(w * 0.22f, h * 0.16f)
    )
    drawOval(
        Color(0x8829B6F6),
        Offset(cx + w * 0.05f, cy + 0f),
        Size(w * 0.22f, h * 0.16f)
    )

    val tail = Path()
    tail.moveTo(cx - w * 0.42f, cy - h * 0.10f)
    tail.lineTo(cx - w * 0.52f, cy - h * 0.42f)
    tail.lineTo(cx - w * 0.38f, cy - h * 0.42f)
    tail.lineTo(cx - w * 0.32f, cy - h * 0.10f)
    tail.close()
    drawPath(tail, Color(0xFF455A64))

    if (world.plane.invuln > 0f) {
        val blink = (sin(t * 26f) + 1f) * 0.5f
        drawCircle(
            Color(0xFFFFFFFF).copy(alpha = blink * 0.25f),
            w * 0.7f,
            Offset(cx, cy),
            style = Stroke(width = world.viewH * 0.006f)
        )
    }
}

// ============================================================================
//  OBSTACLES
// ============================================================================

private fun DrawScope.drawV2Obstacles(world: V2GameWorld) {
    for (o in world.obstacles) {
        when (o.kind) {
            ObstacleKind.ROCK -> drawV2Rock(world, o)
            ObstacleKind.MINE -> drawV2Mine(world, o)
            ObstacleKind.ICEBERG -> drawV2Iceberg(world, o)
            ObstacleKind.WHALE -> drawV2Whale(world, o)
            ObstacleKind.PIRATE -> drawV2Pirate(world, o)
        }
    }
}

private fun DrawScope.drawV2Rock(world: V2GameWorld, o: V2Obstacle) {
    val r = o.w * 0.5f
    val p = Path()
    val n = 9
    for (i in 0 until n) {
        val a = (i.toFloat() / n) * PI.toFloat() * 2f
        val rr = r * (0.70f + 0.30f * abs(sin(i * 2.3f + o.phase * 0.15f)))
        val px = o.x + cos(a) * rr
        val py = o.y + sin(a) * rr * 0.92f
        if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
    }
    p.close()
    drawPath(p, v2Tint(world, Color(0xFF757575)))
    drawPath(p, v2Tint(world, Color(0xFF424242)), style = Stroke(width = r * 0.10f))
    drawCircle(
        v2Tint(world, Color(0xFF9E9E9E)),
        r * 0.28f,
        Offset(o.x - r * 0.30f, o.y - r * 0.32f)
    )
    drawCircle(Color(0x33FFFFFF), r * 0.14f, Offset(o.x - r * 0.38f, o.y - r * 0.40f))
}

private fun DrawScope.drawV2Mine(world: V2GameWorld, o: V2Obstacle) {
    val r = o.w * 0.5f
    val spike = r * 0.55f
    for (i in 0 until 8) {
        val a = (i.toFloat() / 8f) * PI.toFloat() * 2f + o.phase * 0.4f
        val sx = o.x + cos(a) * r * 0.85f
        val sy = o.y + sin(a) * r * 0.85f
        val ex = o.x + cos(a) * (r * 0.85f + spike)
        val ey = o.y + sin(a) * (r * 0.85f + spike)
        drawLine(
            v2Tint(world, Color(0xFF212121)),
            Offset(sx, sy), Offset(ex, ey),
            strokeWidth = r * 0.20f
        )
    }
    drawCircle(v2Tint(world, Color(0xFF212121)), r * 0.85f, Offset(o.x, o.y))
    drawCircle(v2Tint(world, Color(0xFF616161)), r * 0.45f, Offset(o.x, o.y))
    val blink = 0.5f + 0.5f * sin(world.getTime() * 7f + o.phase)
    drawCircle(
        Color(0xFFFF1744).copy(alpha = 0.35f + blink * 0.65f),
        r * 0.20f,
        Offset(o.x, o.y)
    )
}

private fun DrawScope.drawV2Iceberg(world: V2GameWorld, o: V2Obstacle) {
    val p = Path()
    p.moveTo(o.x, o.y - o.h * 0.50f)
    p.lineTo(o.x + o.w * 0.50f, o.y + o.h * 0.48f)
    p.lineTo(o.x - o.w * 0.50f, o.y + o.h * 0.48f)
    p.close()
    drawPath(p, v2Tint(world, Color(0xFFE3F2FD)))
    drawPath(p, v2Tint(world, Color(0xFF64B5F6)), style = Stroke(width = o.w * 0.035f))
    drawLine(
        v2Tint(world, Color(0xFF90CAF9)),
        Offset(o.x, o.y - o.h * 0.50f),
        Offset(o.x - o.w * 0.16f, o.y + o.h * 0.48f),
        strokeWidth = o.w * 0.022f
    )
    drawLine(
        v2Tint(world, Color(0xFF90CAF9)),
        Offset(o.x, o.y - o.h * 0.50f),
        Offset(o.x + o.w * 0.22f, o.y + o.h * 0.48f),
        strokeWidth = o.w * 0.018f
    )
}

private fun DrawScope.drawV2Whale(world: V2GameWorld, o: V2Obstacle) {
    val w = o.w
    val h = o.h
    val tail = Path()
    tail.moveTo(o.x - w * 0.42f, o.y)
    tail.lineTo(o.x - w * 0.62f, o.y - h * 0.55f)
    tail.lineTo(o.x - w * 0.62f, o.y + h * 0.55f)
    tail.close()
    drawPath(tail, v2Tint(world, Color(0xFF0D3B66)))

    drawOval(
        v2Tint(world, Color(0xFF0D3B66)),
        Offset(o.x - w * 0.50f, o.y - h * 0.50f),
        Size(w, h)
    )
    drawOval(
        v2Tint(world, Color(0xFF4A7FA8)),
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
    drawPath(fin, v2Tint(world, Color(0xFF0A2E52)))
}

private fun DrawScope.drawV2Pirate(world: V2GameWorld, o: V2Obstacle) {
    val w = o.w
    val h = o.h
    val cx = o.x
    val cy = o.y
    val flash = if (o.flash > 0f) Color(0xFFFF5252) else null

    val hull = Path()
    hull.moveTo(cx - w * 0.50f, cy - h * 0.10f)
    hull.lineTo(cx + w * 0.34f, cy - h * 0.10f)
    hull.lineTo(cx + w * 0.48f, cy + h * 0.12f)
    hull.lineTo(cx + w * 0.40f, cy + h * 0.36f)
    hull.lineTo(cx - w * 0.44f, cy + h * 0.36f)
    hull.lineTo(cx - w * 0.52f, cy + h * 0.08f)
    hull.close()
    drawPath(hull, flash ?: v2Tint(world, Color(0xFF5D4037)))
    drawPath(hull, v2Tint(world, Color(0xFF3E2723)), style = Stroke(width = h * 0.045f))

    drawRect(
        flash ?: v2Tint(world, Color(0xFF4E342E)),
        Offset(cx - w * 0.48f, cy - h * 0.16f),
        Size(w * 0.86f, h * 0.07f)
    )

    drawLine(
        v2Tint(world, Color(0xFF3E2723)),
        Offset(cx - w * 0.10f, cy - h * 0.14f),
        Offset(cx - w * 0.10f, cy - h * 0.86f),
        strokeWidth = h * 0.045f
    )
    val sail = Path()
    sail.moveTo(cx - w * 0.06f, cy - h * 0.80f)
    sail.lineTo(cx + w * 0.22f, cy - h * 0.42f)
    sail.lineTo(cx - w * 0.06f, cy - h * 0.30f)
    sail.close()
    drawPath(sail, flash ?: v2Tint(world, Color(0xFFECEFF1)))

    val flag = Path()
    flag.moveTo(cx - w * 0.10f, cy - h * 0.86f)
    flag.lineTo(cx + w * 0.14f, cy - h * 0.78f)
    flag.lineTo(cx - w * 0.10f, cy - h * 0.68f)
    flag.close()
    drawPath(flag, Color(0xFFD32F2F))

    drawRect(
        flash ?: v2Tint(world, Color(0xFF212121)),
        Offset(cx - w * 0.60f, cy - h * 0.02f),
        Size(w * 0.20f, h * 0.10f)
    )
}

// ============================================================================
//  PICKUPS
// ============================================================================

private fun DrawScope.drawV2Pickups(world: V2GameWorld) {
    for (item in world.pickups) {
        val r = item.r
        when (item.kind) {
            PickupKind.BARREL -> {
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
            PickupKind.HEART -> {
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
            PickupKind.SHIELD -> {
                drawCircle(Color(0xFF29B6F6), r, Offset(item.x, item.y))
                drawCircle(
                    Color(0xFF0D47A1), r, Offset(item.x, item.y),
                    style = Stroke(width = r * 0.16f)
                )
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
            PickupKind.MAGNET -> {
                drawCircle(Color(0xFFAB47BC), r, Offset(item.x, item.y))
                drawCircle(
                    Color(0xFF4A148C), r, Offset(item.x, item.y),
                    style = Stroke(width = r * 0.16f)
                )
                drawArc(
                    color = Color.White,
                    startAngle = 0f, sweepAngle = 180f, useCenter = false,
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
            PickupKind.TURBO -> {
                drawCircle(Color(0xFFFF7043), r, Offset(item.x, item.y))
                drawCircle(
                    Color(0xFFBF360C), r, Offset(item.x, item.y),
                    style = Stroke(width = r * 0.16f)
                )
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

// ============================================================================
//  BOSS
// ============================================================================

private fun DrawScope.drawV2Boss(world: V2GameWorld) {
    val b = world.boss ?: return
    when (b.type) {
        BossType.LEVIATHAN -> drawLeviathan(world, b)
        BossType.KRAKEN -> drawKraken(world, b)
    }

    val barW = world.viewW * 0.55f
    val barH = world.viewH * 0.028f
    val barX = (world.viewW - barW) / 2f
    val barY = world.viewH * 0.035f
    drawRoundRect(
        Color(0xAA000000),
        Offset(barX, barY),
        Size(barW, barH),
        CornerRadius(barH * 0.5f)
    )
    val frac = (world.bossHp.toFloat() / world.bossMaxHp.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
    if (frac > 0f) {
        drawRoundRect(
            Color(0xFFE53935),
            Offset(barX, barY),
            Size(barW * frac, barH),
            CornerRadius(barH * 0.5f)
        )
    }
    drawRoundRect(
        Color(0x55FFFFFF),
        Offset(barX, barY),
        Size(barW, barH),
        CornerRadius(barH * 0.5f),
        style = Stroke(width = world.viewH * 0.003f)
    )
}

private fun DrawScope.drawLeviathan(world: V2GameWorld, b: V2Boss) {
    val w = b.w
    val h = b.h
    val cx = b.x
    val cy = b.y
    val flash = if (b.flash > 0f) Color(0xFFFF5252) else null

    val body = Path()
    body.moveTo(cx - w * 0.50f, cy)
    body.cubicTo(
        cx - w * 0.48f, cy - h * 0.70f,
        cx + w * 0.20f, cy - h * 0.70f,
        cx + w * 0.42f, cy - h * 0.18f
    )
    body.cubicTo(
        cx + w * 0.55f, cy + h * 0.10f,
        cx + w * 0.40f, cy + h * 0.55f,
        cx + w * 0.20f, cy + h * 0.55f
    )
    body.lineTo(cx - w * 0.30f, cy + h * 0.55f)
    body.lineTo(cx - w * 0.50f, cy + h * 0.30f)
    body.close()
    drawPath(body, flash ?: v2Tint(world, Color(0xFF1A237E)))
    drawPath(body, v2Tint(world, Color(0xFF0D1440)), style = Stroke(width = h * 0.035f))

    drawOval(
        v2Tint(world, Color(0xFF3949AB)),
        Offset(cx - w * 0.30f, cy + h * 0.10f),
        Size(w * 0.60f, h * 0.30f)
    )

    val head = Path()
    head.moveTo(cx - w * 0.48f, cy - h * 0.20f)
    head.cubicTo(
        cx - w * 0.75f, cy - h * 0.50f,
        cx - w * 0.75f, cy + h * 0.30f,
        cx - w * 0.48f, cy + h * 0.22f
    )
    head.close()
    drawPath(head, flash ?: v2Tint(world, Color(0xFF1A237E)))

    val eyeGlow = 0.7f + 0.3f * sin(world.getTime() * 5f)
    drawCircle(Color(0xFFFF1744).copy(alpha = eyeGlow), h * 0.07f, Offset(cx - w * 0.60f, cy - h * 0.05f))
    drawCircle(Color.White, h * 0.030f, Offset(cx - w * 0.62f, cy - h * 0.05f))

    val fin = Path()
    fin.moveTo(cx - w * 0.05f, cy - h * 0.60f)
    fin.lineTo(cx + w * 0.15f, cy - h * 1.10f)
    fin.lineTo(cx + w * 0.28f, cy - h * 0.60f)
    fin.close()
    drawPath(fin, flash ?: v2Tint(world, Color(0xFF283593)))

    val tail = Path()
    tail.moveTo(cx + w * 0.40f, cy)
    tail.lineTo(cx + w * 0.75f, cy - h * 0.60f)
    tail.lineTo(cx + w * 0.62f, cy + h * 0.10f)
    tail.lineTo(cx + w * 0.75f, cy + h * 0.55f)
    tail.close()
    drawPath(tail, flash ?: v2Tint(world, Color(0xFF1A237E)))

    for (i in 0..3) {
        drawRect(
            v2Tint(world, Color(0xFF00E5FF)).copy(alpha = 0.6f + 0.3f * sin(world.getTime() * 4f + i)),
            Offset(cx - w * 0.20f + i * w * 0.15f, cy - h * 0.15f),
            Size(w * 0.05f, h * 0.04f)
        )
    }
}

private fun DrawScope.drawKraken(world: V2GameWorld, b: V2Boss) {
    val w = b.w
    val h = b.h
    val cx = b.x
    val cy = b.y
    val flash = if (b.flash > 0f) Color(0xFFFF5252) else null

    for (i in 0 until 6) {
        val angle = -0.9f + i * 0.36f
        val baseX = cx
        val baseY = cy + h * 0.20f
        val phase = world.getTime() * 2f + i * 0.7f
        val sway = sin(phase) * h * 0.10f
        val len = h * 0.90f
        val tipX = baseX + cos(angle + PI.toFloat() * 0.5f) * len * 0.5f + sway
        val tipY = baseY + sin(angle + PI.toFloat() * 0.5f) * len

        val tentPath = Path()
        tentPath.moveTo(baseX - w * 0.05f, baseY)
        tentPath.quadraticBezierTo(
            baseX - w * 0.10f + sway * 0.5f, (baseY + tipY) / 2f,
            tipX, tipY
        )
        tentPath.quadraticBezierTo(
            baseX + w * 0.10f + sway * 0.5f, (baseY + tipY) / 2f,
            baseX + w * 0.05f, baseY
        )
        tentPath.close()
        drawPath(tentPath, flash ?: v2Tint(world, Color(0xFF4A148C)))
        drawPath(
            tentPath,
            v2Tint(world, Color(0xFF1A0033)),
            style = Stroke(width = h * 0.020f)
        )

        for (k in 1..3) {
            val tk = k / 4f
            val sx = baseX + (tipX - baseX) * tk
            val sy = baseY + (tipY - baseY) * tk
            drawCircle(
                Color(0xFFE1BEE7).copy(alpha = 0.7f),
                h * 0.018f,
                Offset(sx, sy)
            )
        }
    }

    val head = Path()
    head.moveTo(cx - w * 0.42f, cy + h * 0.20f)
    head.cubicTo(
        cx - w * 0.50f, cy - h * 0.60f,
        cx + w * 0.50f, cy - h * 0.60f,
        cx + w * 0.42f, cy + h * 0.20f
    )
    head.cubicTo(
        cx + w * 0.20f, cy + h * 0.40f,
        cx - w * 0.20f, cy + h * 0.40f,
        cx - w * 0.42f, cy + h * 0.20f
    )
    head.close()
    drawPath(head, flash ?: v2Tint(world, Color(0xFF6A1B9A)))
    drawPath(head, v2Tint(world, Color(0xFF1A0033)), style = Stroke(width = h * 0.030f))

    val eyeGlow = 0.6f + 0.4f * sin(world.getTime() * 6f)
    drawCircle(Color(0xFFFFEB3B).copy(alpha = eyeGlow), h * 0.075f, Offset(cx - w * 0.18f, cy - h * 0.10f))
    drawCircle(Color(0xFFFFEB3B).copy(alpha = eyeGlow), h * 0.075f, Offset(cx + w * 0.18f, cy - h * 0.10f))
    drawCircle(Color(0xFF1A0033), h * 0.030f, Offset(cx - w * 0.18f, cy - h * 0.10f))
    drawCircle(Color(0xFF1A0033), h * 0.030f, Offset(cx + w * 0.18f, cy - h * 0.10f))

    val mouth = Path()
    mouth.moveTo(cx - w * 0.15f, cy + h * 0.15f)
    mouth.quadraticBezierTo(cx, cy + h * 0.30f, cx + w * 0.15f, cy + h * 0.15f)
    drawPath(mouth, Color(0xFF1A0033), style = Stroke(width = h * 0.025f))
}

// ============================================================================
//  SKY ENEMIES
// ============================================================================

private fun DrawScope.drawV2SkyEnemies(world: V2GameWorld) {
    for (e in world.skyEnemies) {
        val cx = e.x
        val cy = e.y
        val w = e.w
        val h = e.h
        val flash = if (e.flash > 0f) Color(0xFFFF5252) else null

        when (e.type) {
            SkyEnemyType.DRONE -> {
                drawCircle(flash ?: Color(0xFF37474F), h * 0.55f, Offset(cx, cy))
                drawCircle(Color(0xFFFF5252).copy(alpha = 0.8f), h * 0.18f, Offset(cx, cy))
                drawLine(
                    Color(0xFF263238), Offset(cx - w * 0.5f, cy - h * 0.3f), Offset(cx - w * 0.5f, cy + h * 0.3f),
                    strokeWidth = h * 0.08f
                )
                drawLine(
                    Color(0xFF263238), Offset(cx + w * 0.5f, cy - h * 0.3f), Offset(cx + w * 0.5f, cy + h * 0.3f),
                    strokeWidth = h * 0.08f
                )
            }
            SkyEnemyType.HELI -> {
                drawOval(
                    flash ?: Color(0xFF546E7A),
                    Offset(cx - w * 0.5f, cy - h * 0.5f),
                    Size(w, h)
                )
                drawLine(
                    Color(0xFF263238),
                    Offset(cx - w * 0.7f, cy - h * 0.55f),
                    Offset(cx + w * 0.7f, cy - h * 0.55f),
                    strokeWidth = h * 0.08f
                )
                drawRect(
                    Color(0xFF37474F),
                    Offset(cx - w * 0.3f, cy + h * 0.3f),
                    Size(w * 0.6f, h * 0.15f)
                )
            }
            SkyEnemyType.JET -> {
                val body = Path()
                body.moveTo(cx - w * 0.55f, cy)
                body.lineTo(cx + w * 0.40f, cy - h * 0.25f)
                body.lineTo(cx + w * 0.55f, cy)
                body.lineTo(cx + w * 0.40f, cy + h * 0.25f)
                body.close()
                drawPath(body, flash ?: Color(0xFF37474F))
                val wing = Path()
                wing.moveTo(cx - w * 0.15f, cy - h * 0.20f)
                wing.lineTo(cx + w * 0.10f, cy - h * 0.70f)
                wing.lineTo(cx - w * 0.25f, cy - h * 0.70f)
                wing.close()
                drawPath(wing, flash ?: Color(0xFF263238))
                val wing2 = Path()
                wing2.moveTo(cx - w * 0.15f, cy + h * 0.20f)
                wing2.lineTo(cx + w * 0.10f, cy + h * 0.70f)
                wing2.lineTo(cx - w * 0.25f, cy + h * 0.70f)
                wing2.close()
                drawPath(wing2, flash ?: Color(0xFF263238))
            }
            SkyEnemyType.BOMBER -> {
                drawOval(
                    flash ?: Color(0xFF455A64),
                    Offset(cx - w * 0.5f, cy - h * 0.4f),
                    Size(w, h * 0.8f)
                )
                drawRect(
                    Color(0xFF37474F),
                    Offset(cx - w * 0.7f, cy - h * 0.12f),
                    Size(w * 0.6f, h * 0.24f)
                )
                drawRect(
                    Color(0xFF37474F),
                    Offset(cx + w * 0.1f, cy - h * 0.12f),
                    Size(w * 0.6f, h * 0.24f)
                )
                drawCircle(Color(0xFFFF5252).copy(alpha = 0.7f), h * 0.10f, Offset(cx, cy))
            }
        }
    }
}

// ============================================================================
//  BULLETS & MISSILES
// ============================================================================

private fun DrawScope.drawV2Bullets(world: V2GameWorld) {
    for (b in world.bullets) {
        if (b.enemy) {
            drawCircle(Color(0xFFFF7043), b.r * 1.4f, Offset(b.x, b.y))
            drawCircle(Color(0xFFD84315), b.r, Offset(b.x, b.y))
            drawCircle(Color(0xFFFFE0B2), b.r * 0.42f, Offset(b.x, b.y))
        } else {
            drawCircle(Color(0x66FFEB3B), b.r * 1.8f, Offset(b.x, b.y))
            drawCircle(Color(0xFFFFC107), b.r, Offset(b.x, b.y))
            drawCircle(Color.White, b.r * 0.40f, Offset(b.x, b.y))
        }
    }
}

private fun DrawScope.drawV2Missiles(world: V2GameWorld) {
    for (m in world.missiles) {
        drawOval(
            Color(0xFFFF5722).copy(alpha = 0.6f),
            Offset(m.x - m.r * 3f, m.y - m.r),
            Size(m.r * 4f, m.r * 2f)
        )
        drawCircle(Color(0xFFFFEB3B), m.r * 0.7f, Offset(m.x, m.y))
        drawCircle(Color.White, m.r * 0.35f, Offset(m.x, m.y))
        for (i in 1..3) {
            drawCircle(
                Color(0xFFFFC107).copy(alpha = 0.5f / i),
                m.r * (0.6f - i * 0.15f),
                Offset(m.x - m.r * i * 1.5f, m.y)
            )
        }
    }
}

// ============================================================================
//  PARTICLES
// ============================================================================

private fun DrawScope.drawV2Particles(world: V2GameWorld) {
    for (p in world.particles) {
        val a = (p.life / p.maxLife).coerceIn(0f, 1f)
        val baseColor = Color(p.color)
        drawCircle(
            baseColor.copy(alpha = a * baseColor.alpha),
            p.size * (0.4f + a * 0.9f),
            Offset(p.x, p.y)
        )
    }
}

// ============================================================================
//  WEATHER
// ============================================================================

private fun DrawScope.drawV2Weather(world: V2GameWorld) {
    val storm = world.getStormLevel()
    val t = world.getTime()
    if (storm > 0.02f) {
        val count = (80 * storm).toInt()
        for (k in 0 until count) {
            val seedX = ((k * 1234567) % 10000) / 10000f * world.viewW
            val speed = 0.7f + ((k * 7654321) % 1000) / 1000f * 0.9f
            val fallRange = world.viewH + world.viewH * 0.2f
            val y = ((((k * 98765) % 1000) / 1000f * fallRange) + t * speed * world.viewH * 1.5f) % fallRange - world.viewH * 0.1f
            var x = (seedX - t * world.viewH * 0.30f) % world.viewW
            if (x < 0f) x += world.viewW
            drawLine(
                Color(0x66B3E5FC),
                Offset(x, y),
                Offset(x - world.viewH * 0.022f, y + world.viewH * 0.055f),
                strokeWidth = world.viewH * 0.0035f
            )
        }
    }
    val flash = world.getWhiteFlash()
    if (flash > 0f) {
        drawRect(
            Color.White.copy(alpha = (flash * 0.55f).coerceIn(0f, 0.55f)),
            Offset.Zero,
            Size(world.viewW, world.viewH)
        )
    }
}

// ============================================================================
//  SCREEN EFFECTS
// ============================================================================

private fun DrawScope.drawV2ScreenEffects(world: V2GameWorld) {
    val red = world.getRedFlash()
    if (red > 0f) {
        drawRect(
            Color(0xFFFF1744).copy(alpha = (red * 0.35f).coerceIn(0f, 0.35f)),
            Offset.Zero,
            Size(world.viewW, world.viewH)
        )
    }
    val inv = if (world.fighterMode) world.plane.invuln else world.ship.invuln
    if (inv > 0f && !world.isOver()) {
        val blink = (sin(world.getTime() * 26f) + 1f) * 0.5f
        drawRect(
            Color(0x33FFFFFF).copy(alpha = blink * 0.12f),
            Offset.Zero,
            Size(world.viewW, world.viewH)
        )
    }
}
