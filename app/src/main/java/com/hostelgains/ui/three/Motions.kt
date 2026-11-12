package com.hostelgains.ui.three

import androidx.compose.ui.graphics.Color
import com.hostelgains.ui.theme.FrameMetal
import com.hostelgains.ui.theme.Rubber
import com.hostelgains.ui.theme.Steel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class Tempo { CONCENTRIC_FIRST, ECCENTRIC_FIRST, ISOMETRIC, CYCLIC }

/**
 * Every animated movement. [center], [halfHeight] and [radius] frame the camera so the
 * athlete and equipment always fit, on any phone size.
 */
enum class MotionKind(
    val cycleSeconds: Float,
    val tempo: Tempo,
    val center: Vec3,
    val halfHeight: Float,
    val radius: Float
) {
    LAT_PULLDOWN_WIDE(3.4f, Tempo.CONCENTRIC_FIRST, v(0, 118, -10), 122f, 82f),
    LAT_PULLDOWN_NEUTRAL(3.4f, Tempo.CONCENTRIC_FIRST, v(0, 118, -10), 122f, 82f),
    CABLE_ROW(3.4f, Tempo.CONCENTRIC_FIRST, v(0, 92, 28), 104f, 92f),
    REAR_DELT_FLY(3.0f, Tempo.CONCENTRIC_FIRST, v(0, 86, 16), 90f, 82f),
    HAMMER_CURL(3.0f, Tempo.CONCENTRIC_FIRST, v(0, 92, 0), 95f, 55f),
    INCLINE_DB_PRESS(3.2f, Tempo.CONCENTRIC_FIRST, v(0, 70, 0), 80f, 70f),
    DB_LATERAL_RAISE(3.0f, Tempo.CONCENTRIC_FIRST, v(0, 95, 0), 98f, 80f),
    INCLINE_PUSHUP(2.6f, Tempo.ECCENTRIC_FIRST, v(0, 60, 0), 68f, 90f),
    ROPE_PUSHDOWN(2.8f, Tempo.CONCENTRIC_FIRST, v(0, 108, 20), 112f, 60f),
    GOBLET_SQUAT(3.6f, Tempo.ECCENTRIC_FIRST, v(0, 90, 0), 95f, 55f),
    BW_SQUAT(2.4f, Tempo.ECCENTRIC_FIRST, v(0, 90, 5), 95f, 62f),
    LYING_LEG_CURL(3.2f, Tempo.CONCENTRIC_FIRST, v(0, 65, 5), 70f, 100f),
    PLANK(4.0f, Tempo.ISOMETRIC, v(0, 30, -5), 42f, 100f),
    DB_ROW(3.0f, Tempo.CONCENTRIC_FIRST, v(0, 85, -5), 92f, 66f),
    INCLINE_DB_CURL(3.0f, Tempo.CONCENTRIC_FIRST, v(0, 70, -5), 80f, 66f),
    PREACHER_CURL(3.0f, Tempo.CONCENTRIC_FIRST, v(0, 76, 5), 82f, 62f),
    FACE_PULL(2.8f, Tempo.CONCENTRIC_FIRST, v(0, 100, 50), 105f, 90f),
    OH_DB_PRESS(3.0f, Tempo.CONCENTRIC_FIRST, v(0, 105, 0), 108f, 55f),
    CABLE_LATERAL_RAISE(3.0f, Tempo.CONCENTRIC_FIRST, v(-10, 98, -5), 102f, 80f),
    FLAT_DB_PRESS(3.0f, Tempo.CONCENTRIC_FIRST, v(0, 60, 0), 70f, 86f),
    OH_CABLE_TRICEP(3.0f, Tempo.CONCENTRIC_FIRST, v(0, 100, -25), 105f, 82f),
    DIPS(2.8f, Tempo.ECCENTRIC_FIRST, v(0, 105, 0), 110f, 52f),
    WALK(1.1f, Tempo.CYCLIC, v(0, 92, 0), 95f, 55f)
}

/** Resolves an exercise id or display name to its animation. */
fun motionFor(idOrName: String): MotionKind {
    val s = idOrName.lowercase()
    return when {
        "neutral" in s && "pulldown" in s -> MotionKind.LAT_PULLDOWN_NEUTRAL
        "pulldown" in s || "pull-down" in s -> MotionKind.LAT_PULLDOWN_WIDE
        "leg curl" in s || "leg_curl" in s -> MotionKind.LYING_LEG_CURL
        "rear delt" in s || "rear_delt" in s -> MotionKind.REAR_DELT_FLY
        "face pull" in s || "face_pull" in s -> MotionKind.FACE_PULL
        "pushdown" in s -> MotionKind.ROPE_PUSHDOWN
        "tricep" in s && ("overhead" in s || "oh_" in s) -> MotionKind.OH_CABLE_TRICEP
        "push-up" in s || "pushup" in s -> MotionKind.INCLINE_PUSHUP
        "hammer" in s -> MotionKind.HAMMER_CURL
        "preacher" in s -> MotionKind.PREACHER_CURL
        "incline" in s && "curl" in s -> MotionKind.INCLINE_DB_CURL
        "incline" in s && "press" in s -> MotionKind.INCLINE_DB_PRESS
        "flat" in s && "press" in s -> MotionKind.FLAT_DB_PRESS
        ("overhead" in s || "oh_db" in s) && "press" in s -> MotionKind.OH_DB_PRESS
        "cable" in s && "lateral" in s -> MotionKind.CABLE_LATERAL_RAISE
        "lateral" in s -> MotionKind.DB_LATERAL_RAISE
        "goblet" in s -> MotionKind.GOBLET_SQUAT
        "squat" in s -> MotionKind.BW_SQUAT
        "plank" in s -> MotionKind.PLANK
        "cable row" in s || "cable_rows" in s || ("seated" in s && "row" in s) -> MotionKind.CABLE_ROW
        "row" in s -> MotionKind.DB_ROW
        "dip" in s -> MotionKind.DIPS
        "curl" in s -> MotionKind.HAMMER_CURL
        "press" in s -> MotionKind.FLAT_DB_PRESS
        else -> MotionKind.WALK
    }
}

