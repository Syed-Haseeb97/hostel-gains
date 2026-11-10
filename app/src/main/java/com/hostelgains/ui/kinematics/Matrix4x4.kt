package com.hostelgains.ui.kinematics

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 4x4 Affine Transformation Matrix for 3D graphics projection and hierarchical skeletal rendering.
 * Stored in row-major order:
 * [ m00, m01, m02, m03 ]
 * [ m10, m11, m12, m13 ]
 * [ m20, m21, m22, m23 ]
 * [ m30, m31, m32, m33 ]
 */
class Matrix4x4(val values: FloatArray = identityArray()) {

    companion object {
        fun identityArray(): FloatArray = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )

        fun identity(): Matrix4x4 = Matrix4x4(identityArray())

        fun translation(tx: Float, ty: Float, tz: Float): Matrix4x4 = Matrix4x4(
            floatArrayOf(
                1f, 0f, 0f, tx,
                0f, 1f, 0f, ty,
                0f, 0f, 1f, tz,
                0f, 0f, 0f, 1f
            )
        )

        fun scale(sx: Float, sy: Float, sz: Float): Matrix4x4 = Matrix4x4(
            floatArrayOf(
                sx, 0f, 0f, 0f,
                0f, sy, 0f, 0f,
                0f, 0f, sz, 0f,
                0f, 0f, 0f, 1f
            )
        )

        fun rotationX(deg: Float): Matrix4x4 {
            val rad = (deg * Math.PI / 180.0).toFloat()
            val c = cos(rad)
            val s = sin(rad)
            return Matrix4x4(
                floatArrayOf(
                    1f, 0f, 0f, 0f,
                    0f, c, -s, 0f,
                    0f, s, c, 0f,
                    0f, 0f, 0f, 1f
                )
            )
        }

        fun rotationY(deg: Float): Matrix4x4 {
            val rad = (deg * Math.PI / 180.0).toFloat()
            val c = cos(rad)
            val s = sin(rad)
            return Matrix4x4(
                floatArrayOf(
                    c, 0f, s, 0f,
                    0f, 1f, 0f, 0f,
                    -s, 0f, c, 0f,
                    0f, 0f, 0f, 1f
                )
            )
        }

        fun rotationZ(deg: Float): Matrix4x4 {
            val rad = (deg * Math.PI / 180.0).toFloat()
            val c = cos(rad)
            val s = sin(rad)
            return Matrix4x4(
                floatArrayOf(
                    c, -s, 0f, 0f,
                    s, c, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f
                )
            )
        }
    }

    /**
     * Multiplies this matrix by another (this * other).
     */
    operator fun times(other: Matrix4x4): Matrix4x4 {
        val a = this.values
        val b = other.values
        val r = FloatArray(16)

        for (row in 0..3) {
            val rIdx = row * 4
            for (col in 0..3) {
                r[rIdx + col] =
                    a[rIdx + 0] * b[0 * 4 + col] +
                            a[rIdx + 1] * b[1 * 4 + col] +
                            a[rIdx + 2] * b[2 * 4 + col] +
                            a[rIdx + 3] * b[3 * 4 + col]
            }
        }
        return Matrix4x4(r)
    }

    /**
     * Transforms a 3D point by this affine matrix.
     */
    fun transformPoint(p: Point3D): Point3D {
        val v = values
        val x = v[0] * p.x + v[1] * p.y + v[2] * p.z + v[3]
        val y = v[4] * p.x + v[5] * p.y + v[6] * p.z + v[7]
        val z = v[8] * p.x + v[9] * p.y + v[10] * p.z + v[11]
        val w = v[12] * p.x + v[13] * p.y + v[14] * p.z + v[15]

        return if (w != 0f && w != 1f) {
            Point3D(x / w, y / w, z / w)
        } else {
            Point3D(x, y, z)
        }
    }
}

/**
 * 3D polygonal quad face for Painter's algorithm depth sorting and flat polygon lighting.
 */
data class PolygonFace3D(
    val v0: Point3D,
    val v1: Point3D,
    val v2: Point3D,
    val v3: Point3D,
    val baseColor: Color,
    val isGlowing: Boolean = false
) {
    /**
     * Average depth (Z coordinate in camera view space) for Painter's depth sorting.
     */
    val averageDepth: Float
        get() = (v0.z + v1.z + v2.z + v3.z) * 0.25f

    /**
     * Computes surface normal and applies directional shading.
     */
    fun computeShadedColor(lightDir: Point3D): Color {
        // Edge vectors
        val ax = v1.x - v0.x
        val ay = v1.y - v0.y
        val az = v1.z - v0.z

        val bx = v2.x - v0.x
        val by = v2.y - v0.y
        val bz = v2.z - v0.z

        // Cross product for surface normal
        var nx = ay * bz - az * by
        var ny = az * bx - ax * bz
        var nz = ax * by - ay * bx
        val len = sqrt(nx * nx + ny * ny + nz * nz)

        if (len > 0.0001f) {
            nx /= len
            ny /= len
            nz /= len
        }

        // Dot product with directional light
        val dot = (nx * lightDir.x + ny * lightDir.y + nz * lightDir.z).coerceIn(-1.0f, 1.0f)
        val diffuse = (dot * 0.5f + 0.5f).coerceIn(0.25f, 1.0f)

        val glowFactor = if (isGlowing) 1.25f else 1.0f

        val r = (baseColor.red * diffuse * glowFactor).coerceIn(0f, 1f)
        val g = (baseColor.green * diffuse * glowFactor).coerceIn(0f, 1f)
        val b = (baseColor.blue * diffuse * glowFactor).coerceIn(0f, 1f)

        return Color(r, g, b, baseColor.alpha)
    }
}
