package com.hostelgains.ui.three

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp as lerpColor
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Primitives
// ─────────────────────────────────────────────────────────────────────────────

sealed interface Prim {
    val anchor: Vec3
}

/** Tapered capsule: limbs, bars, rollers, cables with thickness. */
data class Capsule(
    val a: Vec3,
    val b: Vec3,
    val ra: Float,
    val rb: Float,
    val color: Color,
    val glow: Float = 0f
) : Prim {
    override val anchor: Vec3 get() = lerp(a, b, 0.5f)
}

data class Ball(val c: Vec3, val r: Float, val color: Color, val glow: Float = 0f) : Prim {
    override val anchor: Vec3 get() = c
}

/** Solid cylinder with flat caps: weight plates, dumbbell heads, pulleys. */
data class Cylinder(val a: Vec3, val b: Vec3, val r: Float, val side: Color, val cap: Color) : Prim {
    override val anchor: Vec3 get() = lerp(a, b, 0.5f)
}

/** Oriented box given by centre and three half-extent vectors: benches, pads, machine frames. */
data class Block(val c: Vec3, val ax: Vec3, val ay: Vec3, val az: Vec3, val color: Color) : Prim {
    override val anchor: Vec3 get() = c
}

/** Machine structure that turns translucent when it sits between the camera and the athlete. */
class Ghosted(val inner: Prim) : Prim {
    override val anchor: Vec3 get() = inner.anchor
}

/** Hair-thin steel cable drawn with constant screen width. */
data class Cable(val a: Vec3, val b: Vec3, val color: Color) : Prim {
    override val anchor: Vec3 get() = lerp(a, b, 0.5f)
}

class SceneBuilder(
    /** Mannequin skin tone for the active theme. */
    val skin: Color = Color(0xFFD5D9E1),
    /** Steel cable colour with enough contrast against the active stage. */
    val cableColor: Color = Color(0xFFCBD1DB)
) {
    /** Regular primitives, depth-sorted together. */
    val prims = ArrayList<Prim>(96)

    /** Surfaces the athlete rests on (benches, seats, floor plates). Drawn first, beneath everything. */
    val supports = ArrayList<Prim>(16)

    private var supportMode = false
    private var ghostMode = false

    private fun add(p: Prim) {
        val item = if (ghostMode) Ghosted(p) else p
        if (supportMode) supports += item else prims += item
    }

    /** Machine parts emitted inside [block] fade out when they would hide the athlete. */
    fun ghost(block: SceneBuilder.() -> Unit) {
        val prev = ghostMode
        ghostMode = true
        block()
        ghostMode = prev
    }

    /** Everything emitted inside [block] is treated as a support surface. */
    fun support(block: SceneBuilder.() -> Unit) {
        val prev = supportMode
        supportMode = true
        block()
        supportMode = prev
    }

    fun capsule(a: Vec3, b: Vec3, ra: Float, rb: Float = ra, color: Color, glow: Float = 0f) =
        add(Capsule(a, b, ra, rb, color, glow))

    fun ball(c: Vec3, r: Float, color: Color, glow: Float = 0f) = add(Ball(c, r, color, glow))

    fun cylinder(a: Vec3, b: Vec3, r: Float, side: Color, cap: Color) = add(Cylinder(a, b, r, side, cap))

    /** Axis-aligned box from centre and half sizes. */
    fun box(c: Vec3, hx: Float, hy: Float, hz: Float, color: Color) =
        add(Block(c, v(hx, 0f, 0f), v(0f, hy, 0f), v(0f, 0f, hz), color))

    fun block(c: Vec3, ax: Vec3, ay: Vec3, az: Vec3, color: Color) = add(Block(c, ax, ay, az, color))

    fun cable(a: Vec3, b: Vec3, color: Color) = add(Cable(a, b, color))
}

// ─────────────────────────────────────────────────────────────────────────────
// Camera
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Orbit camera. yaw 0 looks at the athlete's front, positive yaw orbits to their left side.
 * pitch > 0 raises the camera and looks down.
 */
