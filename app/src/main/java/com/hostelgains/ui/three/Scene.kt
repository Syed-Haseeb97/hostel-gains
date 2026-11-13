package com.hostelgains.ui.three

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp as lerpColor
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// Primitives
// ─────────────────────────────────────────────────────────────────────────────

sealed interface Prim {
    val anchor: Vec3
}

/** Articulated 3D anatomical male mesh primitive from `Male-low.OBJ`. */
class BodyMeshPrim(
    val body: SolvedBody,
    val muscles: Set<Muscle>,
    val activation: Float,
    val skin: Color
) : Prim {
    override val anchor: Vec3 get() = body.torso.pelvis + body.torso.up * 22f
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
    val prims = ArrayList<Prim>(64)

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

    fun bodyMesh(body: SolvedBody, muscles: Set<Muscle>, activation: Float, skin: Color) =
        add(BodyMeshPrim(body, muscles, activation, skin))

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
    val target: Vec3,
    val distance: Float,
    val focal: Float,
    val cx: Float,
    val cy: Float
) {
    val cy0 = cosD(yawDeg)
    val sy0 = sinD(yawDeg)
    val cp = cosD(pitchDeg)
    val sp = sinD(pitchDeg)

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

    /** Lambert brightness 0.42..1 for a world normal with a fixed key light from upper-left-front. */
    fun light(normal: Vec3): Float {
        val n = rotate(normal).normalized()
        val d = n dot KEY_LIGHT
        return 0.42f + 0.58f * d.coerceIn(0f, 1f)
    }

    companion object {
        internal val KEY_LIGHT = Vec3(-0.45f, 0.75f, 0.55f).normalized()
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

private val fallbackWorkspace by lazy { MaleBodyMesh.createWorkspace() }
private val fallbackScratchPath by lazy { Path() }
private val ghostPaint by lazy { Paint().apply { alpha = 0.26f } }

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
    shadowRadiusZ: Float,
    scratchPath: Path = fallbackScratchPath
) {
    // Spotlight pool on the floor.
    scratchPath.rewind()
    val segments = 32
    for (i in 0..segments) {
        val a = i / segments.toFloat() * 2f * Math.PI.toFloat()
        val p = cam.project(v(cos(a) * halfExtent * 0.95f, 0f, sin(a) * halfExtent * 0.95f))
        if (i == 0) scratchPath.moveTo(p.x, p.y) else scratchPath.lineTo(p.x, p.y)
    }
    scratchPath.close()
    val c0 = cam.project(Vec3.ZERO)
    val edge = cam.project(v(halfExtent, 0f, 0f))
    val radius = max(20f, (edge - c0).getDistance())
    drawPath(
        scratchPath,
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
    scratchPath.rewind()
    for (i in 0..24) {
        val a = i / 24f * 2f * Math.PI.toFloat()
        val p = cam.project(v(shadowCenter.x + cos(a) * shadowRadiusX, 0.2f, shadowCenter.z + sin(a) * shadowRadiusZ))
        if (i == 0) scratchPath.moveTo(p.x, p.y) else scratchPath.lineTo(p.x, p.y)
    }
    scratchPath.close()
    val sc = cam.project(v(shadowCenter.x, 0f, shadowCenter.z))
    val se = cam.project(v(shadowCenter.x + max(shadowRadiusX, shadowRadiusZ), 0f, shadowCenter.z))
    drawPath(
        scratchPath,
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
fun DrawScope.drawScene(
    prims: List<Prim>,
    cam: OrbitCamera,
    style: StageStyle,
    focus: Vec3? = null,
    compactLod: Boolean = false,
    workspace: MeshRenderWorkspace = fallbackWorkspace,
    scratchPath: Path = fallbackScratchPath
) {
    if (prims.isEmpty()) return
    val sorted = if (prims.size <= 1) prims else prims.sortedByDescending { cam.depth(it.anchor) }
    val focusDepth = focus?.let { cam.depth(it) }
    for (p in sorted) {
        if (p is Ghosted) {
            val inFront = focusDepth != null && cam.depth(p.inner.anchor) < focusDepth - 4f
            if (inFront) {
                drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), ghostPaint) }
                drawPrim(p.inner, cam, style, compactLod, workspace, scratchPath)
                drawIntoCanvas { it.restore() }
            } else {
                drawPrim(p.inner, cam, style, compactLod, workspace, scratchPath)
            }
        } else {
            drawPrim(p, cam, style, compactLod, workspace, scratchPath)
        }
    }
}

private fun DrawScope.drawPrim(
    p: Prim,
    cam: OrbitCamera,
    style: StageStyle,
    compactLod: Boolean,
    workspace: MeshRenderWorkspace,
    scratchPath: Path
) {
    when (p) {
        is BodyMeshPrim -> drawBodyMesh(p, cam, style, compactLod, workspace, scratchPath)
        is Capsule -> drawCapsule(p, cam, style, scratchPath)
        is Ball -> drawBall(p, cam, style)
        is Cylinder -> drawCylinder(p, cam, scratchPath)
        is Block -> drawBlock(p, cam, scratchPath)
        is Cable -> drawLine(
            p.color,
            cam.project(p.a),
            cam.project(p.b),
            strokeWidth = max(1.5f, 1.2f * density),
            cap = StrokeCap.Round
        )
        is Ghosted -> drawPrim(p.inner, cam, style, compactLod, workspace, scratchPath)
    }
}

/**
 * Renders the articulated `Male-low.OBJ` 3D triangle mesh with:
 * - Zero per-frame heap allocations using [workspace] and [scratchPath].
 * - View-space backface culling (skips ~50% of triangles immediately).
 * - In-place primitive quicksort by triangle depth.
 * - 3-point athletic studio lighting (Key light + rim light) and dynamic target-muscle glow.
 */
private fun DrawScope.drawBodyMesh(
    p: BodyMeshPrim,
    cam: OrbitCamera,
    style: StageStyle,
    compactLod: Boolean,
    ws: MeshRenderWorkspace,
    scratchPath: Path
) {
    val mesh = if (compactLod) MaleBodyMesh.compactLod else MaleBodyMesh.highLod
    MaleBodyMesh.poseVertices(p.body, mesh, ws)

    val vCount = mesh.vertexCount
    val tx = cam.target.x
    val ty = cam.target.y
    val tz = cam.target.z
    val cy0 = cam.cy0
    val sy0 = cam.sy0
    val cp = cam.cp
    val sp = cam.sp
    val camDist = cam.distance
    val focal = cam.focal
    val cx = cam.cx
    val cy = cam.cy

    // 1. Transform posed world vertices to view space and project to screen space
    for (i in 0 until vCount) {
        val rx = ws.worldX[i] - tx
        val ry = ws.worldY[i] - ty
        val rz = ws.worldZ[i] - tz

        val x1 = rx * cy0 + rz * sy0
        val z1 = -rx * sy0 + rz * cy0
        val vx = x1
        val vy = ry * cp - z1 * sp
        val vz = ry * sp + z1 * cp

        ws.viewX[i] = vx
        ws.viewY[i] = vy
        ws.viewZ[i] = vz

        val d = max(camDist - vz, 1f)
        val s = focal / d
        ws.screenX[i] = cx + vx * s
        ws.screenY[i] = cy - vy * s
    }

    // 2. Backface culling + view-space normal lighting
    val tCount = mesh.triangleCount
    val triV0 = mesh.triV0
    val triV1 = mesh.triV1
    val triV2 = mesh.triV2
    val triMuscle = mesh.triMuscleId

    val lx = OrbitCamera.KEY_LIGHT.x
    val ly = OrbitCamera.KEY_LIGHT.y
    val lz = OrbitCamera.KEY_LIGHT.z

    var visCount = 0
    for (j in 0 until tCount) {
        val i0 = triV0[j]
        val i1 = triV1[j]
        val i2 = triV2[j]

        val vx0 = ws.viewX[i0]; val vy0 = ws.viewY[i0]; val vz0 = ws.viewZ[i0]
        val ux = ws.viewX[i1] - vx0; val uy = ws.viewY[i1] - vy0; val uz = ws.viewZ[i1] - vz0
        val wx = ws.viewX[i2] - vx0; val wy = ws.viewY[i2] - vy0; val wz = ws.viewZ[i2] - vz0

        val nz = ux * wy - uy * wx
        if (nz <= 0.01f) continue // Back-facing in view space; cull immediately!

        val nx = uy * wz - uz * wy
        val ny = uz * wx - ux * wz
        val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-4f)

        val dotKey = ((nx * lx + ny * ly + nz * lz) / nLen).coerceIn(0f, 1f)
        val rim = (1f - (nz / nLen).coerceIn(0f, 1f)) * 0.16f
        val brightness = (0.36f + 0.60f * dotKey + rim).coerceIn(0.28f, 1.08f)

        val avgViewZ = (vz0 + ws.viewZ[i1] + ws.viewZ[i2]) * 0.33333334f
        ws.visibleIndices[visCount] = j
        // Larger depth = farther from camera (drawn first)
        ws.visibleDepths[visCount] = camDist - avgViewZ
        ws.visibleBrightness[visCount] = brightness
        visCount++
    }

    if (visCount == 0) return

    // 3. Sort visible triangles farthest-first (descending depth) in place
    quickSortByDepthDescending(ws.visibleIndices, ws.visibleDepths, ws.visibleBrightness, 0, visCount - 1)

    // 4. Rasterize triangles using the single reusable scratchPath
    val activeMuscles = p.muscles
    val act = p.activation
    val baseSkin = p.skin
    val accent = style.accent

    for (k in 0 until visCount) {
        val triIdx = ws.visibleIndices[k]
        val i0 = triV0[triIdx]
        val i1 = triV1[triIdx]
        val i2 = triV2[triIdx]

        val x0 = ws.screenX[i0]; val y0 = ws.screenY[i0]
        val x1 = ws.screenX[i1]; val y1 = ws.screenY[i1]
        val x2 = ws.screenX[i2]; val y2 = ws.screenY[i2]

        val mId = triMuscle[triIdx]
        val muscle = MaleBodyMesh.muscleForId(mId)
        val isHighlighted = muscle != null && muscle in activeMuscles && act > 0.01f
        val lit = ws.visibleBrightness[k]

        val baseColor = if (isHighlighted) {
            lerpColor(baseSkin, accent, (0.82f * act).coerceIn(0f, 1f))
        } else {
            baseSkin
        }

        val shadedColor = if (lit <= 1f) {
            baseColor.shade(lit)
        } else {
            baseColor.tint((lit - 1f) * 1.8f)
        }

        scratchPath.rewind()
        scratchPath.moveTo(x0, y0)
        scratchPath.lineTo(x1, y1)
        scratchPath.lineTo(x2, y2)
        scratchPath.close()

        drawPath(scratchPath, shadedColor)
    }
}