/** Current position inside one repetition. [t] 0 = start position, 1 = end range. */
data class RepPhase(val t: Float, val label: String, val cycle: Float)

fun repPhase(kind: MotionKind, timeSec: Float): RepPhase {
    val raw = timeSec / kind.cycleSeconds
    val c = raw - kotlin.math.floor(raw)
    return when (kind.tempo) {
        Tempo.CONCENTRIC_FIRST -> when {
            c < 0.36f -> RepPhase(easeInOut(c / 0.36f), "Lift", c)
            c < 0.46f -> RepPhase(1f, "Squeeze", c)
            c < 0.90f -> RepPhase(1f - easeInOut((c - 0.46f) / 0.44f), "Lower slowly", c)
            else -> RepPhase(0f, "Reset", c)
        }
        Tempo.ECCENTRIC_FIRST -> when {
            c < 0.44f -> RepPhase(easeInOut(c / 0.44f), "Lower slowly", c)
            c < 0.54f -> RepPhase(1f, "Pause", c)
            c < 0.90f -> RepPhase(1f - easeInOut((c - 0.54f) / 0.36f), "Drive up", c)
            else -> RepPhase(0f, "Lockout", c)
        }
        Tempo.ISOMETRIC -> RepPhase(1f, "Hold & brace", c)
        Tempo.CYCLIC -> RepPhase(c, "Steady pace", c)
    }
}

class MotionFrame(
    /** Athlete centre used to decide which machine parts to ghost. */
    val focus: Vec3,
    val supports: List<Prim>,
    val prims: List<Prim>,
    val shadowCenter: Vec3,
    val shadowRx: Float,
    val shadowRz: Float,
    val floorScroll: Float,
    val phase: RepPhase
)

private class Shadow(val c: Vec3, val rx: Float, val rz: Float)

private val StackPlate = Color(0xFF3A404C)

private fun stand(stance: Float = 11f) =
    Limb(v(-stance, 8f, 0f), Vec3.FWD) to Limb(v(stance, 8f, 0f), Vec3.FWD)

private fun seatedLegs(z: Float = 42f, width: Float = 14f) =
    Limb(v(-width, 8f, z), v(0f, 1f, 1f)) to Limb(v(width, 8f, z), v(0f, 1f, 1f))

private inline fun sides(f: (Float) -> Limb): Pair<Limb, Limb> = f(-1f) to f(1f)

fun buildMotionFrame(
    kind: MotionKind,
    timeSec: Float,
    muscles: Set<Muscle>,
    skin: Color = Color(0xFFD5D9E1),
    cableColor: Color = Color(0xFFCBD1DB)
): MotionFrame {
    val phase = repPhase(kind, timeSec)
    val t = phase.t
    val act = when (kind.tempo) {
        Tempo.ISOMETRIC -> 0.8f + 0.2f * sin(timeSec * 2.4f)
        Tempo.CYCLIC -> 0.6f
        else -> 0.3f + 0.7f * t
    }
    val sb = SceneBuilder(skin, cableColor)
    var scroll = 0f
    val shadow = with(sb) {
        when (kind) {
            MotionKind.LAT_PULLDOWN_WIDE -> latPulldown(t, muscles, act, neutral = false)
            MotionKind.LAT_PULLDOWN_NEUTRAL -> latPulldown(t, muscles, act, neutral = true)
            MotionKind.CABLE_ROW -> cableRow(t, muscles, act)
            MotionKind.REAR_DELT_FLY -> rearDeltFly(t, muscles, act)
            MotionKind.HAMMER_CURL -> hammerCurl(t, muscles, act)
            MotionKind.INCLINE_DB_PRESS -> inclinePress(t, muscles, act)
            MotionKind.DB_LATERAL_RAISE -> lateralRaise(t, muscles, act)
            MotionKind.INCLINE_PUSHUP -> inclinePushup(t, muscles, act)
            MotionKind.ROPE_PUSHDOWN -> ropePushdown(t, muscles, act)
            MotionKind.GOBLET_SQUAT -> squat(t, muscles, act, goblet = true)
            MotionKind.BW_SQUAT -> squat(t, muscles, act, goblet = false)
            MotionKind.LYING_LEG_CURL -> legCurl(t, muscles, act)
            MotionKind.PLANK -> plank(timeSec, muscles, act)
            MotionKind.DB_ROW -> chestSupportedRow(t, muscles, act)
            MotionKind.INCLINE_DB_CURL -> inclineCurl(t, muscles, act)
            MotionKind.PREACHER_CURL -> preacherCurl(t, muscles, act)
            MotionKind.FACE_PULL -> facePull(t, muscles, act)
            MotionKind.OH_DB_PRESS -> overheadPress(t, muscles, act)
            MotionKind.CABLE_LATERAL_RAISE -> cableLateral(t, muscles, act)
            MotionKind.FLAT_DB_PRESS -> flatPress(t, muscles, act)
            MotionKind.OH_CABLE_TRICEP -> overheadTricep(t, muscles, act)
            MotionKind.DIPS -> dips(t, muscles, act)
            MotionKind.WALK -> {
                scroll = -(timeSec / kind.cycleSeconds) * 96f
                walk(phase.cycle, muscles, act)
            }
        }
    }
    val focus = v(shadow.c.x, kind.center.y, shadow.c.z)
    return MotionFrame(focus, sb.supports, sb.prims, shadow.c, shadow.rx, shadow.rz, scroll, phase)
}