class OrbitCamera(
    yawDeg: Float,
    pitchDeg: Float,
    private val target: Vec3,
    val distance: Float,
    val focal: Float,
    val cx: Float,
    val cy: Float
) {
    private val cy0 = cosD(yawDeg)
    private val sy0 = sinD(yawDeg)
    private val cp = cosD(pitchDeg)
    private val sp = sinD(pitchDeg)

    /** Rotation only (for normals and directions). z grows toward the camera. */
    fun rotate(p: Vec3): Vec3 {
        val x1 = p.x * cy0 + p.z * sy0
        val z1 = -p.x * sy0 + p.z * cy0
        val y2 = p.y * cp - z1 * sp
        val z2 = p.y * sp + z1 * cp
        return Vec3(x1, y2, z2)
    }

    fun toView(p: Vec3): Vec3 = rotate(p - target)

    /** Distance from the camera plane; larger means farther. */
    fun depth(p: Vec3): Float = distance - toView(p).z

    fun scaleAt(depth: Float): Float = focal / max(depth, 1f)

    fun project(p: Vec3): Offset {
        val vp = toView(p)
        val s = scaleAt(distance - vp.z)
        return Offset(cx + vp.x * s, cy - vp.y * s)
    }

    /** True when a surface with world [normal] at world [point] faces the camera. */
    fun facesCamera(point: Vec3, normal: Vec3): Boolean {
        val vp = toView(point)
        val n = rotate(normal)
        val toCam = Vec3(-vp.x, -vp.y, distance - vp.z)
        return (n dot toCam) > 0f
    }

    /** Lambert brightness 0.45..1 for a world normal with a fixed key light from upper-left-front. */
    fun light(normal: Vec3): Float {
        val n = rotate(normal).normalized()
        val d = n dot KEY_LIGHT
        return 0.42f + 0.58f * d.coerceIn(0f, 1f)
    }

    companion object {
        private val KEY_LIGHT = Vec3(-0.45f, 0.75f, 0.55f).normalized()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Renderer
// ─────────────────────────────────────────────────────────────────────────────

class StageStyle(
    val accent: Color,
    val background: Color,
    val gridColor: Color,
    val shadowColor: Color = Color.Black.copy(alpha = 0.38f),
    /** Soft light pool on the floor; white on dark stages, subtle on light ones. */
    val poolColor: Color = Color.White.copy(alpha = 0.07f)
)

private fun Color.shade(f: Float): Color = Color(
    red = (red * f).coerceIn(0f, 1f),
    green = (green * f).coerceIn(0f, 1f),
    blue = (blue * f).coerceIn(0f, 1f),
    alpha = alpha
)

private fun Color.tint(f: Float): Color = lerpColor(this, Color.White, f)

fun DrawScope.drawFloor(
    cam: OrbitCamera,
    style: StageStyle,
    halfExtent: Float,
    step: Float,
    scrollZ: Float,
    shadowCenter: Vec3,
    shadowRadiusX: Float,
    shadowRadiusZ: Float
) {
    // Spotlight pool on the floor.
    val pool = Path()
    val segments = 48
    for (i in 0..segments) {
        val a = i / segments.toFloat() * 2f * Math.PI.toFloat()
        val p = cam.project(v(cos(a) * halfExtent * 0.95f, 0f, sin(a) * halfExtent * 0.95f))
        if (i == 0) pool.moveTo(p.x, p.y) else pool.lineTo(p.x, p.y)
    }
    pool.close()
    val c0 = cam.project(Vec3.ZERO)
    val edge = cam.project(v(halfExtent, 0f, 0f))
    val radius = max(20f, (edge - c0).getDistance())
    drawPath(
        pool,
        brush = Brush.radialGradient(
            colors = listOf(style.poolColor, style.poolColor.copy(alpha = style.poolColor.alpha * 0.3f), Color.Transparent),
            center = c0,
            radius = radius
        )
    )

    // Grid lines that fade toward the edge of the pool.
    var o = -halfExtent
    val shift = ((scrollZ % step) + step) % step
    while (o <= halfExtent + 0.1f) {
        val fade = (1f - kotlin.math.abs(o) / halfExtent).coerceIn(0f, 1f)
        val col = style.gridColor.copy(alpha = style.gridColor.alpha * fade)
        drawLine(col, cam.project(v(o, 0f, -halfExtent)), cam.project(v(o, 0f, halfExtent)), 1f)
        val z = o + shift - step / 2f
        if (z in -halfExtent..halfExtent) {
            val fz = (1f - kotlin.math.abs(z) / halfExtent).coerceIn(0f, 1f)
            drawLine(
                style.gridColor.copy(alpha = style.gridColor.alpha * fz),
                cam.project(v(-halfExtent, 0f, z)),
                cam.project(v(halfExtent, 0f, z)),
                1f
            )
        }
        o += step
    }

    // Contact shadow.
    val shadow = Path()
    for (i in 0..32) {
        val a = i / 32f * 2f * Math.PI.toFloat()
        val p = cam.project(v(shadowCenter.x + cos(a) * shadowRadiusX, 0.2f, shadowCenter.z + sin(a) * shadowRadiusZ))
        if (i == 0) shadow.moveTo(p.x, p.y) else shadow.lineTo(p.x, p.y)
    }
    shadow.close()
    val sc = cam.project(v(shadowCenter.x, 0f, shadowCenter.z))
    val se = cam.project(v(shadowCenter.x + max(shadowRadiusX, shadowRadiusZ), 0f, shadowCenter.z))
    drawPath(
        shadow,
        brush = Brush.radialGradient(
            colors = listOf(style.shadowColor, style.shadowColor.copy(alpha = 0f)),
            center = sc,
            radius = max(10f, (se - sc).getDistance())
        )
    )
}

/**
 * Painter's-algorithm render of every primitive, farthest first.
 * [focus] is the athlete's centre; ghosted machine parts nearer than it are drawn translucent.
 */
fun DrawScope.drawScene(prims: List<Prim>, cam: OrbitCamera, style: StageStyle, focus: Vec3? = null) {
    val sorted = prims.sortedByDescending { cam.depth(it.anchor) }
    val focusDepth = focus?.let { cam.depth(it) }
    for (p in sorted) {
        if (p is Ghosted) {
            val inFront = focusDepth != null && cam.depth(p.inner.anchor) < focusDepth - 4f
            if (inFront) {
                drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), Paint().apply { alpha = 0.26f }) }
                drawPrim(p.inner, cam, style)
                drawIntoCanvas { it.restore() }
            } else {
                drawPrim(p.inner, cam, style)
            }
        } else {
            drawPrim(p, cam, style)
        }
    }
}