private fun quickSortByDepthDescending(
    indices: IntArray,
    depths: FloatArray,
    brightness: FloatArray,
    low: Int,
    high: Int
) {
    var l = low
    var h = high
    while (l < h) {
        if (h - l < 12) {
            // Insertion sort for small partitions
            for (i in (l + 1)..h) {
                val keyIdx = indices[i]
                val keyDepth = depths[i]
                val keyBright = brightness[i]
                var j = i - 1
                while (j >= l && depths[j] < keyDepth) {
                    indices[j + 1] = indices[j]
                    depths[j + 1] = depths[j]
                    brightness[j + 1] = brightness[j]
                    j--
                }
                indices[j + 1] = keyIdx
                depths[j + 1] = keyDepth
                brightness[j + 1] = keyBright
            }
            return
        }

        val pivot = depths[(l + h) ushr 1]
        var i = l
        var j = h
        while (i <= j) {
            while (depths[i] > pivot) i++
            while (depths[j] < pivot) j--
            if (i <= j) {
                val tmpI = indices[i]; indices[i] = indices[j]; indices[j] = tmpI
                val tmpD = depths[i]; depths[i] = depths[j]; depths[j] = tmpD
                val tmpB = brightness[i]; brightness[i] = brightness[j]; brightness[j] = tmpB
                i++
                j--
            }
        }
        if (j - l < h - i) {
            if (l < j) quickSortByDepthDescending(indices, depths, brightness, l, j)
            l = i
        } else {
            if (i < h) quickSortByDepthDescending(indices, depths, brightness, i, h)
            h = j
        }
    }
}