// ─────────────────────────────────────────────────────────────────────────────
// Back
// ─────────────────────────────────────────────────────────────────────────────

private fun SceneBuilder.latPulldown(t: Float, m: Set<Muscle>, act: Float, neutral: Boolean): Shadow {
    val torso = Torso(v(0f, 52f, 0f), lerp(-6f, -16f, t))
    val sh = torso.shoulder(1f)
    val grip = if (neutral) 7f else 40f
    val barY = lerp(sh.y + if (neutral) 51f else 48f, sh.y + 2f, t)
    val barZ = lerp(sh.z + 6f, sh.z + 15f, t)
    val (aL, aR) = sides { s ->
        Limb(v(s * grip, barY, barZ), if (neutral) v(s * 0.3f, -1f, 0.2f) else v(s * 1f, -0.8f, -0.3f))
    }
    val (lL, lR) = seatedLegs()
    val b = Pose(torso, aL, aR, lL, lR).solve()
    body(b, m, act)

    val hy = (b.hands[0].y + b.hands[1].y) / 2f
    val hz = (b.hands[0].z + b.hands[1].z) / 2f
    val attach: Vec3
    if (neutral) {
        for (i in 0..1) capsule(b.hands[i] - v(0f, 0f, 5f), b.hands[i] + v(0f, 0f, 5f), 1.8f, 1.8f, Rubber)
        attach = v(0f, hy + 11f, hz)
        capsule(b.hands[0] + v(0f, 0f, 4f), attach, 1.3f, 1.3f, Steel)
        capsule(b.hands[1] + v(0f, 0f, 4f), attach, 1.3f, 1.3f, Steel)
    } else {
        capsule(v(-50f, hy, hz), v(50f, hy, hz), 1.4f, 1.4f, Steel)
        capsule(v(-50f, hy, hz), v(-58f, hy - 7f, hz), 1.4f, 1.4f, Steel)
        capsule(v(50f, hy, hz), v(58f, hy - 7f, hz), 1.4f, 1.4f, Steel)
        attach = v(0f, hy + 1.5f, hz)
    }

    // Machine
    val frontPulley = v(0f, 226f, 6f)
    val rearPulley = v(0f, 226f, -62f)
    pulley(frontPulley, Vec3.RIGHT)
    pulley(rearPulley, Vec3.RIGHT)
    cable(attach, frontPulley - v(0f, 5.5f, 0f), cableColor)
    ghost {
        box(v(0f, 233f, -28f), 4f, 3f, 38f, FrameMetal)
        box(v(0f, 116f, -48f), 5f, 116f, 5f, FrameMetal)
    }
    support { box(v(0f, 2f, -15f), 34f, 2f, 56f, FrameMetal) }
    val lift = t * 30f
    ghost { weightStack(v(0f, 4f, -62f), lift, 218f) }
    cable(rearPulley - v(0f, 5.5f, 0f), v(0f, 4f + 36f + lift, -62f), cableColor)
    support {
        box(v(0f, 39f, 4f), 18f, 3f, 19f, PadColor)
        capsule(v(0f, 36f, 4f), v(0f, 4f, 4f), 3f, 3f, FrameMetal)
    }
    capsule(v(-22f, 62f, 40f), v(22f, 62f, 40f), 5f, 5f, PadColor)
    capsule(v(0f, 62f, 40f), v(0f, 38f, 22f), 2f, 2f, FrameMetal)
    return Shadow(v(0f, 0f, 4f), 30f, 34f)
}

private fun SceneBuilder.cableRow(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val torso = Torso(v(0f, 49f, -15f), lerp(22f, -6f, t))
    val (aL, aR) = sides { s -> Limb(lerp(v(s * 5f, 70f, 42f), v(s * 8f, 72f, 2f), t), v(s * 0.3f, -0.2f, -1f)) }
    val legs = sides { s -> Limb(v(s * 12f, 18f, 54f), v(0f, 1f, 0.3f)) }
    val b = Pose(torso, aL, aR, legs.first, legs.second, footL = v(0f, 12f, 6f), footR = v(0f, 12f, 6f)).solve()
    body(b, m, act)

    // V-handle and cable
    val mid = lerp(b.hands[0], b.hands[1], 0.5f)
    capsule(b.hands[0], b.hands[1], 1.7f, 1.7f, Rubber)
    val apex = mid + v(0f, 0f, 8f)
    capsule(b.hands[0], apex, 1.3f, 1.3f, Steel)
    capsule(b.hands[1], apex, 1.3f, 1.3f, Steel)
    val low = v(0f, 24f, 70f)
    pulley(low, Vec3.RIGHT)
    cable(apex, low - v(0f, 0f, 5.5f), cableColor)
    val lift = t * 26f
    ghost { weightStack(v(0f, 4f, 88f), lift, 188f) }
    cable(low + v(0f, 0f, 5.5f), v(0f, 4f + 36f + lift, 88f), cableColor)
    ghost { box(v(0f, 194f, 88f), 16f, 3f, 8f, FrameMetal) }

    // Bench, footplate, base rail
    support {
        box(v(0f, 2f, 30f), 22f, 2f, 72f, FrameMetal)
        box(v(0f, 18f, -5f), 6f, 15f, 38f, FrameMetal)
        box(v(0f, 36f, -5f), 15f, 3f, 45f, PadColor)
    }
    ghost { box(v(0f, 24f, 61f), 20f, 15f, 2f, FrameMetal) }
    return Shadow(v(0f, 0f, 10f), 30f, 64f)
}