private fun DrawScope.drawPrim(p: Prim, cam: OrbitCamera, style: StageStyle) {
    when (p) {
        is Capsule -> drawCapsule(p, cam, style)
        is Ball -> drawBall(p, cam, style)
        is Cylinder -> drawCylinder(p, cam)
        is Block -> drawBlock(p, cam)
        is Cable -> drawLine(
            p.color,
            cam.project(p.a),
            cam.project(p.b),
            strokeWidth = max(1.5f, 1.2f * density),
            cap = StrokeCap.Round
        )
        is Ghosted -> drawPrim(p.inner, cam, style)
    }
}

private fun DrawScope.taperedPath(p1: Offset, r1: Float, p2: Offset, r2: Float): Path? {
    val d = p2 - p1
    val len = d.getDistance()
    if (len < 0.5f) return null
    val n = Offset(-d.y / len, d.x / len)
    val scratch = Path()
    scratch.moveTo(p1.x + n.x * r1, p1.y + n.y * r1)
    scratch.lineTo(p2.x + n.x * r2, p2.y + n.y * r2)
    scratch.lineTo(p2.x - n.x * r2, p2.y - n.y * r2)
    scratch.lineTo(p1.x - n.x * r1, p1.y - n.y * r1)
    scratch.close()
    return scratch
}

private fun DrawScope.fillCapsule(p1: Offset, r1: Float, p2: Offset, r2: Float, color: Color) {
    drawCircle(color, r1, p1)
    drawCircle(color, r2, p2)
    taperedPath(p1, r1, p2, r2)?.let { drawPath(it, color) }
}

private fun DrawScope.drawCapsule(c: Capsule, cam: OrbitCamera, style: StageStyle) {
    val p1 = cam.project(c.a)
    val p2 = cam.project(c.b)
    val r1 = max(0.8f, c.ra * cam.scaleAt(cam.depth(c.a)))
    val r2 = max(0.8f, c.rb * cam.scaleAt(cam.depth(c.b)))
    val base = if (c.glow > 0f) lerpColor(c.color, style.accent, 0.78f * c.glow) else c.color

    if (c.glow > 0.05f) {
        val halo = style.accent.copy(alpha = 0.22f * c.glow)
        fillCapsule(p1, r1 * 1.45f, p2, r2 * 1.45f, halo)
    }
    // Three-tone clay shading: shadow rim, lit body, specular streak toward the key light.
    fillCapsule(p1, r1, p2, r2, base.shade(0.52f))
    val lo = Offset(-0.16f, -0.2f)
    fillCapsule(p1 + lo * r1, r1 * 0.8f, p2 + lo * r2, r2 * 0.8f, base.shade(0.9f))
    val hi = Offset(-0.32f, -0.38f)
    fillCapsule(p1 + hi * r1, r1 * 0.34f, p2 + hi * r2, r2 * 0.34f, base.tint(0.35f).copy(alpha = 0.55f))
}