private fun populateTaperedPath(scratch: Path, p1: Offset, r1: Float, p2: Offset, r2: Float): Boolean {
    val d = p2 - p1
    val len = d.getDistance()
    if (len < 0.5f) return false
    val n = Offset(-d.y / len, d.x / len)
    scratch.rewind()
    scratch.moveTo(p1.x + n.x * r1, p1.y + n.y * r1)
    scratch.lineTo(p2.x + n.x * r2, p2.y + n.y * r2)
    scratch.lineTo(p2.x - n.x * r2, p2.y - n.y * r2)
    scratch.lineTo(p1.x - n.x * r1, p1.y - n.y * r1)
    scratch.close()
    return true
}

private fun DrawScope.fillCapsule(p1: Offset, r1: Float, p2: Offset, r2: Float, color: Color, scratch: Path) {
    drawCircle(color, r1, p1)
    drawCircle(color, r2, p2)
    if (populateTaperedPath(scratch, p1, r1, p2, r2)) {
        drawPath(scratch, color)
    }
}

private fun DrawScope.drawCapsule(c: Capsule, cam: OrbitCamera, style: StageStyle, scratch: Path) {
    val p1 = cam.project(c.a)
    val p2 = cam.project(c.b)
    val r1 = max(0.8f, c.ra * cam.scaleAt(cam.depth(c.a)))
    val r2 = max(0.8f, c.rb * cam.scaleAt(cam.depth(c.b)))
    val base = if (c.glow > 0f) lerpColor(c.color, style.accent, 0.78f * c.glow) else c.color

    if (c.glow > 0.05f) {
        val halo = style.accent.copy(alpha = 0.22f * c.glow)
        fillCapsule(p1, r1 * 1.45f, p2, r2 * 1.45f, halo, scratch)
    }
    fillCapsule(p1, r1, p2, r2, base.shade(0.52f), scratch)
    val lo = Offset(-0.16f, -0.2f)
    fillCapsule(p1 + lo * r1, r1 * 0.8f, p2 + lo * r2, r2 * 0.8f, base.shade(0.9f), scratch)
    val hi = Offset(-0.32f, -0.38f)
    fillCapsule(p1 + hi * r1, r1 * 0.34f, p2 + hi * r2, r2 * 0.34f, base.tint(0.35f).copy(alpha = 0.55f), scratch)
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

private fun DrawScope.drawCylinder(cy: Cylinder, cam: OrbitCamera, scratch: Path) {
    val axis = (cy.b - cy.a).normalized()
    val helper = if (kotlin.math.abs(axis.y) < 0.9f) Vec3.UP else Vec3.RIGHT
    val u = (axis cross helper).normalized()
    val w = axis cross u
    val n = 12
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
        scratch.rewind()
        scratch.moveTo(hull[0].x, hull[0].y)
        for (i in 1 until hull.size) scratch.lineTo(hull[i].x, hull[i].y)
        scratch.close()
        val sideLight = cam.light(u * 0.6f + Vec3.UP * 0.4f)
        drawPath(scratch, cy.side.shade(0.75f * sideLight + 0.15f))
    }
    val showB = cam.facesCamera(cy.b, axis)
    val ring = if (showB) ringB else ringA
    val capNormal = if (showB) axis else -axis
    scratch.rewind()
    scratch.moveTo(ring[0].x, ring[0].y)
    for (i in 1 until ring.size) scratch.lineTo(ring[i].x, ring[i].y)
    scratch.close()
    drawPath(scratch, cy.cap.shade(cam.light(capNormal)))
    drawPath(scratch, cy.cap.tint(0.25f).copy(alpha = 0.5f), style = Stroke(width = max(1f, density * 0.8f)))
}

private fun DrawScope.drawBlock(b: Block, cam: OrbitCamera, scratch: Path) {
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
            scratch.rewind()
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