private fun SceneBuilder.chestSupportedRow(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val torso = Torso(v(0f, 88f, -22f), 45f)
    val (aL, aR) = sides { s ->
        val sh = torso.shoulder(s)
        Limb(lerp(sh + v(-s * 2f, -53f, 3f), sh + v(s * 3f, -20f, -14f), t), v(s * 0.3f, 0.4f, -1f))
    }
    val legs = sides { s -> Limb(v(s * 14f, 8f, -50f), Vec3.FWD) }
    val b = Pose(torso, aL, aR, legs.first, legs.second).solve()
    body(b, m, act)
    for (i in 0..1) dumbbell(b.hands[i], Vec3.FWD)

    val padCenter = torso.pelvis + torso.up * 30f + torso.fwd * 14.5f
    ghost { pad(padCenter, torso.up, torso.fwd, 30f, 13f, 3f) }
    capsule(padCenter + torso.fwd * 3f, v(0f, 2f, padCenter.z + 18f), 2.6f, 2.6f, FrameMetal)
    capsule(padCenter + torso.fwd * 3f - torso.up * 22f, v(0f, 2f, padCenter.z - 4f), 2.6f, 2.6f, FrameMetal)
    support { box(v(0f, 1.5f, padCenter.z + 8f), 14f, 1.5f, 22f, FrameMetal) }
    return Shadow(v(0f, 0f, -8f), 30f, 40f)
}

private fun SceneBuilder.facePull(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val torso = Torso(v(0f, 95f, 0f), -4f)
    val (aL, aR) = sides { s ->
        Limb(
            lerp(v(s * 6f, 150f, 47f), v(s * 22f, 160f, 8f), t),
            lerp(v(s * 1f, -0.2f, -0.2f), v(s * 1f, 0.3f, -1f), t)
        )
    }
    val legL = Limb(v(-12f, 8f, 10f), Vec3.FWD)
    val legR = Limb(v(12f, 8f, -10f), Vec3.FWD)
    val b = Pose(torso, aL, aR, legL, legR).solve()
    body(b, m, act)

    val p = v(0f, 160f, 118f)
    val mid = lerp(b.hands[0], b.hands[1], 0.5f)
    val j = mid + (p - mid).normalized() * 14f
    rope(b.hands[0], b.hands[1], j)
    cable(j, p, cableColor)
    pulley(p, Vec3.RIGHT)
    ghost {
        box(v(0f, 160f, 122f), 4f, 3f, 5f, FrameMetal)
        box(v(0f, 100f, 127f), 10f, 100f, 6f, FrameMetal)
        box(v(0f, 28f + t * 24f, 120f), 9f, 12f, 1.5f, StackPlate)
    }
    support { box(v(0f, 2f, 112f), 24f, 2f, 24f, FrameMetal) }
    return Shadow(v(0f, 0f, 0f), 30f, 30f)
}

// ─────────────────────────────────────────────────────────────────────────────
// Shoulders
// ─────────────────────────────────────────────────────────────────────────────

private fun SceneBuilder.rearDeltFly(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val torso = Torso(v(0f, 55f, 0f), 4f)
    val alpha = lerp(-14f, 88f, t)
    val (aL, aR) = sides { s ->
        Limb(torso.shoulder(s) + v(s * sinD(alpha), -0.06f, cosD(alpha)) * 52f, v(s * 0.8f, 0.5f, -0.3f))
    }
    val (lL, lR) = seatedLegs(z = 32f)
    val b = Pose(torso, aL, aR, lL, lR).solve()
    body(b, m, act)

    ghost {
        box(v(0f, 85f, 52f), 8f, 85f, 8f, FrameMetal)
        box(v(0f, 152f, 44f), 12f, 5f, 6f, FrameMetal)
        pad(v(0f, 92f, 20f), Vec3.UP, Vec3.FWD, 18f, 12f, 3f)
        capsule(v(0f, 92f, 23f), v(0f, 92f, 44f), 2.5f, 2.5f, FrameMetal)
    }
    support {
        box(v(0f, 2f, 24f), 26f, 2f, 40f, FrameMetal)
        capsule(v(0f, 38f, 2f), v(0f, 3f, 2f), 3f, 3f, FrameMetal)
        box(v(0f, 41.5f, 2f), 17f, 3f, 17f, PadColor)
    }
    for (i in 0..1) {
        val s = if (i == 0) -1f else 1f
        val pivot = v(s * 6f, 150f, 40f)
        capsule(pivot, b.hands[i] + v(0f, 6f, 0f), 1.8f, 1.8f, Steel)
        capsule(b.hands[i] - v(0f, 5f, 0f), b.hands[i] + v(0f, 6f, 0f), 1.9f, 1.9f, Rubber)
    }
    return Shadow(v(0f, 0f, 14f), 34f, 46f)
}

