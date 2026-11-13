package com.hostelgains.ui.three

import androidx.compose.ui.graphics.Color
import com.hostelgains.ui.theme.FrameMetal
import com.hostelgains.ui.theme.Rubber
import com.hostelgains.ui.theme.RubberFace
import com.hostelgains.ui.theme.Steel
import com.hostelgains.ui.theme.Upholstery

// ─────────────────────────────────────────────────────────────────────────────
// Muscles
// ─────────────────────────────────────────────────────────────────────────────

enum class Muscle { CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS, CORE, GLUTES, QUADS, HAMSTRINGS, CALVES }

/** Maps free-text target muscle names (e.g. "Latissimus Dorsi") to rig regions. */
fun musclesFrom(targets: List<String>): Set<Muscle> {
    val out = mutableSetOf<Muscle>()
    targets.forEach { raw ->
        val s = raw.lowercase()
        if ("lats" in s || "latissimus" in s || s == "lat" || "teres" in s || "rhomboid" in s || "trap" in s || "back" in s) out += Muscle.BACK
        if ("chest" in s || "pec" in s) out += Muscle.CHEST
        if ("delt" in s || "shoulder" in s || "rotator" in s) out += Muscle.SHOULDERS
        if ("bicep" in s || "brachialis" in s) out += Muscle.BICEPS
        if ("brachioradialis" in s || "forearm" in s || "grip" in s) out += Muscle.FOREARMS
        if ("tricep" in s) out += Muscle.TRICEPS
        if ("quad" in s) out += Muscle.QUADS
        if ("glute" in s) out += Muscle.GLUTES
        if ("hamstring" in s) out += Muscle.HAMSTRINGS
        if ("core" in s || "abdomin" in s || "abs" in s) out += Muscle.CORE
        if ("calf" in s || "calves" in s) out += Muscle.CALVES
    }
    return out
}

// ─────────────────────────────────────────────────────────────────────────────
// Skeleton
// ─────────────────────────────────────────────────────────────────────────────

object Body {
    const val SPINE = 48f
    const val SHOULDER_HALF = 19f
    const val HIP_HALF = 9.5f
    const val UPPER_ARM = 29f
    const val FOREARM = 26f
    const val THIGH = 44f
    const val SHIN = 42f
}

/**
 * Rigid torso frame. pitchDeg = 0 is upright, positive leans forward (toward +Z),
 * 90 is prone (chest facing the floor), -90 is supine (lying on the back).
 */
class Torso(val pelvis: Vec3, pitchDeg: Float) {
    val up = v(0f, cosD(pitchDeg), sinD(pitchDeg))
    val fwd = v(0f, -sinD(pitchDeg), cosD(pitchDeg))
    val right = Vec3.RIGHT
    val neckBase = pelvis + up * Body.SPINE
    val chest = pelvis + up * 34f + fwd * 3f
    val head = neckBase + up * 19f + fwd * 1.5f

    /** s = -1 for the athlete's left (-X), +1 for the right (+X). */
    fun shoulder(s: Float) = neckBase + right * (s * Body.SHOULDER_HALF) - up * 3f
    fun hip(s: Float) = pelvis + right * (s * Body.HIP_HALF) - up * 2f
}

/** IK goal for one limb. Supply [mid] to pin the elbow/knee explicitly instead of solving. */
class Limb(val end: Vec3, val pole: Vec3, val mid: Vec3? = null)

class Pose(
    val torso: Torso,
    val armL: Limb,
    val armR: Limb,
    val legL: Limb,
    val legR: Limb,
    /** Vector from ankle to toe tip. */
    val footL: Vec3 = v(0f, -5f, 17f),
    val footR: Vec3 = v(0f, -5f, 17f)
)

class SolvedBody(
    val torso: Torso,
    val shoulders: Array<Vec3>,
    val elbows: Array<Vec3>,
    val hands: Array<Vec3>,
    val hips: Array<Vec3>,
    val knees: Array<Vec3>,
    val ankles: Array<Vec3>,
    val toes: Array<Vec3>
)

private fun solveLimb(root: Vec3, limb: Limb, upper: Float, lower: Float): Pair<Vec3, Vec3> {
    val pinned = limb.mid
    if (pinned != null) {
        val dir = (limb.end - pinned).normalized()
        return pinned to (pinned + dir * lower)
    }
    return solveTwoBone(root, limb.end, upper, lower, limb.pole)
}

