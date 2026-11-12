package com.hostelgains.ui.three

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Immutable 3D vector in world units (roughly centimetres). Y is up, the athlete faces +Z. */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    operator fun unaryMinus() = Vec3(-x, -y, -z)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun length() = sqrt(x * x + y * y + z * z)
    fun normalized(): Vec3 {
        val l = length()
        return if (l < 1e-5f) this else Vec3(x / l, y / l, z / l)
    }

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val UP = Vec3(0f, 1f, 0f)
        val DOWN = Vec3(0f, -1f, 0f)
        val FWD = Vec3(0f, 0f, 1f)
        val BACK = Vec3(0f, 0f, -1f)
        val RIGHT = Vec3(1f, 0f, 0f)
    }
}

fun v(x: Float, y: Float, z: Float) = Vec3(x, y, z)
fun v(x: Int, y: Int, z: Int) = Vec3(x.toFloat(), y.toFloat(), z.toFloat())

fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
fun lerp(a: Vec3, b: Vec3, t: Float) = Vec3(lerp(a.x, b.x, t), lerp(a.y, b.y, t), lerp(a.z, b.z, t))
fun Float.rad() = this * (PI.toFloat() / 180f)

fun sinD(deg: Float) = sin(deg.rad())
fun cosD(deg: Float) = cos(deg.rad())

/** Smooth 0..1 ease (sine in-out). */
fun easeInOut(t: Float): Float = (0.5f - 0.5f * cos(t.coerceIn(0f, 1f) * PI.toFloat()))

/**
 * Analytic two-bone IK. Returns the middle joint (elbow / knee) and the reachable end point.
 * [pole] is a world direction the middle joint should bend toward.
 */
fun solveTwoBone(root: Vec3, target: Vec3, upper: Float, lower: Float, pole: Vec3): Pair<Vec3, Vec3> {
    val toTarget = target - root
    val rawDist = toTarget.length()
    val dir = if (rawDist < 1e-4f) Vec3.DOWN else toTarget * (1f / rawDist)
    val minReach = kotlin.math.abs(upper - lower) + 0.5f
    val maxReach = upper + lower - 0.05f
    val d = rawDist.coerceIn(minReach, maxReach)
    val along = (upper * upper - lower * lower + d * d) / (2f * d)
    val height = sqrt((upper * upper - along * along).coerceAtLeast(0f))
    var bend = pole - dir * (pole dot dir)
    if (bend.length() < 1e-3f) {
        // Pole is parallel to the limb: pick any perpendicular.
        bend = dir cross (if (kotlin.math.abs(dir.y) < 0.9f) Vec3.UP else Vec3.RIGHT)
    }
    bend = bend.normalized()
    val mid = root + dir * along + bend * height
    val end = root + dir * d
    return mid to end
}