private fun SceneBuilder.lateralRaise(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val torso = Torso(v(0f, 95f, 0f), 6f)
    val alpha = lerp(8f, 86f, t)
    val (aL, aR) = sides { s ->
        Limb(torso.shoulder(s) + v(s * sinD(alpha), -cosD(alpha), 0.12f) * 53f, v(s * 0.25f, 0.3f, -1f))
    }
    val (lL, lR) = stand()
    val b = Pose(torso, aL, aR, lL, lR).solve()
    body(b, m, act)
    for (i in 0..1) dumbbell(b.hands[i], Vec3.FWD)
    return Shadow(Vec3.ZERO, 30f, 26f)
}

private fun SceneBuilder.cableLateral(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val torso = Torso(v(0f, 95f, 0f), 4f)
    val alpha = lerp(-12f, 82f, t)
    val working = Limb(torso.shoulder(1f) + v(sinD(alpha), -cosD(alpha), 0.1f) * 53f, v(0.3f, 0.3f, -1f))
    val resting = Limb(v(-42f, 104f, -10f), v(-1f, -0.3f, 0f))
    val (lL, lR) = stand()
    val b = Pose(torso, resting, working, lL, lR).solve()
    body(b, m, act)

    val p = v(-42f, 16f, -16f)
    val hand = b.hands[1]
    capsule(hand - v(0f, 0f, 4f), hand + v(0f, 0f, 4f), 1.8f, 1.8f, Rubber)
    cable(p, hand, cableColor)
    pulley(p, Vec3.FWD)
    ghost {
        box(v(-52f, 100f, -16f), 7f, 100f, 9f, FrameMetal)
        box(v(-44.5f, 30f + t * 22f, -16f), 1.5f, 12f, 8f, StackPlate)
    }
    support { box(v(-46f, 2f, -16f), 14f, 2f, 18f, FrameMetal) }
    return Shadow(v(-6f, 0f, -4f), 36f, 28f)
}

private fun SceneBuilder.overheadPress(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val torso = Torso(v(0f, 95f, 0f), 0f)
    val (aL, aR) = sides { s ->
        val sh = torso.shoulder(s)
        Limb(lerp(sh + v(s * 11f, 10f, 4f), sh + v(-s * 6f, 52f, 0f), t), v(s * 1f, -0.6f, 0.15f))
    }
    val (lL, lR) = stand(13f)
    val b = Pose(torso, aL, aR, lL, lR).solve()
    body(b, m, act)
    for (i in 0..1) dumbbell(b.hands[i], Vec3.RIGHT)
    return Shadow(Vec3.ZERO, 30f, 26f)
}

// ─────────────────────────────────────────────────────────────────────────────
// Chest
// ─────────────────────────────────────────────────────────────────────────────

private fun SceneBuilder.inclinePress(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val torso = Torso(v(0f, 55f, 0f), -60f)
    val (aL, aR) = sides { s ->
        val sh = torso.shoulder(s)
        val bottom = sh + torso.fwd * 18f + torso.right * (s * 8f) - torso.up * 4f
        val top = sh + torso.fwd * 52f - torso.right * (s * 9f)
        Limb(lerp(bottom, top, t), torso.right * s - torso.fwd * 0.9f)
    }
    val legs = sides { s -> Limb(v(s * 18f, 8f, 38f), v(0f, 1f, 0.5f)) }
    val b = Pose(torso, aL, aR, legs.first, legs.second).solve()
    body(b, m, act)
    for (i in 0..1) dumbbell(b.hands[i], Vec3.RIGHT)

    val back = torso.pelvis + torso.up * 30f - torso.fwd * 13.5f
    pad(back, torso.up, torso.fwd, 34f, 14f, 3f)
    capsule(back - torso.fwd * 4f, v(0f, 2f, back.z), 2.6f, 2.6f, FrameMetal)
    support {
        box(v(0f, 1.5f, -12f), 14f, 1.5f, 34f, FrameMetal)
        capsule(v(0f, 38f, 6f), v(0f, 2f, 6f), 2.6f, 2.6f, FrameMetal)
        box(v(0f, 41.5f, 6f), 15f, 3f, 15f, PadColor)
    }
    return Shadow(v(0f, 0f, -8f), 32f, 50f)
}

private fun SceneBuilder.flatPress(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val torso = Torso(v(0f, 56.5f, 25f), -90f)
    val (aL, aR) = sides { s ->
        val sh = torso.shoulder(s)
        val bottom = sh + torso.fwd * 16f + torso.right * (s * 10f) - torso.up * 4f
        val top = sh + torso.fwd * 52f - torso.right * (s * 6f) - torso.up * 3f
        Limb(lerp(bottom, top, t), torso.right * s + v(0f, -0.7f, 0f))
    }
    val legs = sides { s -> Limb(v(s * 24f, 8f, 64f), v(s * 0.2f, 1f, 0.4f)) }
    val b = Pose(torso, aL, aR, legs.first, legs.second).solve()
    body(b, m, act)
    for (i in 0..1) dumbbell(b.hands[i], Vec3.RIGHT)
    flatBench(v(0f, 46f, -10f), 58f)
    return Shadow(v(0f, 0f, 0f), 30f, 80f)
}