private fun DrawScope.drawBall(b: Ball, cam: OrbitCamera, style: StageStyle) {
    val p = cam.project(b.c)
    val r = max(1f, b.r * cam.scaleAt(cam.depth(b.c)))
    val base = if (b.glow > 0f) lerpColor(b.color, style.accent, 0.78f * b.glow) else b.color
    if (b.glow > 0.05f) drawCircle(style.accent.copy(alpha = 0.22f * b.glow), r * 1.45f, p)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(base.tint(0.35f), base.shade(0.92f), base.shade(0.5f)),
            center = p + Offset(-0.35f * r, -0.42f * r),
            radius = r * 1.55f
        ),
        radius = r,
        center = p
    )
}

private fun DrawScope.drawCylinder(cy: Cylinder, cam: OrbitCamera) {
    val axis = (cy.b - cy.a).normalized()
    val helper = if (kotlin.math.abs(axis.y) < 0.9f) Vec3.UP else Vec3.RIGHT
    val u = (axis cross helper).normalized()
    val w = axis cross u
    val n = 16
    val ringA = ArrayList<Offset>(n)
    val ringB = ArrayList<Offset>(n)
    for (i in 0 until n) {
        val ang = i / n.toFloat() * 2f * Math.PI.toFloat()
        val off = (u * cos(ang) + w * sin(ang)) * cy.r
        ringA += cam.project(cy.a + off)
        ringB += cam.project(cy.b + off)
    }
    val hull = convexHull(ringA + ringB)
    if (hull.size >= 3) {
        val scratch = Path()
        scratch.moveTo(hull[0].x, hull[0].y)
        for (i in 1 until hull.size) scratch.lineTo(hull[i].x, hull[i].y)
        scratch.close()
        val sideLight = cam.light(u * 0.6f + Vec3.UP * 0.4f)
        drawPath(scratch, cy.side.shade(0.75f * sideLight + 0.15f))
    }
    val showB = cam.facesCamera(cy.b, axis)
    val ring = if (showB) ringB else ringA
    val capNormal = if (showB) axis else -axis
    val capPath = Path()
    capPath.moveTo(ring[0].x, ring[0].y)
    for (i in 1 until ring.size) capPath.lineTo(ring[i].x, ring[i].y)
    capPath.close()
    drawPath(capPath, cy.cap.shade(cam.light(capNormal)))
    drawPath(capPath, cy.cap.tint(0.25f).copy(alpha = 0.5f), style = Stroke(width = max(1f, density * 0.8f)))
}

private fun DrawScope.drawBlock(b: Block, cam: OrbitCamera) {
    val axes = arrayOf(b.ax, b.ay, b.az)
    for (i in 0..2) {
        for (sign in intArrayOf(-1, 1)) {
            val a = axes[i] * sign.toFloat()
            val e1 = axes[(i + 1) % 3]
            val e2 = axes[(i + 2) % 3]
            val center = b.c + a
            val normal = a.normalized()
            if (!cam.facesCamera(center, normal)) continue
            val q0 = cam.project(center + e1 + e2)
            val q1 = cam.project(center + e1 - e2)
            val q2 = cam.project(center - e1 - e2)
            val q3 = cam.project(center - e1 + e2)
            val scratch = Path()
            scratch.moveTo(q0.x, q0.y)
            scratch.lineTo(q1.x, q1.y)
            scratch.lineTo(q2.x, q2.y)
            scratch.lineTo(q3.x, q3.y)
            scratch.close()
            drawPath(scratch, b.color.shade(cam.light(normal)))
            drawPath(scratch, Color.White.copy(alpha = 0.06f), style = Stroke(width = max(1f, density * 0.6f)))
        }
    }
}

private fun cross2(o: Offset, a: Offset, b: Offset): Float = (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)

/** Andrew's monotone chain. */
private fun convexHull(points: List<Offset>): List<Offset> {
    if (points.size < 3) return points
    val pts = points.sortedWith(compareBy<Offset>({ it.x }, { it.y }))
    val lower = ArrayList<Offset>()
    for (p in pts) {
        while (lower.size >= 2 && cross2(lower[lower.size - 2], lower[lower.size - 1], p) <= 0f) lower.removeAt(lower.size - 1)
        lower += p
    }
    val upper = ArrayList<Offset>()
    for (p in pts.asReversed()) {
        while (upper.size >= 2 && cross2(upper[upper.size - 2], upper[upper.size - 1], p) <= 0f) upper.removeAt(upper.size - 1)
        upper += p
    }
    lower.removeAt(lower.size - 1)
    upper.removeAt(upper.size - 1)
    return lower + upper
}