fun Pose.solve(): SolvedBody {
    val sL = torso.shoulder(-1f)
    val sR = torso.shoulder(1f)
    val hL = torso.hip(-1f)
    val hR = torso.hip(1f)
    val (eL, wL) = solveLimb(sL, armL, Body.UPPER_ARM, Body.FOREARM)
    val (eR, wR) = solveLimb(sR, armR, Body.UPPER_ARM, Body.FOREARM)
    val (kL, aL) = solveLimb(hL, legL, Body.THIGH, Body.SHIN)
    val (kR, aR) = solveLimb(hR, legR, Body.THIGH, Body.SHIN)
    return SolvedBody(
        torso = torso,
        shoulders = arrayOf(sL, sR),
        elbows = arrayOf(eL, eR),
        hands = arrayOf(wL, wR),
        hips = arrayOf(hL, hR),
        knees = arrayOf(kL, kR),
        ankles = arrayOf(aL, aR),
        toes = arrayOf(aL + footL, aR + footR)
    )
}

/**
 * Emits the articulated 3D anatomical male mesh (`Male-low.OBJ`).
 * [activation] (0..1) drives how strongly target [muscles] glow on the sculpted facets.
 */
fun SceneBuilder.body(b: SolvedBody, muscles: Set<Muscle>, activation: Float, skin: Color = this.skin) {
    val shoulderGlow = if (Muscle.SHOULDERS in muscles) activation else 0f
    // Internal glenohumeral joint caps ensure zero seam gaps during extreme overhead arm rotations
    ball(b.shoulders[0], 5.6f, skin, glow = shoulderGlow)
    ball(b.shoulders[1], 5.6f, skin, glow = shoulderGlow)
    bodyMesh(b, muscles, activation, skin)
}

// ─────────────────────────────────────────────────────────────────────────────
// Equipment
// ─────────────────────────────────────────────────────────────────────────────

val RopeColor = Color(0xFF5B616E)
val PadColor = Upholstery

fun SceneBuilder.dumbbell(center: Vec3, axis: Vec3, handle: Float = 13f, headR: Float = 5.6f, headLen: Float = 6.5f) {
    val a = axis.normalized()
    val h = handle / 2f
    capsule(center - a * h, center + a * h, 1.5f, 1.5f, Steel)
    cylinder(center + a * h, center + a * (h + headLen), headR, Rubber, RubberFace)
    cylinder(center - a * h, center - a * (h + headLen), headR, Rubber, RubberFace)
}

fun SceneBuilder.ezBar(center: Vec3, plateR: Float = 11f) {
    val x = Vec3.RIGHT
    capsule(center - x * 36f, center + x * 36f, 1.3f, 1.3f, Steel)
    for (s in floatArrayOf(-1f, 1f)) {
        cylinder(center + x * (s * 19f), center + x * (s * 21f), 2.6f, Steel, Steel)
        cylinder(center + x * (s * 22f), center + x * (s * 27f), plateR, Rubber, RubberFace)
    }
}

fun SceneBuilder.rope(handA: Vec3, handB: Vec3, junction: Vec3) {
    capsule(handA, junction, 1.7f, 1.4f, RopeColor)
    capsule(handB, junction, 1.7f, 1.4f, RopeColor)
    ball(handA, 2.8f, Color(0xFF2B2F38))
    ball(handB, 2.8f, Color(0xFF2B2F38))
}

fun SceneBuilder.pulley(center: Vec3, axis: Vec3) {
    val a = axis.normalized()
    cylinder(center - a * 1.6f, center + a * 1.6f, 5.5f, FrameMetal, Steel)
}

/** Selectorized stack between two guide rods. [lift] raises the working plates. */
fun SceneBuilder.weightStack(baseCenter: Vec3, lift: Float, height: Float = 200f) {
    for (s in floatArrayOf(-1f, 1f)) {
        capsule(baseCenter + v(s * 13f, 0f, 0f), baseCenter + v(s * 13f, height, 0f), 1.1f, 1.1f, Steel)
    }
    box(baseCenter + v(0f, 12f, 0f), 11f, 12f, 6f, Rubber)
    box(baseCenter + v(0f, 30f + lift, 0f), 11f, 6f, 6f, Color(0xFF3A404C))
}

/** Flat bench whose padded top surface is centred at [top]. Long axis along Z. */
fun SceneBuilder.flatBench(top: Vec3, halfLength: Float, halfWidth: Float = 14f) = support {
    box(top - v(0f, 3f, 0f), halfWidth, 3f, halfLength, PadColor)
    val legY = top.y - 6f
    for (s in floatArrayOf(-1f, 1f)) {
        val z = top.z + s * (halfLength - 8f)
        capsule(v(top.x, legY, z), v(top.x, 2f, z), 2.6f, 2.6f, FrameMetal)
        box(v(top.x, 1.5f, z), halfWidth * 0.9f, 1.5f, 3f, FrameMetal)
    }
}

/** Upholstered pad oriented along [along] (its long axis) with surface normal [normal]. */
fun SceneBuilder.pad(center: Vec3, along: Vec3, normal: Vec3, halfLength: Float, halfWidth: Float = 14f, thickness: Float = 3f) {
    block(center, v(halfWidth, 0f, 0f), along.normalized() * halfLength, normal.normalized() * thickness, PadColor)
}