private fun SceneBuilder.inclinePushup(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val ankleMid = v(0f, 9f, -70f)
    fun torsoAt(beta: Float) = Torso(ankleMid + v(0f, sinD(beta), cosD(beta)) * 85f, 90f - beta)
    val top = torsoAt(45f)
    val torso = torsoAt(lerp(45f, 27f, t))
    // Hands sit between the top and bottom shoulder positions so the chest lowers onto them.
    val hands = arrayOf(top.shoulder(-1f) + v(-3f, -52f, 14f), top.shoulder(1f) + v(3f, -52f, 14f))
    val aL = Limb(hands[0], v(-0.7f, 0.4f, -1f))
    val aR = Limb(hands[1], v(0.7f, 0.4f, -1f))
    val legs = sides { s -> Limb(v(s * 10f, 9f, -70f), torso.fwd) }
    val b = Pose(torso, aL, aR, legs.first, legs.second, footL = v(0f, -6f, 9f), footR = v(0f, -6f, 9f)).solve()
    body(b, m, act)
    flatBench(v(0f, hands[0].y - 4f, hands[0].z + 4f), 16f, 32f)
    return Shadow(v(0f, 0f, -10f), 28f, 70f)
}

private fun SceneBuilder.dips(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val pelvis = v(0f, lerp(124f, 92f, t), lerp(-2f, -8f, t))
    val torso = Torso(pelvis, lerp(8f, 20f, t))
    val (aL, aR) = sides { s -> Limb(v(s * 26f, 120.5f, 0f), v(s * 0.15f, 0.3f, -1f)) }
    val legs = sides { s -> Limb(pelvis + v(s * 8f, -62f, -32f), Vec3.FWD) }
    val b = Pose(torso, aL, aR, legs.first, legs.second, footL = v(0f, -9f, 13f), footR = v(0f, -9f, 13f)).solve()
    body(b, m, act)
    for (s in floatArrayOf(-1f, 1f)) {
        capsule(v(s * 26f, 118f, -32f), v(s * 26f, 118f, 32f), 2.3f, 2.3f, Steel)
        ghost {
            capsule(v(s * 26f, 0f, -30f), v(s * 26f, 118f, -30f), 2.6f, 2.6f, FrameMetal)
            capsule(v(s * 26f, 0f, 30f), v(s * 26f, 118f, 30f), 2.6f, 2.6f, FrameMetal)
        }
        support { box(v(s * 26f, 1.5f, 0f), 4f, 1.5f, 36f, FrameMetal) }
    }
    return Shadow(v(0f, 0f, -6f), 30f, 30f)
}

// ─────────────────────────────────────────────────────────────────────────────
// Arms
// ─────────────────────────────────────────────────────────────────────────────

private fun SceneBuilder.hammerCurl(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val torso = Torso(v(0f, 95f, 0f), 2f)
    val phi = lerp(8f, 132f, t)
    val fore = v(0f, -cosD(phi), sinD(phi))
    val (aL, aR) = sides { s ->
        val elbow = torso.shoulder(s) + v(s * 2f, -28.5f, -3f)
        Limb(elbow + fore * Body.FOREARM, Vec3.BACK, mid = elbow)
    }
    val (lL, lR) = stand()
    val b = Pose(torso, aL, aR, lL, lR).solve()
    body(b, m, act)
    for (i in 0..1) dumbbell(b.hands[i], v(0f, sinD(phi), cosD(phi)))
    return Shadow(Vec3.ZERO, 30f, 26f)
}

private fun SceneBuilder.inclineCurl(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val torso = Torso(v(0f, 55f, 0f), -35f)
    val phi = lerp(6f, 128f, t)
    val fore = v(0f, -cosD(phi), sinD(phi))
    val (aL, aR) = sides { s ->
        val elbow = torso.shoulder(s) + v(s * 1.5f, -28.5f, -2f)
        Limb(elbow + fore * Body.FOREARM, Vec3.BACK, mid = elbow)
    }
    val legs = sides { s -> Limb(v(s * 18f, 8f, 38f), v(0f, 1f, 0.5f)) }
    val b = Pose(torso, aL, aR, legs.first, legs.second).solve()
    body(b, m, act)
    for (i in 0..1) dumbbell(b.hands[i], Vec3.RIGHT)

    val back = torso.pelvis + torso.up * 30f - torso.fwd * 13.5f
    pad(back, torso.up, torso.fwd, 32f, 13f, 3f)
    capsule(back - torso.fwd * 4f, v(0f, 2f, back.z), 2.6f, 2.6f, FrameMetal)
    support {
        box(v(0f, 1.5f, -10f), 14f, 1.5f, 30f, FrameMetal)
        capsule(v(0f, 38f, 6f), v(0f, 2f, 6f), 2.6f, 2.6f, FrameMetal)
        box(v(0f, 41.5f, 6f), 15f, 3f, 15f, PadColor)
    }
    return Shadow(v(0f, 0f, -8f), 30f, 44f)
}

private fun SceneBuilder.preacherCurl(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val torso = Torso(v(0f, 58f, -8f), 14f)
    val armDir = v(0f, -0.72f, 0.69f).normalized()
    val a = lerp(48f, 150f, t)
    val fore = v(0f, -cosD(a), sinD(a))
    val elbows = arrayOf(
        torso.shoulder(-1f) + armDir * Body.UPPER_ARM + v(3f, 0f, 0f),
        torso.shoulder(1f) + armDir * Body.UPPER_ARM + v(-3f, 0f, 0f)
    )
    val aL = Limb(elbows[0] + fore * Body.FOREARM, Vec3.BACK, mid = elbows[0])
    val aR = Limb(elbows[1] + fore * Body.FOREARM, Vec3.BACK, mid = elbows[1])
    val legs = sides { s -> Limb(v(s * 16f, 8f, 28f), v(0f, 1f, 0.6f)) }
    val b = Pose(torso, aL, aR, legs.first, legs.second).solve()
    body(b, m, act)
    ezBar(lerp(b.hands[0], b.hands[1], 0.5f))

    val n = v(0f, 0.69f, 0.72f).normalized()
    val upperMid = lerp(v(0f, torso.shoulder(1f).y, torso.shoulder(1f).z), v(0f, elbows[1].y, elbows[1].z), 0.5f)
    val padC = upperMid - n * 8.5f
    pad(padC, armDir, n, 15f, 18f, 3f)
    capsule(padC - armDir * 10f - n * 3f, v(0f, 2f, padC.z - 4f), 2.6f, 2.6f, FrameMetal)
    support {
        box(v(0f, 1.5f, 4f), 16f, 1.5f, 28f, FrameMetal)
        capsule(v(0f, 41f, -8f), v(0f, 2f, -8f), 2.6f, 2.6f, FrameMetal)
        box(v(0f, 44.5f, -8f), 16f, 3f, 15f, PadColor)
    }
    return Shadow(v(0f, 0f, 4f), 30f, 36f)
}

private fun SceneBuilder.ropePushdown(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val torso = Torso(v(0f, 95f, -2f), 10f)
    val phi = lerp(100f, 0f, t)
    val (aL, aR) = sides { s ->
        val elbow = torso.shoulder(s) + v(s * 1f, -28f, -4f)
        val dir = v(s * 0.3f * t, -cosD(phi), sinD(phi)).normalized()
        Limb(elbow + dir * Body.FOREARM, Vec3.BACK, mid = elbow)
    }
    val (lL, lR) = stand(12f)
    val b = Pose(torso, aL, aR, lL, lR).solve()
    body(b, m, act)

    val mid = lerp(b.hands[0], b.hands[1], 0.5f)
    val j = mid + v(0f, 12f, 2f)
    rope(b.hands[0], b.hands[1], j)
    val p = v(0f, 212f, 34f)
    cable(j, p - v(0f, 5.5f, 0f), cableColor)
    pulley(p, Vec3.RIGHT)
    ghost {
        box(v(0f, 214f, 39f), 4f, 3f, 7f, FrameMetal)
        box(v(0f, 107f, 47f), 10f, 107f, 6f, FrameMetal)
        box(v(0f, 30f + t * 24f, 39.5f), 9f, 12f, 1.5f, StackPlate)
    }
    support { box(v(0f, 2f, 32f), 24f, 2f, 24f, FrameMetal) }
    return Shadow(v(0f, 0f, 8f), 30f, 36f)
}

private fun SceneBuilder.overheadTricep(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val torso = Torso(v(0f, 95f, 0f), 20f)
    val a = lerp(-125f, 28f, t)
    val fore = v(0f, cosD(a), sinD(a))
    val (aL, aR) = sides { s ->
        val upper = v(-s * 0.12f, cosD(25f), sinD(25f)).normalized()
        val elbow = torso.shoulder(s) + upper * Body.UPPER_ARM
        Limb(elbow + fore * Body.FOREARM + v(-s * 2f, 0f, 0f), Vec3.BACK, mid = elbow)
    }
    val legL = Limb(v(-12f, 8f, 22f), Vec3.FWD)
    val legR = Limb(v(12f, 8f, -18f), Vec3.FWD)
    val b = Pose(torso, aL, aR, legL, legR).solve()
    body(b, m, act)

    val p = v(0f, 40f, -72f)
    val mid = lerp(b.hands[0], b.hands[1], 0.5f)
    val j = mid + (p - mid).normalized() * 14f
    rope(b.hands[0], b.hands[1], j)
    cable(j, p, cableColor)
    pulley(p, Vec3.RIGHT)
    ghost {
        box(v(0f, 100f, -82f), 10f, 100f, 6f, FrameMetal)
        box(v(0f, 28f + t * 22f, -75f), 9f, 12f, 1.5f, StackPlate)
    }
    support { box(v(0f, 2f, -72f), 24f, 2f, 24f, FrameMetal) }
    return Shadow(v(0f, 0f, 2f), 30f, 34f)
}

// ─────────────────────────────────────────────────────────────────────────────
// Legs & core
// ─────────────────────────────────────────────────────────────────────────────

private fun SceneBuilder.squat(t: Float, m: Set<Muscle>, act: Float, goblet: Boolean): Shadow {
    val pelvis = v(0f, lerp(95f, 54f, t), lerp(0f, -20f, t))
    val torso = Torso(pelvis, lerp(4f, if (goblet) 30f else 38f, t))
    val (aL, aR) = sides { s ->
        if (goblet) {
            Limb(torso.neckBase - torso.up * 15f + torso.fwd * 20f + v(s * 5f, 0f, 0f), v(s * 0.6f, -1f, 0f))
        } else {
            Limb(torso.shoulder(s) + v(-s * 4f, 0f, 52f), v(s * 0.3f, -1f, 0f))
        }
    }
    val legs = sides { s -> Limb(v(s * 16f, 8f, 0f), v(s * 0.4f, 0f, 1f)) }
    val b = Pose(torso, aL, aR, legs.first, legs.second, footL = v(-4f, -5f, 16f), footR = v(4f, -5f, 16f)).solve()
    body(b, m, act)
    if (goblet) {
        dumbbell(lerp(b.hands[0], b.hands[1], 0.5f) + v(0f, 3f, 2f), Vec3.UP, handle = 10f, headR = 7f, headLen = 7f)
    }
    return Shadow(v(0f, 0f, -6f), 32f, 34f)
}

private fun SceneBuilder.legCurl(t: Float, m: Set<Muscle>, act: Float): Shadow {
    val torso = Torso(v(0f, 71f, 0f), 86f)
    val (aL, aR) = sides { s -> Limb(v(s * 20f, 52f, 60f), v(s * 1f, 0.3f, 0f)) }
    val phi = lerp(4f, 122f, t)
    val shinDir = v(0f, sinD(phi), -cosD(phi))
    val knees = arrayOf(torso.hip(-1f) + v(0f, -1f, -44f), torso.hip(1f) + v(0f, -1f, -44f))
    val lL = Limb(knees[0] + shinDir * Body.SHIN, Vec3.DOWN, mid = knees[0])
    val lR = Limb(knees[1] + shinDir * Body.SHIN, Vec3.DOWN, mid = knees[1])
    val foot = v(0f, -cosD(phi), -sinD(phi)) * 15f
    val b = Pose(torso, aL, aR, lL, lR, footL = foot, footR = foot).solve()
    body(b, m, act)

    support {
        box(v(0f, 1.5f, -34f), 18f, 1.5f, 4f, FrameMetal)
        box(v(0f, 1.5f, 62f), 18f, 1.5f, 4f, FrameMetal)
        capsule(v(0f, 54f, -34f), v(0f, 2f, -34f), 2.8f, 2.8f, FrameMetal)
        capsule(v(0f, 54f, 62f), v(0f, 2f, 62f), 2.8f, 2.8f, FrameMetal)
        box(v(0f, 57f, 14f), 16f, 3f, 56f, PadColor)
    }
    capsule(v(-24f, 52f, 60f), v(24f, 52f, 60f), 1.6f, 1.6f, Steel)

    val roller = knees[1].copy(x = 0f) + shinDir * 37f + v(0f, cosD(phi), sinD(phi)) * 7f
    capsule(roller - v(16f, 0f, 0f), roller + v(16f, 0f, 0f), 5f, 5f, PadColor)
    for (s in floatArrayOf(-1f, 1f)) {
        val pivot = v(s * 19f, 62f, -46f)
        ball(pivot, 3f, FrameMetal)
        capsule(pivot, roller + v(s * 17f, 0f, 0f), 1.8f, 1.8f, FrameMetal)
    }
    return Shadow(v(0f, 0f, 8f), 28f, 80f)
}

private fun SceneBuilder.plank(time: Float, m: Set<Muscle>, act: Float): Shadow {
    val breathe = sin(time * 1.6f) * 0.8f
    val torso = Torso(v(0f, 25f + breathe, 0f), 78.5f)
    val (aL, aR) = sides { s -> Limb(v(s * 7f, 5f, 68f), Vec3.DOWN, mid = v(s * 13f, 5f, 45f)) }
    val legs = sides { s -> Limb(v(s * 10f, 8f, -83f), torso.fwd) }
    val b = Pose(torso, aL, aR, legs.first, legs.second, footL = v(0f, -5f, 9f), footR = v(0f, -5f, 9f)).solve()
    body(b, m, act)
    support { box(v(0f, 0.6f, -5f), 34f, 0.6f, 100f, Color(0xFF232833)) }
    return Shadow(v(0f, 0f, -8f), 26f, 80f)
}

private fun SceneBuilder.walk(cycle: Float, m: Set<Muscle>, act: Float): Shadow {
    val twoPi = (2.0 * PI).toFloat()
    val phiR = cycle * twoPi
    val pelvis = v(0f, 93.5f + 1.5f * cos(2f * phiR), 0f)
    val torso = Torso(pelvis, 4f)
    fun legFor(s: Float): Limb {
        val phi = phiR + if (s > 0f) 0f else PI.toFloat()
        return Limb(v(s * 10f, 8f + max(0f, cos(phi)) * 9f, 24f * sin(phi)), Vec3.FWD)
    }
    fun armFor(s: Float): Limb {
        val phi = phiR + if (s > 0f) 0f else PI.toFloat()
        val psi = -20f * sin(phi)
        return Limb(torso.shoulder(s) + v(s * 0.08f, -cosD(psi), sinD(psi)) * 53f, Vec3.BACK)
    }
    val b = Pose(torso, armFor(-1f), armFor(1f), legFor(-1f), legFor(1f)).solve()
    body(b, m, act)
    return Shadow(Vec3.ZERO, 30f, 34f)
}
