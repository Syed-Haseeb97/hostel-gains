package com.hostelgains.ui.three

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlin.math.sqrt

/**
 * Articulated 3D anatomical male mesh derived from `Male-low.OBJ` (stored in `assets/models/Male-low.obj`).
 *
 * Mobile-first optimizations:
 * - Provides two automatic Levels of Detail:
 *   - [highLod]: 973 vertices / 1841 triangles for full-screen 3D Form Lab inspection.
 *   - [compactLod]: 514 vertices / 1000 triangles for 92dp inline workout cards & step tracker.
 * - Pre-bound to the 12 anatomical bone segments of [SolvedBody] with per-facet [Muscle] group tags.
 * - Uses flat primitive arrays ([IntArray], [FloatArray]) and [MeshRenderWorkspace] so zero objects
 *   are allocated on the heap during 60/120 Hz Canvas frame rendering.
 */
class LodMeshData(
    val vertexCount: Int,
    val triangleCount: Int,
    val vertexBone: IntArray,
    val localU: FloatArray,
    val localRight: FloatArray,
    val localFwd: FloatArray,
    val triV0: IntArray,
    val triV1: IntArray,
    val triV2: IntArray,
    val triMuscleId: IntArray
)

/**
 * Pre-allocated per-canvas workspace avoiding per-frame allocations during 3D projection & sorting.
 */
class MeshRenderWorkspace(maxVerts: Int, maxTris: Int) {
    val worldX = FloatArray(maxVerts)
    val worldY = FloatArray(maxVerts)
    val worldZ = FloatArray(maxVerts)
    val viewX = FloatArray(maxVerts)
    val viewY = FloatArray(maxVerts)
    val viewZ = FloatArray(maxVerts)
    val screenX = FloatArray(maxVerts)
    val screenY = FloatArray(maxVerts)

    val visibleIndices = IntArray(maxTris)
    val visibleDepths = FloatArray(maxTris)
    val visibleBrightness = FloatArray(maxTris)

    val boneOriginX = FloatArray(12)
    val boneOriginY = FloatArray(12)
    val boneOriginZ = FloatArray(12)
    val boneAlongX = FloatArray(12)
    val boneAlongY = FloatArray(12)
    val boneAlongZ = FloatArray(12)
    val boneRightX = FloatArray(12)
    val boneRightY = FloatArray(12)
    val boneRightZ = FloatArray(12)
    val boneFwdX = FloatArray(12)
    val boneFwdY = FloatArray(12)
    val boneFwdZ = FloatArray(12)
    val boneLength = FloatArray(12)
}

object MaleBodyMesh {

    const val BONE_TORSO = 0
    const val BONE_HEAD = 1
    const val BONE_UPPER_ARM_L = 2
    const val BONE_UPPER_ARM_R = 3
    const val BONE_FOREARM_L = 4
    const val BONE_FOREARM_R = 5
    const val BONE_THIGH_L = 6
    const val BONE_THIGH_R = 7
    const val BONE_SHIN_L = 8
    const val BONE_SHIN_R = 9
    const val BONE_FOOT_L = 10
    const val BONE_FOOT_R = 11

    private val MUSCLE_BY_ID: Array<Muscle?> = arrayOf(
        null,
        Muscle.CHEST,
        Muscle.BACK,
        Muscle.SHOULDERS,
        Muscle.BICEPS,
        Muscle.TRICEPS,
        Muscle.FOREARMS,
        Muscle.CORE,
        Muscle.GLUTES,
        Muscle.QUADS,
        Muscle.HAMSTRINGS,
        Muscle.CALVES
    )

    fun muscleForId(id: Int): Muscle? = MUSCLE_BY_ID.getOrNull(id)

    val highLod: LodMeshData by lazy {
        decodeLod(
            vertexCount = 973,
            triangleCount = 1841,
            vertB64 = HIGH_V_B64,
            triB64 = HIGH_T_B64
        )
    }

    val compactLod: LodMeshData by lazy {
        decodeLod(
            vertexCount = 514,
            triangleCount = 1000,
            vertB64 = COMPACT_V_B64,
            triB64 = COMPACT_T_B64
        )
    }

    fun createWorkspace(): MeshRenderWorkspace = MeshRenderWorkspace(
        maxVerts = 973,
        maxTris = 1841
    )

    /**
     * Populates the 12 bone coordinate frames in [ws] from [body] and transforms all vertices
     * of [mesh] into world space with zero heap allocations.
     */
    fun poseVertices(body: SolvedBody, mesh: LodMeshData, ws: MeshRenderWorkspace) {
        val t = body.torso
        val headTop = t.neckBase + t.up * 29f + t.fwd * 1.5f

        setBoneFrame(ws, BONE_TORSO, t.pelvis, t.neckBase, t.fwd)
        setBoneFrame(ws, BONE_HEAD, t.neckBase, headTop, t.fwd)
        setBoneFrame(ws, BONE_UPPER_ARM_L, body.shoulders[0], body.elbows[0], t.fwd)
        setBoneFrame(ws, BONE_UPPER_ARM_R, body.shoulders[1], body.elbows[1], t.fwd)
        setBoneFrame(ws, BONE_FOREARM_L, body.elbows[0], body.hands[0], t.fwd)
        setBoneFrame(ws, BONE_FOREARM_R, body.elbows[1], body.hands[1], t.fwd)
        setBoneFrame(ws, BONE_THIGH_L, body.hips[0], body.knees[0], t.fwd)
        setBoneFrame(ws, BONE_THIGH_R, body.hips[1], body.knees[1], t.fwd)
        setBoneFrame(ws, BONE_SHIN_L, body.knees[0], body.ankles[0], t.fwd)
        setBoneFrame(ws, BONE_SHIN_R, body.knees[1], body.ankles[1], t.fwd)
        setBoneFrame(ws, BONE_FOOT_L, body.ankles[0], body.toes[0], t.up)
        setBoneFrame(ws, BONE_FOOT_R, body.ankles[1], body.toes[1], t.up)

        val radialScale = 0.88f
        val n = mesh.vertexCount
        val vBone = mesh.vertexBone
        val lU = mesh.localU
        val lR = mesh.localRight
        val lF = mesh.localFwd

        for (i in 0 until n) {
            val b = vBone[i]
            val uDist = lU[i] * ws.boneLength[b]
            val rr = lR[i] * radialScale
            val rf = lF[i] * radialScale

            ws.worldX[i] = ws.boneOriginX[b] + ws.boneAlongX[b] * uDist + ws.boneRightX[b] * rr + ws.boneFwdX[b] * rf
            ws.worldY[i] = ws.boneOriginY[b] + ws.boneAlongY[b] * uDist + ws.boneRightY[b] * rr + ws.boneFwdY[b] * rf
            ws.worldZ[i] = ws.boneOriginZ[b] + ws.boneAlongZ[b] * uDist + ws.boneRightZ[b] * rr + ws.boneFwdZ[b] * rf
        }
    }

    private fun setBoneFrame(ws: MeshRenderWorkspace, idx: Int, a: Vec3, b: Vec3, refFwd: Vec3) {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val dz = b.z - a.z
        val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-4f)
        val ax = dx / len
        val ay = dy / len
        val az = dz / len

        val dot = refFwd.x * ax + refFwd.y * ay + refFwd.z * az
        var fx = refFwd.x - ax * dot
        var fy = refFwd.y - ay * dot
        var fz = refFwd.z - az * dot
        var fLen = sqrt(fx * fx + fy * fy + fz * fz)
        if (fLen < 1e-4f) {
            val hx = if (kotlin.math.abs(ax) < 0.9f) 1f else 0f
            val hy = if (kotlin.math.abs(ax) < 0.9f) 0f else 1f
            fx = -az * hy
            fy = az * hx
            fz = ax * hy - ay * hx
            fLen = sqrt(fx * fx + fy * fy + fz * fz).coerceAtLeast(1e-4f)
        }
        fx /= fLen
        fy /= fLen
        fz /= fLen

        var rx = ay * fz - az * fy
        var ry = az * fx - ax * fz
        var rz = ax * fy - ay * fx
        val rLen = sqrt(rx * rx + ry * ry + rz * rz).coerceAtLeast(1e-4f)
        rx /= rLen
        ry /= rLen
        rz /= rLen

        ws.boneOriginX[idx] = a.x
        ws.boneOriginY[idx] = a.y
        ws.boneOriginZ[idx] = a.z
        ws.boneAlongX[idx] = ax
        ws.boneAlongY[idx] = ay
        ws.boneAlongZ[idx] = az
        ws.boneRightX[idx] = rx
        ws.boneRightY[idx] = ry
        ws.boneRightZ[idx] = rz
        ws.boneFwdX[idx] = fx
        ws.boneFwdY[idx] = fy
        ws.boneFwdZ[idx] = fz
        ws.boneLength[idx] = len
    }

    /**
     * Parses raw Wavefront `.obj` streams from `assets/models/Male-low.obj`.
     */
    fun parseWavefrontObjStream(input: InputStream): Pair<List<Vec3>, List<IntArray>> {
        val verts = ArrayList<Vec3>(1024)
        val tris = ArrayList<IntArray>(2048)
        BufferedReader(InputStreamReader(input, Charsets.UTF_8)).useLines { lines ->
            lines.forEach { raw ->
                val line = raw.trim()
                if (line.startsWith("v ")) {
                    val p = line.split(Regex("\\s+"))
                    if (p.size >= 4) {
                        verts.add(Vec3(p[1].toFloat(), p[2].toFloat(), p[3].toFloat()))
                    }
                } else if (line.startsWith("f ")) {
                    val p = line.split(Regex("\\s+"))
                    if (p.size >= 4) {
                        val idx = IntArray(p.size - 1) { k -> p[k + 1].substringBefore('/').toInt() - 1 }
                        for (k in 1 until idx.size - 1) {
                            tris.add(intArrayOf(idx[0], idx[k], idx[k + 1]))
                        }
                    }
                }
            }
        }
        return verts to tris
    }

    private fun decodeLod(
        vertexCount: Int,
        triangleCount: Int,
        vertB64: String,
        triB64: String
    ): LodMeshData {
        val vBytes = Base64.getDecoder().decode(vertB64)
        val vBuf = ByteBuffer.wrap(vBytes).order(ByteOrder.LITTLE_ENDIAN)
        val vertexBone = IntArray(vertexCount)
        val localU = FloatArray(vertexCount)
        val localRight = FloatArray(vertexCount)
        val localFwd = FloatArray(vertexCount)

        for (i in 0 until vertexCount) {
            vertexBone[i] = vBuf.get().toInt() and 0xFF
            localU[i] = vBuf.short / 4000.0f
            localRight[i] = vBuf.short / 500.0f
            localFwd[i] = vBuf.short / 500.0f
        }

        val tBytes = Base64.getDecoder().decode(triB64)
        val tBuf = ByteBuffer.wrap(tBytes).order(ByteOrder.LITTLE_ENDIAN)
        val triV0 = IntArray(triangleCount)
        val triV1 = IntArray(triangleCount)
        val triV2 = IntArray(triangleCount)
        val triMuscleId = IntArray(triangleCount)

        for (j in 0 until triangleCount) {
            triV0[j] = tBuf.short.toInt() and 0xFFFF
            triV1[j] = tBuf.short.toInt() and 0xFFFF
            triV2[j] = tBuf.short.toInt() and 0xFFFF
            triMuscleId[j] = tBuf.get().toInt() and 0xFF
        }

        return LodMeshData(
            vertexCount = vertexCount,
            triangleCount = triangleCount,
            vertexBone = vertexBone,
            localU = localU,
            localRight = localRight,
            localFwd = localFwd,
            triV0 = triV0,
            triV1 = triV1,
            triV2 = triV2,
            triMuscleId = triMuscleId
        )
    }

    private const val HIGH_V_B64 =
        "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"

    private const val HIGH_T_B64 =
        "AAACAAEAAAkADAALAAsJAAsACgALDQAKAAsACw0ACwAOAAsVABIAFAALEQCoARYACxEAFgCEAQsKAA0ADwALrgEYABEACxkACwAM" +
        "AAsZAAwAGgALCwAZABsACwsAGwAOAAuuARwAGAALqAERABgACyoAKwAuAAAqAC4AAgAAKQCDASMAC4MBggEvAAuDAS8AKQALNAAz" +
        "ADEAADQAMQAyAAAyAAIALgAAMgAuADQAADUALQA2AAAtADUAJAAAjAGLATAAAIwBMAAtAAArADMALgAAPAA7AJMBCT0AQQBAAAk9" +
        "AEAAPwAJQABDAEIACUAAQgA/AAlAAEEARQAJQABFAEQACUUARwBGAAlFAEYARAAJuAFIAEcACbgBRwC5AQm3AUwASAAItwFIALgB" +
        "Ck0ATgBMAAhNAEwAtwEIuQFHAEUACbkBRQC6AQlQALoBRQAJUABFAEEACT0AOgBQAAk9AFAAQQAJUQBGAEcACVEARwBIAAlRAFQA" +
        "UwAKUQBTAFIAClUARgBRAAlVAFEAUgAJVgBVAFIACVUApAFYAAlVAFgAVwAJRABGAFUACUQAVQBXAAlZAFYAUgAJWQBSAFMACloA" +
        "XABbAApaAFsAWQAKWQBbAFYAClMAXQBaAApTAFoAWQAKVABRAEgACk4AXgBUAApOAFQASAAISABMAE4ACE4AXwBgAAhOAGAAXgAK" +
        "XgBdAFMACl4AUwBUAApAAEQAVwAJQABXAEMACWEAQwBXAAliAJ0BZAAJYwBYAKQBCWgAaQB/AQloAH8BnQEJaQBoAGoAAGwAZQBu" +
        "AAlsAG4AbQAJbwBCAGwACW8AbABtAAlCAEMAbAAJbgBlAGYACW4AZgB/AQk/AEIAbwAJkgFvAHQACZIBdABzAAlzAHQAdgAKcwB2" +
        "AHUACncAYAB1AAp5AHgAdgAKeQB2AHQACnkAcQB6AAp5AHoAeAAKbwB5AHQACXcAWgBdAAp+AHUAYAAKfgBgAF8ACLQBfgBfAAi0" +
        "AV8AtQEIcwB1AH8ACnMAfwA8AAp1AH4AfwAIMAAxACoAADYALAAjAAsoACkAIwALXwBOAE0ACF8ATQC2AQgvAIIBHgAAEgCRAZAB" +
        "CxIAkAEUAAuBAYABagAAgQFqAIMAAGoAnAGDAACHAGsAqQEAhwCpAYgAAIYAHACtAQuGAK0BhQAAHACuAa0BC4MAnAGFAAATABAA" +
        "FgALEwAWAIQACxYAiwByAAsWAHIAhAALcgCLAH0ACnIAfQCrAQoXAIoBiwALFwCLAIUBC2kAagCAAQAfAI0AHgAAIgCNABoACxoA" +
        "jQCKAAAaAIoAGQALLwAeAI0AACkALwAjAAs0ADgANwAAXgBgAHcACl4AdwBdAApcAI4AWwAKrAGPAIgACh0AqgGKAQuqAR0AhwAL" +
        "qgGHAIgAC2gAZwCIAAmfAJsDvAADsgG7ALoAApIAqQCwAQaSALABsQAGkgCiAKgABs0ApwDOAAa1AJwDtgAFzADLAL8ABcwAvwCk" +
        "AAWQAMAAdgEEkAB2AZEABLMBdgGUAAWUALMAsgAFdAGmAKkABnQBqQCSAAasAJgAlgAAlgCcAJcAAMQAwwCjAAOxAbAAkQAGkwCg" +
        "AKUABpkArgCYAACwANEAuAAGsAC4AK8ABtEAzwCzAAbRALMAuAAGkgCgAJMABpcAoQCSAAZyAZUAcwEGowC+AEkBA6MASQHEAAO/" +
        "AL4AowAFvwCjAKQABMoAvwDLAATNAHgBpwAGvwDBAL4ABLUAtADHAAWdAMgAyQAEygCQAJ0ABMoAnQCeAATPAKoAvQAGuQCqAM8A" +
        "BrkAzwDQAAawALkA0AAGUADTAJ4BCVAAngFPAAlNAfUA9AAB1wDZAOUAAdcA5QDeAAGdA+AAngMD6gDfAOsAA90A3gDkAAHdAOQA" +
        "4gAB3gDdANsAAd4A2wDXAAGWAeoA6wADlgHrANYAA3kB7gDjAAPjAO4ARQEDZwHwAFwBA08BTgH0AAdPAfQAogEH8wD0APUAB/MA" +
        "9QDUAAL4ACsB9gAA8gD9AEkBA/IASQFIAQP+ANMAOgAJ0wBQADoACU8A0gClAQfSAEkASgAHSQACAY0BCEkAjQFLAAgCARUBgQAI" +
        "AgGBAE0ACAEBhgGBAAgGAQQBBQEC0wCfAZ4BBxgBSQDSAAcYAdIAGQEHFwFJABgBCAgB1AD1AAIIAfUABwECFwECAUkACBUBAgEW" +
        "AQiHAYYBAQEIhwEBARUBCAwBDQEbAQIdARQBHgEIHwGHARUBCCABFQEWAQjTACEBnwEHVAEhAdMAB1QB0wD+AAcoAScBoQEHTwEo" +
        "AVIBB1IBKAF+AQcMAR0BHgEIDAEeAQ0BCA0BHwEOAQgPARcBGAEIDwEYARABAhABGAEZAQcQARkBEQEHIQESAaABByEBoAGfAQdT" +
        "ARIBIQEHUwEhAVQBBwQBIwEbAQIEARsBBQECCwEbASQBAgsBJAEKAQIIAY4BJgECCAEmAdQAAigBTwGiAQcoAaIBJwEHMwH2AC8B" +
        "AEcBRgHuAANrAf0A8gADawHyAGwBAy0B/QBrAQMuAfwAxAADLgHEAHEBA58DLgFwAQMwATIBfAECMgEHAfUAAjIB9QAxAQK7ADAB" +
        "xQAC+QD2ADMBADgBjwG7AAI4AbsANwECNQE6ATQBAjYBQgE0AQI2ATQBOQECNwE2ATkBAjcBOQE4AQI5AZkBmAECOQGYATgBAjQB" +
        "mgE5AQI6AZsBNAECPgEGAT0BAjsBCgEJAQI7AQkBPwECQQFDAUIBAkEBQgFAAQJDATUBNAECQwE0AUIBAvsAbgFtAQP7ADYBNwED" +
        "+wBEATYBAkQBQAFCAQJEAUIBNgECiQFAAUQBAokBRAFlAQJlAUQBXQECoAPvAKEDA0UBaAHjAAOiA+8AowMDLQHvAKIDAy0BogOk" +
        "AwMvAW8BMwEDpgG8AJsDA0gBwQCnAQNJAb4AwQADSQHBAEgBA8QA/ADDAAPDAPwApQMD6wDFAHwBAuAAngOmAwPsAKMBTQEB9ABO" +
        "AewAB/QA7ABNAQHbAOgASwEBUgFRAVABB1IBUAFPAQdQAVEB7QAHUAHtAOwAAVUBOgA7AAn+ADoAVQEJ/gBVAf8ACSIBEwFYAQca" +
        "Af4A/wAHVQFWAf8ACZkAmACsAACtAJgAmgAArQCYAK4AAJgArQCWAACbAKwAmAAANAA3ADMAADMANAAuAABRAdUA7QAHUgEDAdUA" +
        "B1IB1QBRAQdSASkBAwEHKQFSAX4BB1gBEwFTAQdYAVMBVAEH2ADaAFkBAdgAWQFaAQFXAdgAWgEBVwFaAeQAAeUAVwHkAAHlAOQA" +
        "3gAB3ADiAOQAAdwA5ABaAQFZAeYA3AABWQHcAFoBAV0B+wBtAQNEAfsAXQEDXgFBAUABAlgB/gAaAQdYARoBIgEH6ADbAN0AAegA" +
        "3QBfAQFcAeAApwMD6gCWAWABA6cD4ACdAwNbAeoAYAEDZAFeAUABAisBiQFlAQArAWYB9gAALAH2AGYBACwBbwEvAQOoA6kDLQED" +
        "7wBnAaEDA2kB5gBjAQBiAdwA5gABYgHmAGkBAeMAlQHnAABhAdwAYgEBYAGVAeMAAywBZgFlAQIsAWUBXQECqgOfA3ABA6oDcAGp" +
        "AwMtAWsBbAEDLQFsAe8AA6EDXAGrAwNoAWAB4wADXAGnA6sDAzMBLAEvAQP5APgA9gAALAEvAfYAAKwDLgGfAwNdAWoBLAEDrQMu" +
        "AawDA64D/AAuAQOuAy4BrQMDxwCjALUABZwArQB1AQD6APkAMwEAcAEtAakDAy4B/QAtAQMuAS0BcAEDcQH9AC4BA8QA/QBxAQNJ" +
        "Af0AxAADzACkAKMABcwAowDHAAWnAHIBcwEGpwBzAc4ABnQBqAC9AAaoAHQBkgAGRwHuADMBA6kDLQGkAwNvAUcBMwEDmQCWAJcA" +
        "ADcBuwD7AAPDAKUDrwMDwwCvA7ADA3cBtAC1AAXPAJUAsgAGzwCyALMABu8AoAOjAwNsAWcB7wAD8gDwAGcBA/IAZwFsAQNIAfEA" +
        "8AADSAHwAPIAA7EBkQB4AQaxAXgBzQAGkwCiAJIABucAeQHjAABnAaEDsQMDZwFcAaEDA/AA4ABcAQPxAOAA8AADwQC8AKYBA8EA" +
        "pgGnAQOfALwAwQAEnwDBAJ4ABJMApQCiAAaXAJwAoAAGlwCgAJIABrwAsgObAwOwAK8AkQAGlgCtAJwAAMUAMAF8AQLGALsAxQAD" +
        "ugC7AMYAApwDmwOfAAOgALEArwEGoACvAaUABqAAnACxAAaZAJcAqwAAdQGZAKsAAHUBrgCZAACtAK4AdQEAEwF9ASkBB34BUwET" +
        "AQd+ARMBKQEHKAESAVMBBygBUwF+AQcSASgBoQEHEgGhAaABByYBEAERAQcmAREBJwEHjgEPARABAo4BEAEmAQIlAQ4BHAECGwEN" +
        "AQ4BAhsBDgEkAQIjAQwBGwEC/gBYAVQBB7sBOgA9AAm7AT0AOQAJPQA+ADkACT8APgA9AAlvAD4APwAJbgB/AXAACYABhAByAACA" +
        "AXIAaQAAEwCEAIABABMAgAGBAQAEAAYAggELBACCAYMBCyMABACDAQslAAMAAAAAFQAKAA8ACxYAEACEAQuLABYAhQELfAB6AH0A" +
        "CngAegB8AAp4AHwAewAKdwB2AHgACncAeAB7AAp2AHcAdQAKhgEAAYAACBQBAAGGAQgUAYYBhwEIHgEUAYcBCB4BhwEfAQgNAR4B" +
        "HwEIBQEbAQsBAgUBCwE8AQIFATwBBgECPQEGATwBAj0BPAGIAQI0AZsBmgECZAFAAYkBAioBZAGJAQIqAYkBKwEAKgErAfcAAAAA" +
        "AQAnAAAAACcAJgAAJAAlAAAAAIsAigGMAAqLAIwAfQAKWgB7AHwACloAfABcAAp3AHsAWgAKXwC2AbUBCBUBAQGBAAgfARUBIAEI" +
        "DgEfASABCCQBDgElAQIKASQBJQECCgElAQkBAjwBCwEKAQI8AQoBOwECOQGaAZkBAisBZQFmAQAmACcAiwEAJgCLAYwBACQAAAAt" +
        "AAAXAB0AigELAgFNAI0BCBYBAgEXAQggARYBFwEIDgEgARcBCA4BFwEPAQgcAQ4BDwECJQEcAQ8BAiUBDwGOAQIJASUBjgECCQGO" +
        "AQgBAgkBCAEHAQI/AQkBBwECOwE/AQcBAjsBBwEyAQKPATABuwACmAEyATABApgBMAGPAQI4AZgBjwECMgABAAIAAIsBJwABAACL" +
        "AQEAMAAAAQAyADEAAAEAMQAwAAAtADAAKgAAKgACACgAACoAMQAzAAAqADMAKwAAkAEfAB4AAJABHgAIAAuRAYIAHwAAkQEfAJAB" +
        "AKsBfwFpAAmrAWkAcgAJcQBuAHAACW0AbgBxAAltAHEAeQAJbwBtAHkACT4AbwCSAQk+AJMBOQAJOQCTATsACTkAOwC7AQmSAXMA" +
        "PAAJkgE8AJMBCT4AkgGTAQlpAWMB5wAAYAFiAWkBAWABYQFiAQGWAekAYQEBlgFhAWABA9YA4QDpAAPWAOkAlgED7ABLAaMBATIB" +
        "MQF8AQKYATsBMgECmQE7AZgBApoBPAE7AQKaATsBmQECmgGIATwBApsBPQGIAQKbAYgBmgECmwE+AT0BAioAKAAsAAAsACgAIwAL" +
        "LwAiACMAC40AIgAvAAuGAIUAnAEAhgCcAWoAAKkBawBqAACpAWoAaAAAZwBoAJ0BCWMAZwCdAQljAJ0BYgAJWABjAGIACVgAYgBh" +
        "AAlXAFgAYQAJngHSAE8AB58BGQHSAAefAdIAngEHoAERARkBB6ABGQGfAQehAScBEQEHoQERAaABB6IB8wAnAQf0APMAogEHSwFK" +
        "AU0BAUsBTQGjAQFKAUsB1gAB6QDhAOgAAekA6ABfAQHpAF8BYQEBHwCKAI0AAIIAiQCKAACCAIoAHwAAZABmAGUACWQAZQBiAAls" +
        "AGEAYgAJbABiAGUACUMAYQBsAAlQAU4BTwEHTgFQAewAB18B3QDiAAFfAeIAYQEB3ABhAeIAARsAGQCKAAsbAIoAiQAAZwCsAYgA" +
        "CWMAWwBnAAlbAGMApAEJWwCkAVYACVUAVgCkAQnSAEoApQEH8wDUACYBAvMAJgEnAQdNATEB9QACSgF8ATEBAkoBMQFNAQHrAHwB" +
        "SgEC6wBKAdYAAWABaQGVAQGVAWkB5wAA4ACmAZsDA+AAmwOmAwPFAOsAxgAD+wC7AMIAA/wArgOzAwP8ALMDpQMDSAGnAfEAA6YB" +
        "4ADxAAOmAfEApwEDnAB1AasAAJcAnACrAACHABwAhgAAhgBrAIcAAGsAhgBqAACoARcAhQELqAGFARYACxcAqAEYAAscAB0AFwAL" +
        "HAAXABgACxwAhwAdAAupAWgAiAAJqgGPAIwACqoBjACKAQqPAKoBiAAKnQFmAGQACX8BZgCdAQlxAHAAfwEJcQB/AasBCX0AegBx" +
        "AAp9AHEAqwEKjABcAHwACowAfAB9AApcAIwAjwAKXACPAI4AClsAjgCPAApbAI8ArAEKZwBbAKwBCYUAGwCJAACtARsAhQALrgEO" +
        "ABsAC64BGwCtAQsOAK4BEQALDgARAA0ACw8ADQARAAsPABEAhAELDwCEARAACw8AEAASAAsTABIAEAALkQESABMAC4IAkQETAACC" +
        "ABMAgQEAggCBAYMAAIMAiQCCAACFAIkAgwAAFQAPABIACwgAFACQAQsVABQACAALFQAIAAkACwkACgAVAAsIAB4AggELCACCAQYA" +
        "CwcACQAIAAsHAAgABgALIAAMAAkACyAACQAHAAsMACAAIgALDAAiABoAC5wAoQCXAAacAKEAkgAGnACSALEABq8BuQClAAaxALkA" +
        "rwEGsAGmALkABrABuQCxAAapAKYAsAEGpQCwAKIABrkAsAClAAamAKoAuQAGpgC9AKoABnQBvQCmAAaiAM0AqAAG0ADRALAABqgA" +
        "zgBzAQaoAHMBvQAGzQDOAKgABpUAvQBzAQaVAM8AvQAG0ADPANEABqIAsACxAQaiALEBzQAGAAAmAIwBAAAAjAEtAAACAAAAAwAA" +
        "AgADACgAACoALAA2AAAtACoANgAANQA2ACMACzUAIwAhAAs1ACEAJAALBQAlACQACwUAJAAhAAsEAAMAJQALBAAlAAUACygAAwAE" +
        "AAsoAAQAIwALBQAHAAYACwUABgAEAAshACAABwALIQAHAAUACyIAIAAhAAsiACEAIwALtgDIAHcBBLYAdwG1AAXJAMgAtgAEyQC2" +
        "AJ8ABMkAnwCdAASdAJ8AngAEywDAAJAABMsAkADKAASUAMAAywAFlADLAMwABZQAzADHAAW0AJQAxwAFyAC3ALQABcgAtAB3AQSd" +
        "ALcAyAAEngDBAMoABMoAwQC/AASjAMMAtQAFtQDDALADBbUAsAOcAwO2AJwDnwAEdgHAAJQABbQAswCUAAW3ALgAswAFtwCzALQA" +
        "BZ0ArwC4AASdALgAtwAEkQCvAJ0ABJEAnQCQAAR2AacAeAEEdgF4AZEABKcAdgGzAQWUAHIBpwAFlACzAacABLIAlQByAQayAHIB" +
        "lAAFswCVALIABoYBtAG1AQiGAbUBgQAItgGBALUBCE0AgQC2AQiNAU0AtwEIjQG3AUsACEsAtwG4AQpLALgBSQAKSQC4AbkBCUkA" +
        "uQFKAAlKALkBpQEJpQG5AboBCaUBugFPAAlQAE8AugEJOwA6ALsBCTwAlAE7AAmUATwAfwAKlAF/AIAACIAAfwB+AAiAAH4AtAEI" +
        "gAC0AYYBCNYASwHoAAHWAOgA4QABvAG+Ab0BAMABvwG+AQC8AcABvgEAwAHCAcEBAL8BwgHBAQC/AcABwgEAvwHAAcEBAL0BwwHA" +
        "AQC9AcABvAEAwAHFAcQBAMABxAG/AQDDAcgBxQEAwAHDAcUBAMMBvQHGAQDHAcYBvQEAywHKAb4BAMoByQG9AQDKAb0BvgEAwwHN" +
        "AcgBAMMBxgHNAQBZAdoAzwEAzwHOAc0BAM0BzgHIAQDRAdABzgEA0QHOAc8BAMgBzgHFAQBjAc8BzQEA1AHnAGMBAPcA+ADSAQDT" +
        "Ac0BxgEA0wHGAdUBAM0B1AFjAQDUAc0B0wEA1AHTAfoAANcB0wHVAQDXAdUB1gEA0wHSAfgAANcB0gHTAQDZAdsB3AEA2QHcAdoB" +
        "ANgB2QHeAQDeAdkB2gEAyQHHAb0BANsByQHKAQDbAcoB3AEAxwHYAdUBAMcB1QHGAQDZAdgB2wEA2AHHAdsBAM8B5gBZAQBjAeYA" +
        "zwEA1AHuAHkBANQBeQHnAAD6AO4A1AEA+gAzAe4AANMB+QD6AAD4APkA0wEA9wArAfgAAN4B1gHVAQDKAd0B3AEA2AHeAdUBAMcB" +
        "yQHbAQCWAJkArAAA3wHgAeEBAOgB6QHqAQvoAeoB6wEL7AHtAeoBC+wB6gHpAQv0AfMB8QEL8AFKA/UBC/AB9QFuAwvpAe4B7AEL" +
        "dAPwAfcBC/gB+QHrAQv4AesB6gEL6gHtAfoBC+oB+gH4AQt0A/cB+wELbgP3AfABCwkC4QENAgAJAg0CCgIACAICAkkDC0kDCAIO" +
        "AgtJAw4CSAMLEwIRAhACABMCEAISAgARAhMCDQIAEQINAuEBABQCFQIMAgAMAgMCFAIAUwMMAg8CAFMDDwJSAwAKAg0CEgIAGwJa" +
        "AxoCCRwCHgIfAgkcAh8CIAIJHwIeAiECCR8CIQIiAgkfAiMCJAIJHwIkAiACCSQCIwIlAgkkAiUCJgIJfQN+AyYCCX0DJgInAgl8" +
        "A30DJwIKfAMnAisCCCwCfAMrAggsAisCLQIIfgN/AyQCCX4DJAImAgkvAiACJAIJLwIkAn8DCRwCIAIvAgkcAi8CGQIJMAInAiYC" +
        "CTACJgIlAgkwAjECMgIKMAIyAjMCCjQCMQIwAgk0AjACJQIJNQIxAjQCCTQCNgI3Agk0AjcCawMJIwI2AjQCCSMCNAIlAgk4AjIC" +
        "MQIKOAIxAjUCCTkCOAI6Ago5AjoCOwIKOAI1AjoCCjICOAI5AgoyAjkCPAIKMwInAjACCi0CJwIzAggtAjMCPQIKJwItAisCCC0C" +
        "PQI/AgotAj8CPgIIPQIzAjICCj0CMgI8AgofAiICNgIJHwI2AiMCCUACNgIiAglBAkMCZAMJQgJrAzcCCUcCZANFAwlHAkUDSAIJ" +
        "SAJJAkcCAEsCTAJNAglLAk0CRAIJTgJMAksCCU4CSwIhAgkhAksCIgIJTQJFA0UCCU0CRQJEAgkeAk4CIQIJWQNSAlMCCVkDUwJO" +
        "AglSAlQCVQIKUgJVAlMCClYCVAI/AgpYAlMCVQIKWAJVAlcCClgCVwJZAgpYAlkCUAIKTgJTAlgCCVYCPAI5AgpdAj4CPwIIXQI/" +
        "AlQCCnkDegM+Agh5Az4CXQIIUgIbAl4CClICXgJUAgpUAl4CXQIIDwIJAhACABUCAgILAgsHAgICCAILPgJ7AywCCD4CLAItAggO" +
        "Av0BSAMA8QHzAVcDC/EBVwNYAwtHA2ICSQIARwNJAkYDAEkCYgJjAwBmAmcCbwMAZgJvA0oCAGUCZAJzAwBlAnMD+wEL+wFzA3QD" +
        "C2ICZAJjAwDyAWMC9QEL8gH1Ae8BC/UBYwJRAgv1AVECagILUQJxA1wCClECXAJqAgr2AUsDagIL9gFqAlEDC0gCRgNJAgD+Af0B" +
        "bAIAAQL5AWwCC/kB+AFpAgv5AWkCbAIADgJsAv0BAAgCAgIOAgsTAhYCFwIAPQI8AlYCCj0CVgI/Ago7AjoCbQIKcgNnAm4CCvwB" +
        "UQNwAwtwA2cCZgILcANmAvwBC0cCZwJGAgl+ApsCtAMDPwOZApoCAnECkAJ2AwZxAnYDiAIGcQKHAoECBqwCrQKGAga1A5QClQIF" +
        "tQOVArYDA6sCgwKeAgWrAp4CqgIFbwJwAj4DBG8CPgOfAgR4A3MCPgMFcwKRApICBTwDcQKIAgY8A4gChQIGiwJ1AncCAHUCdgJ7" +
        "AgCjAoICogIDdwNwAo8CBnIChAJ/AgZ4AncCjQIAjwKOApcCBo8ClwKwAgawApcCkgIGsAKSAq4CBnECcgJ/AgZ2AnECgAIGOgM7" +
        "A3QCBoICowIQAwOCAhADnQIDngKDAoICBJ4CggKdAgWpAqoCngIErAKGAkEDBp4CnQKgAgSUAqYCkwIFfAKoAqcCBKkCfQJ8AgSp" +
        "AnwCbwIErgKcAokCBpgCrwKuAgaYAq4CiQIGjwKvApgCBi8CLgJlAwkvAmUDsgIJFAPMAs0CB7cDuAO6AgO4ArwCVwEBuAJXAeUA" +
        "AeUA2QC2AgHlALYCuAIBXQO1ArkCA10DuQLCAgO/AkIDxgIAQgO9AgwDA0IDDAPGAgMuAyQDyAIDyAIkA8kCAxcDaQPMAgcXA8wC" +
        "FgMHywKzAs0CAssCzQLMAgfPAs4C+AIAygIPAxADA8oCEAPVAgPWAhkCsgIJsgIZAi8CCS4CbAOxAgexAikCKAIHKAIqAlQDCCgC" +
        "VAPZAgjZAiwCYAII2QJgAucCCNgCYAJMAwggA9YC7AIHFAHmAtcCCAYB2gIEAQKyAmUDZgMH6gLrArECB+oCsQIoAgfpAuoCKAII" +
        "3ALbAs0CAtwCzQKzAgLpAigC2QII5wLoAtkCCE0D5wLYAghNA9gCTAMIDAHtAuACAh0B5gIUAQjvAucCTQMI8ALoAucCCLICZgPx" +
        "AgfsAtYCsgIH7AKyAvECB/YCaAP1AgcXAxkD9gIHGQNEA/YCBwwB4AIdAQjgAuEC7wII4gLjAuoCAuIC6gLpAgjjAuQC6wIH4wLr" +
        "AuoCB/ECZgNnAwfxAmcD5QIHGgPsAvECBxoD8QLlAgcEAe0CIwEC3wLeAvICAt8C8gLtAgLcArMC9AIC3AL0AlUDAvYC9QJpAwf2" +
        "AmkDFwMHAAP8As4CAA4DxgINAwMyAzMDygIDMgPKAtUCA/oCMgPVAgP7AjkDowID+wKjAtQCA7kDOAP7AgP9AkMD/wIC/wL+As0C" +
        "Av8CzQLbAgKaAtIC/QIC0AIAA84CAAQDAwOaAgIEA5oCVgMCNQEBAzoBAgIDBQMBAwICAwEDCgMCAwMEAwUDAgMDBQMCAwIFAwQD" +
        "XwMCBQNfA2ADAgEDBQNhAwI6AQEDYgMCOgFiA5sBAj4B2gIGAQIGAwgD3QICBgPdAt4CAkEBCQMKAwJBAQoDQwECQwEKAwEDAkMB" +
        "AQM1AQLTAjUDNgMD0wIDAwIDA9MCAgMLAwILAwIDCgMCCwMKAwkDAk8DLAMLAwJPAwsDCQMCLAMlAwsDAgwDvQIvAwO6A7sDxwID" +
        "vAO9A8cCA/oCvgO8AwP6ArwDxwID/AIAAzcDA6QCtAObAgMPA20DmwIDDwObAqACAxADDwOgAgMQA6ACnQIDowKiAtQCA6ICvwPU" +
        "AgPDAkMD0gICxAIUAxEDAcQCEQMSAwHMAhQDxAIBzALEAhUDAcwCFQMWAwe2AhIDwAIBGQMXAxUDBxkDFQMYAwcVA8QCxQIBFQPF" +
        "AhgDBxsDGgIZAgnWAiADGwMJ1gIbAxkCCRwD/wBWAQcbAxwDVgEJHQO0AtUABx0D1QADAQcpAR4DHQMHKQEdAwMBByIBIQMfAwci" +
        "AR8DEwEHGgH/ACADBxoBIAPWAgf/ABwDIAMHGwMgAxwDCXgCiwJ3AgCMAo0CdwIAjAJ5AncCAHcCdQKMAgB6AncCiwIAEwISAhYC" +
        "ABICDQITAgAYA8UCtAIHGQMYA7QCBxkDtAIdAwcZAx0DHgMHHgNEAxkDBx8DGgMiAwchA+wCGgMHIQMaAx8DB7cC2ABXAQG3AlcB" +
        "vAIB2gDYALcCAdoAtwK+AgElAzUD0wIDCwMlA9MCA14BCQNBAQIhAyIBGgEHIQMaAdYCB8ACJgO4AgHAArgCtgIBJAPBA7oCA8IC" +
        "JwNdAwMjAycDwgIDwQO3A7oCA2QBKwMJAwJkAQkDXgECKgH3AisDAioBKwNkAQJQA/gCLAMAUAMsA08DAvgCzgItAwD5Ai0DzgIA" +
        "+QL8AjcDA8ID+gLDAwPHArsDLgMDMAMqA74CACkDMAO+AgEpA74CtwIBvQK/AlwDACgDKQO3AgEnA70CXAMD+QIlAywDAvkCLAMt" +
        "AwLEA8MDOAMDxAM4A7kDA/oCxwIzAwP6AjMDMgMDLwO9AicDA7sDxQMkAwMkA8UDwQMDAAP8AvkCA1ADNAP4AgAqAfcANAMAKgE0" +
        "A/cCANACzgLPAgD5As4C/AIAJQP5AjEDA8YDuQP7AgPHA8YD+wIDyAPHA/sCA8gD+wLUAgOmApQCggIFewI9A4wCANECAAPQAgA4" +
        "A8MD+gID+wI4A/oCA/sC+gLVAgM5A/sC1QIDowI5A9UCAxADowLVAgOrAqYCggIFqwKCAoMCBYYCrQI7AwaGAjsDOgMGPAOcAocC" +
        "BocCcQI8AwYOAwADxgIDNwMAAw4DA8MDvgP6AgN4AnYCdQIAAwPTApoCA6ICtQPJAwOiAskDvwMDQAOUApMCBa4CkgKRAgauApEC" +
        "dAIGxwK9A7oDAzMDxwIuAwPKAjMDLgMDygIuA8gCAw8DygLIAgMPA8gCyQIDdwOsAkEDBncDQQNwAgZyAnECgQIGvwK9AkIDAC4D" +
        "ygO7AwMuA7sDJAMDyQIkA7oCA5sCbQOkAgOgAqQCmwIDfgJ9AqACBH4CoAKbAgRyAoEChAIGdgJxAn8CBnYCfwJ7AgabArQDywMD" +
        "jwJwAo4CBnUCewKMAgDSAkMD/QICpQLSApoCA7YDfgK0AwOZAqUCmgICfwKEAnUDBn8CdQOQAgZ/ApACewIGeAKKAnYCAD0DigJ4" +
        "AgA9A3gCjQIAjAI9A40CABMBHwMeAwcTAR4DfQEHRAMeAx8DB0QDHwMiAwdEAyIDGgMH9gJEAxoDB/YCGgPlAgflAmcDaAMH5QJo" +
        "A/YCB/QC9QLkAgf0AuQC4wIHVQP0AuMCAlUD4wLiAgLzAu4C4QIC7QLyAuECAu0C4QLgAgIjAe0CDAEC1gLsAiEDB4ADGAIcAgmA" +
        "AxwCGQIJHAIYAh0CCR4CHAIdAglOAh4CHQIJTQJPAkUDCUYDSAJRAgBGA1ECYwIA8gFHA0YDAPIBRgNjAgDjAUkDSAML4wFIA+UB" +
        "CwICSQPjAQsEAt8B4gEA9AHuAekBC/UBSgPvAQtqAksD9QELWwJcAlkCClcCWgJbAgpXAlsCWQIKVgJaAlcCClYCVwJVAgpVAlQC" +
        "VgIKTANfAtcCCOYCTQNMAwjmAkwD1wIIHQHvAk0DCB0BTQPmAgjgAu8CHQEIBAHfAu0CAgQB2gIHAwIEAQcD3wICPgFOAwcDAj4B" +
        "BwPaAgIBA2EDYgMCKwNPAwkDAvcCUANPAwL3Ak8DKwMC9wI0A1ADAN8BBQIGAgDfAQYC4AEAAwLfAQQCAGoCXAJrAgpqAmsCUQMK" +
        "OQI7AlsCCjkCWwJaAgpWAjkCWgIKPgJ6A3sDCOcCYALYAgjvAvAC5wII4QLwAu8CCPIC8wLhAgLeAt0C8wIC3gLzAvICAgcDBgPe" +
        "AgIHA94C3wICBQNgA2EDAvgCLQMsAwAFAlMDUgMABQJSAwYCAAMCDALfAQD2AVED/AEL2QJUAywCCOgC6QLZAgjwAukC6AII4QLi" +
        "AukCCOEC6QLwAgjuAuIC4QIC8wJVA+ICAvMC4gLuAgLdAtwCVQMC3QJVA/MCAt0C2wLcAgIIA9sC3QICBgP/AtsCAgYD2wIIAwJW" +
        "A5oC/QICXwNWA/0CAl8D/QL/AgIEA1YDXwMCEQLhAeABAFIDDwLgAQBSA+ABBgIA4AEPAhACAOABEAIRAgAMAgkCDwIACQIHAuEB" +
        "AAkCCgISAgAJAhICEAIAVwPnAf0BC1cD/QH+AQBYA1cD/gEAWAP+AWECAHEDUQJIAglxA0gCRQMJUAJPAk0CCUwCWAJQAglMAlAC" +
        "TQIJTgJYAkwCCR0CWQNOAgkdAhgCWgMJGAKAAxoCCRgCGgJaAwlZA1oDGwIJWQMbAlICCR0CWgNZAwl9AR4DKQEHtQLDArkCAzAD" +
        "vwIqAwAnAzADKQMBJwMpAygDAV0DJwMoAwNdAygDwQIBtQJdA8ECA7UCwQK7AgMSA2oDEQMB/wJDA/4CAl8D/wIGAwJgA18DBgMC" +
        "YQNgAwYDAmEDBgMHAwJhAwcDTgMCYgNhA04DAmIDTgM+AQKbAWIDPgECCQILAgcCAAsCAgIHAgsOAgICAQILbAIOAgECC2UCSQJj" +
        "AwBlAmMDZAIAbwNHAkkCAG8DSQJKAgBGAmQDRwIJQgJBAmQDCUICZANGAgk3AkACQQIJNwJBAkICCTYCQAI3AgllAy4CsQIHZgNl" +
        "A7ECB2YDsQLrAgdnA2YD6wIHZwPrAuQCB2gDZwPkAgdoA+QC9QIHaQP1AssCB8wCaQPLAgcRA7UCEgMBwQImA8ACAcECwAK7AgHB" +
        "AigDJgMB/gFsAmkCAGEC/gFpAgBhAmkCaAIAQwJBAkQCCUMCRAJFAglLAkQCQQIJSwJBAkACCSICSwJAAgkVAxcDFgMHJgMoA7wC" +
        "ASYDvAK4AgG3ArwCKAMB+gFoAmkCAPoBaQL4AQtGAmcCcgMJQgJGAjoCCToCNQJrAwk6AmsDQgIJNAJrAzUCCbECbAMpAgfLAvUC" +
        "9AIHywL0ArMCAhQDzQL+AgIRAxQD/gIBEQP+AkMDAsMCtQIRAwHDAhEDQwMCJwNcAzADAVwDvwIwAwC6AsADtAMDugK0A6QCA9IC" +
        "pQLDAgPTAqECmgID1AK/A8wDA9QCzAPIAwMPA8kCbQMDpAJtA8kCA6QCyQK6AgN7AooCPQMAdgKKAnsCAGYCZQL7AQBlAmYCSgIA" +
        "SgJJAmUCAG4D9QFLAwtuA0sD9gEL9gH3AW4DC/sB9wH2AQv7AfYB/AEL+wH8AWYCC28DZwJHAglwA1EDawIKcANrAm4CCm4CZwJw" +
        "AwpkA0MCRQIJRQNkA0UCCVACcQNFAwlQAkUDTwIJXAJxA1ACClwCUAJZAgprAlwCWwIKawJbAjsCCjsCbQJuAgo7Am4CawIKOgJy" +
        "A24CCjoCbgJtAgpGAnIDOgIJZAJoAvoBAHMDZAL6AQt0A3MD+gELdAP6Ae0BC+0B7AHwAQvtAfABdAML7gFKA/ABC+4B8AHsAQvu" +
        "Ae8BSgML7gHxAe8BC/IB7wHxAQtYA/IB8QELYQLyAVgDAGECRwPyAQBhAmICRwMAYgJhAmgCAGQCYgJoAgD0AfEB7gEL5wFXA/MB" +
        "C/QB6AHnAQv0AecB8wEL6AH0AekBC+cB5QFIAwvnAUgD/QEL5gHlAecBC+YB5wHoAQv/AeYB6AEL/wHoAesBC+sB+QEBAgvrAQEC" +
        "/wELewJxAoACBnsCdgKAAgZ7ApACcQIGdQOEApgCBpACdQOYAgZ2A5ACmAIGdgOYAoUCBogCdgOFAgaEAoECjwIGmAKEAo8CBoUC" +
        "mAKJAgaFAokCnAIGPAOFApwCBoEChwKsAgavAo8CsAIGhwKcAjsDBocCOwOtAgasAocCrQIGdAI7A5wCBnQCnAKuAgavArACrgIG" +
        "gQJ3A48CBoECrAJ3AwbfAQwCUwMA3wFTAwUCAOEB4gHfAQDhAQcC4gEACQIVAgsCAAwCFQIJAgAUAgICFQILFAIDAgACCxQCAAIC" +
        "AgvkAQACAwIL5AEDAgQCC+MB5AEEAgvjAQQC4gELBwICAuMBCwcC4wHiAQvkAeMB5QEL5AHlAeYBCwAC5AHmAQsAAuYB/wELAQIC" +
        "AgACCwECAAL/AQuVApQCQAMFlQJAA6cCBKgCfgKVAgSoApUCpwIEqAJ8An4CBHwCfQJ+AgSqAqkCbwIEqgJvAp8CBHMCqwKqAgVz" +
        "AqoCnwIFcwKmAqsCBZMCpgJzAgWnAkADkwIEpwKTApYCBXwCpwKWAgR9AqkCoAIEqQKeAqACBIIClAKiAgWUArUDogIFlQJ+ArYD" +
        "BD4DcwKfAgWTAnMCkgIFlgKTApICBZYCkgKXAgV8ApYClwIEfAKXAo4CBHACfAKOAgRwAm8CfAIEPgNwAkEDBD4DQQOGAgSGAngD" +
        "PgMFcwKGAngDBHMChgI6AwWRAnMCOgMFkQI6A3QCBpICkQJ0AgZMA2ACegMITAN6A3kDCHsDegNgAggsAnsDYAIIVAMqAnwDCFQD" +
        "fAMsAggqAigCfQMKKgJ9A3wDCigCKQJ+AwkoAn4DfQMJKQJsA34DCWwDLgJ/AwlsA38DfgMJLwJ/Ay4CCRoCgAMZAgkbAhoCWwMJ" +
        "WwNfAl4CCFsDXgIbAgpfAl0CXgIIXwJMA3kDCF8CeQNdAgi1ArsCwAIBtQLAAhIDAYEDggODAwDCAYMDwQEAgQODA8IBAIMDvgG/" +
        "AQCDA78BwQEAggOBA8IBAIIDwgGEAwDCAb8BxAEAwgHEAcwBAIQDzAGHAwDCAcwBhAMAxQHMAcQBAIQDhQOCAwCGA4IDhQMAvgGD" +
        "A4oDAL4BigPLAQCKA4MDiQMAiQODA4IDAIkDggOIAwDLAYoDiQMAywGJA8oBAIQDhwOLAwCEA4sDhQMA2gDRAc8BANEBiwPQAQCL" +
        "A4cD0AEAhwPMAdABAMwBxQHOAQDMAc4B0AEAKgOLA9EBAI4DKgO/AgD3AIwDNAMANAOMA88CAIwD9wDSAQCNA48DhQMAjQOFA4sD" +
        "AIsDKgOOAwCOA9ECjQMAjgONA4sDAJEDkAOPAwCRA48DjQMA1wHWAZADANcBkAORAwCNA88CjAMAkQONA4wDANIB1wGRAwDSAZED" +
        "jAMAkwOUA5YDAJMDlgOVAwCUA9oB3AEAlAPcAZYDAJIDmAOTAwCYA5QDkwMAmAPeAdoBAJgD2gGUAwCIA4IDhgMAlQOJA4gDAJcD" +
        "3QHKAQCXA8oBiQMAlQOWA4kDAJYD3AHdAQCWA90BlwMAhgOFA48DAIYDjwOSAwCTA5UDkgMAkgOVA4YDANEB2gC+AgAqA9EBvgIA" +
        "jgO/AsYCANECjgPGAgDRAsYCAAMAjQPRAtACAM8CjQPQAgA0A88C+AIA3gGYA5ADAN4BkAPWAQCYA48DkAMAiQOWA5cDAJIDjwOY" +
        "AwCGA5UDiAMAdQKLAngCAMABwwGZAwDCAZoDwAEAmgPAAZkDAMIBmgOEAwDCAZkDmgMAwgGZA8ABANUAlwHtAAHVALQCXgMH1QBe" +
        "A5cBAbQCxQJeAwF7AUwB7QABewHtAJcBAXoBlwFeAwETA3oBXgMBEwNeA8UCARMDEgO2AgFMAdsASwEB7QBMAUsBAe0ASwHsAAHF" +
        "AsQCEgMBxQISAxMDAXoB2QDXAAF6AdcAewEBewHXANsAAXsB2wBMAQF6ARMDtgIBegG2AtkAAXoBewGXAQE="

    private const val COMPACT_V_B64 =
        "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"

    private const val COMPACT_T_B64 =
        "AAADAAIAAAgA6AAKAAsIAAoACQALCQAMAAsACwkACwAIAAsGAPIA8QALBgASABEACwYAEQAFAAsfABQAAgAAAAACABgAAAAAGAAX" +
        "AAAXABgAHAALGgAXABkAAAEAFQADAAABAAMAAAAAAQAbABUAABoAHwAWAAABACAAHgAAKgAsACkACS8AKQAsAAkpAC8ANAAKKQA0" +
        "ADMACSYA7gDtAAkmAO0AKAAJNQA3ADYACjUANgA0AAo0ADYAOAAKNAA4ADMACS8ALAA5AAg6ADkALgAIOQA1ADQACjkANAAvAAo8" +
        "AD4APQAJPADsADgACewAPAAoAAnsACgA7QAJJwA8AD0ACUQARQBGAAk7AEQARgAKOwBGAEcACkcASAA3AApGAEMASAAKRgBIAEcA" +
        "CjoAIgBEAAg6AEQAOwAIIgA6AEkACBsAAQAgAAAUABkAHAALFAAcABMAC/EAEgAGAAtLABAADgALSwAOAE0AC00AQQBLAABOABIA" +
        "TQAASgBAAEEAAEIADAAJAAtCAAkATwALPwBBAEAAAD8AQQA+AAAdABIATgAADQBNAA4ACwoAEABMAAsKAEwAUAALTAAQAEsAC0wA" +
        "8ABLAABgAGwA7gEEbABgAF8ABOEA7gFsAAThAGwAcAAFVgB0AG0ABlYAbQB/AAZiAGgAZgAGYgBxAGYABlkAeABdAABbAFwAWAAA" +
        "mgB7AGEAA3oA7wFkAANUAGIAZgAGfwBtAGcABlYAbwB0AAZgAHwAXwADawBSAF0ABn0AewB6AANlAHsAfQAFaQB2AGMABmkAYwB3" +
        "AAZaAFwAWwAAfwBnAHcABn8AdwBjAAZcAHIAcwAAWQBYAHgAAGEAUQDiAARVAGUAZAAFiQCIAJIAAYkAkgCRAAGOAIcAiAABjgCI" +
        "AIkAAYMAiwCGAAGRAIsAgwABkQCDAIkAAZIAiwCRAAGLAJIAiAABiwCIAIYAAZMAigCVAAOQAJQAjAAAjwDrAIcAAY8AhwCOAAGb" +
        "AJoAfAADmwB8ANkAA5oAewB8AAN7AJoAegADogAyACQACaUApAAuAAikAOYAOgAI1QCjAKIAB7IASQCzAAi0AOYApAAItQCrAKwA" +
        "ArUArAC2AAK2AK0ArgACtgCuALcAArcArgCvAAK3AK8AuAACuACvALAAB9MA0QDSAAfTANIA2AAHqwCyALMACKsAswC7AAjnALQA" +
        "rQAIrQCkAKUACK0ApQCuAAiuAKUALQAIrgAtAK8ACKsAuwCsAAinALUAvAACpwC8AKgAAqoAtgC3AAKqALcAqQACqQC3ALgAAqkA" +
        "uACAAAK5AJ0AgAAHuQDQAJ0AB+UA0ADRAAflANEA0wAH3QCYAJ4AAJsAewCaAAPAAHoAmgADwACaAJsAA5UAdQDDAAKVAMMAwgAC" +
        "xQDGAMQAAnkAxwDDAALDAKkAwgAC6QCqAKkAAukAqQDDAALJAMoAyAACqQCAAMIAAswAxgDEAALMAMQAywAC3AChAL8AAvABmQDx" +
        "AQN8AF8A8gEDegDzAe8BA5YA0ADXAAGiACEA1AAJogDUANUACSEA1QDUAAmBAKYAlwABcgBcAFcAAFcAcwByAABeAHIAcwAAXgBz" +
        "AF0AAAEAGgAeAAAaAAEAAAAAAAAXABoAAIUAjQCJAAGFAIkAgwAB0ACmAIEAB9AAgQDXAAHTALoA1gAH0wDWAOUAB7oA0wDYAAe6" +
        "ANgAsQAHjQCEAI4AAY0AjgCJAAH0AXwA8gEDggCGAIgAAdkAfADxAQPxAXwA9AEDjACTANoAA+8AdQB5AAOQAIwA2wAA3wDcAL4A" +
        "AL4A3AC/AAC+AL8AzgAAnACbANkAA5kA2QDxAQPrANsAhwAAvwCeAM4AAN4AvgDOAADfAL4A3gAAvQDfAJ8AAN4AzgCeAADfAN4A" +
        "4AAA3wCgAJ8AAL8A3QCeAADzAXoAwAAD8wHAAPUBA3AAVQBkAAVoAHYAVgAGUgBeAF0ABpgA3QC/AAOYAL8AwQAD9QGbAPYBA8AA" +
        "mwD1AQNlAH0AegAFZQB6AGQABWkAVQBWAAZpAFYAdgAGUgBqAHYABvYBnAD3AQObAJwA9gEDYQB7AGUABGUAUQBhAARrAFMAUgAG" +
        "eABTAGsABqEAeQDvAANkAOEAcAAFVQBwAHQABVUAdABvAAVuAGgAYgAGUgBuAGIABpwAmQDwAQOcAPAB9wED2QCZAJwAA3wAmgBh" +
        "AAN8AGEAYAADVABjAFMABmwAfgDiAARsAOIAUQAEUwBYAGIABl8A7gHyAQNfAGAA7gEDXAByAF4AAFwAXgBYAABzAFcAXQAAXQBX" +
        "AFkAAFgAWQBXAABYAFcAXAAAtQC2ALwAAtAA5QCmAAfWAKYA5QAHowCxANgAB6MA2ACiAAeiANgA0gAHogAkACEACSMAJQAhAAkj" +
        "ACEAJAAJJwAlACMACUEADABCAABBAEIAPgAA6gAaAB4AABUAGwAWAABQAE8ACQALUAAJAAoAC+YASQA6AAizAEkA5gAIswDmALQA" +
        "CLsAswC0AAi7ALQA5wAIrAC7AOcACKwA5wCtAAi2AKwArQACqAC8ALYAAqgAtgCqAALKAKgAqgACyADKAKoAAsgAqgDHAALGAMgA" +
        "xwACxgDHAMQAAs0AywDEAAK9AMsAzQACvQDNANwAAr0A3ADfAADfAOAAoAAAAgADAB8AADUARwA3AAo5ADsARwAKOQBHADUACjoA" +
        "OwA5AAikADoALgAIrQC0AKQACMcAqgDpAALEAMcAeQACoQDEAHkAAqEAzQDEAALcAM0AoQAC3gCeAOAAAOAAngCgAAANAAYABQAL" +
        "DQAFAA8ACw4ABgANAAvoAA4AEAAL6AAQAAoACywALgA5AAh5AMMAdQACIAAeABsAABYAGwAaAAAbAB4AGgAASgALAAwAAEoADABB" +
        "AAAnAD0AQwAJJwBDAEYACScARgBFAAklACcARQAJJQBFAEQACSUARAAiAAklACIAIQAJjACHANsAANoAhwCMAAGTAIcA2gABggCV" +
        "AMIAAYIAwgDPAAHPAIYAggABxwDpAMMAAkEATQBKAABMAEEAQAAATABAAD8AAIAAnQDPAAeAAM8AwgAHkwCIAIcAARIAHQARAAAn" +
        "ACgAPAAJJgAoACcACSYAJwAjAAkkACsAJgAJJAAmACMACaIAMAAxAAmiADEAMgAJsAAwAKIAB7AAogDSAAe5ALAA0gAHuQDSANEA" +
        "B9AAuQDRAAedANAAlgAHnQCWAM8AAZYAhgDPAAEPAB0ATgALDwBOAA0AC00ADQBOAAtLAEEATAAA7AA4ADYACTgA7ADtAAk4AO0A" +
        "MwAJ7gApADMACe4AMwDtAAkmACkA7gAJKwAqACkACSsAKQAmAAksACoAKwAJsACvAC0AB7AALQAwAAe5ALgAsAAHgAC4ALkAB5MA" +
        "lQCCAAN1AJUAigADPgA8AD8ACUIAPQA+AAlDAD0AQgAJQwBCAE8ACkMATwBIAApIAE8AUAAKSABQADcACjcAUABMAAo3AEwANgAK" +
        "OAA2AEwACT8APAA4AAk/ADgATAAJBwAOAOgACwgABwDoAAsSAEoATQAACwBKABIAAAsAEgDxAADyAAgACwAL8gALAPEACwYACADy" +
        "AAsGAAcACAALBwDoAAYACw4ABwAGAAtYAF4AYgAGeABYAFMABl0AeABrAAZSAGIAXgAGUwBiAFQABlIAaABuAAZqAFIAUwAGYwBq" +
        "AFMABmYAfwBjAAZmAGMAVAAGbgB2AGgABlIAdgBuAAZoAFYAfwAGaAB/AGYABnYAagBjAAYfABoAFAAAFgAfABUAAAMAFQAfAAAX" +
        "ABwAGQAAGgAZABQAABQAEwAEAAsUAAQAAgALGAACAAQACxwAHQAPAAscAA8AEwALEQAdABwAABEAHAAYAAsEAAUAEQALBAARABgA" +
        "CxMADwAFAAsTAAUABAALYQDiAF8ABGwAXwB+AATiAH4AXwAEVQBRAGUABV8AYQBgAARkAO8B4QAF4QDvAe4BA2wAbQB0AARsAHQA" +
        "cAAFZwBtAGwABFEAZwBsAARRAHcAZwAEaQB3AFEABWkAUQBVAAVvAFYAVQAGpQAuACwACKUALAAtAAgtACwAKwAJLQArADAACTEA" +
        "MAArAAkyADEAKwAJMgArACQACZMAggCIAAHzAPUA9gAA9ADzAPYAAPkA9AD4AADzAPQA+QAA+AD6APkAAPYA+wD0AAD4APQA+wAA" +
        "jwAFAf0AAPgAkAD6AACQANsA/QAAkAD9APoAAPgA/wCQAACQAP8AngAA/wAAAZ4AAKAAngAAAQD7APYA9QAA+wD1AAMBAAEB+wAC" +
        "AQACAfsAAwEA/wD4AAABAPsA/wD4AAABAf8A+wAAAgEAAf8AAAEBAgH/AACOAIQABQEBjgAFAY8AAY4A/QCPAAD9AOsAjwAA2wDr" +
        "AP0AAJAAmACUAACeAJgAkAAAcwBcAFcAAAYBCAEJAQAOAQ8BEAELDgEQAdYBCw8BDgERAQsPAREBEgELDAHfAeABCwwBCwEXAQsM" +
        "ARcBGAELJQEIARoBAAYBHQEeAQAGAR4BCAEAHQEiAR4BCyABHwEdAQAHAQYBCQEABwEJARsBAAcBGwEhAQAgARwBJQEABwEkASYB" +
        "ADABLwEyAQk1ATIBLwEJLwE5AToBCS8BOgE1AQosAS4B2wEJLAHbAdwBCTsBOgE8AQo7ATwBPQEKOgE5AT4BCToBPgE8AQo1AT8B" +
        "MgEIQAE0AT8BCD8BNQE6AQo/AToBOwEKQgFDAUQBCUIBPgHaAQnaAdsBLgEJ2gEuAUIBCS0BQwFCAQlKAUwBSwEJQQFNAUwBCkEB" +
        "TAFKAQpNAT0BTgEKTAFNAU4BCkwBTgFJAQpAAUEBSgEIQAFKASgBCCgBTwFAAQghASYBBwEAGgEZASIBCxoBIgEfAQvfAQwBGAEL" +
        "UQFTARQBC1EBFAEWAQtTAVEBRwEAVAFTARgBAFABRwFGAQBIAVUBDwELSAEPARIBC0UBRAFHAQBFAUYBRwEAIwFUARgBABMBFAFT" +
        "AQsQAVYBUgELEAFSARYBC1IBUQHeAQBSAVEBFgELZgGEAfgBA2YB+AFyAQRyAWUBZgEE+QF2AXIBBfkBcgH4AQNcAYYBcwEGXAFz" +
        "AXoBBmgBbAF3AQZoAWwBbgEGXwFjAX4BAGEBXgFiAQDRAWcBgQEDgAFqAfkBA1oBbAFoAQaGAW0BcwEGXAF6AXUBBmYBZQGCAQNx" +
        "AWMBWAEGgwGAAYEBA2sBgwGBAQVvAX0BaQEGbwFpAXwBBmABYQFiAQCGAWkBfQEGhgF9AW0BBmIBeQF4AQBfAX4BXgEAZwHSAVcB" +
        "BFsBagFrAQWNAJEBkgEBjQCSAYwBAYQAjQCMAQGEAIwBiwEBhQCKAY4BAZEBjQCFAAGRAYUAjgEBkgGRAY4BAY4BigGMAQGOAYwB" +
        "kgEBkwGVAY0BA5ABjwGUAQAFAYQAiwEBBQGLAdkBAZkByAGCAQOZAYIBgQEDgQGAAdEBA54BKgE4AQmgATQBnwEInwFAAdQBCMAB" +
        "ngHHAQeyAKcBTwEIqAGfAdQBCLUAqQGrAAKpAaoBpAECqQGkAaMBAqoBqwGlAQKqAaUBpAECqwGmAaUBB74BvQG8AQe+AcYBvQEH" +
        "qwCtAacBCKsApwGyAAjVAaMBqAEIowGkAaABCKMBoAGfAQikAaUBMwEIpAEzAaABCKIBoQGqAQKiAaoBqQECoQGHAasBAqEBqwGq" +
        "AQKsAYcBmwEHrAGbAbsBB9MBvAG7AQfTAcIBvgEH0wG+AbwBB7oBhwGzAQfMAZwBlwEAsQGZAdEBA7EB0QGAAQOVAbMBtAEClQG0" +
        "AXsBAn8BtAG2AQLGAMUAyQACxgDJAMgAArQBswGhAQLXAbQBoQEC1wGhAaIBAskAtwHKAAKhAbMBhwECygC3AacAAsoApwCoAALM" +
        "ALUBxgACxgC1AcUAAssBsAGdAQL6AfsBmAEDggH8AWUBA4AB+QH9AQOWAcUBuwEBngHAAb8BCZ4BvwEnAQm/AcAB1QAJiAGXAKYA" +
        "AYgBpgDBAQfCAcEBpgAHsQDEAcMBB7EAwwG6AAejAMABxwEH1QDAAaMAB3gBXQFiAQBdAXgBeQEAZAFjAXkBAGQBeQF4AQAHASQB" +
        "IAEAIAEGAQcBAAYBIAEdAQC7AcUBiAEBuwGIAcEBB74BwgHDAQfDAcQBxgEHwwHGAb4BB8QBsQCjAAfEAaMAxwEH/gH8AYIBA4kB" +
        "jAGKAQHIAfsBggEDjwHJAZMBA/sB/gGCAQPdAX8BewEDkAHKAY8BAK8BuQGwAQCvAbABywEAmgHIAZkBA5gB+wHIAQPZAYsBygEA" +
        "vQCuAcwAAr0AzADLAAKwAZwBzAEAsAG5AZwBAM0BuQGvAQDLAc4BzQEAywHNAa8BAL0AnwDOAQC9AM4BrgEAzQGcAbkBAM4BzwHN" +
        "AQDOAZ8A0AEAnwCgANABAP0B/wGxAQP9AbEBgAEDdgFqAVsBBW4BXAF8AQZYAWMBZAEGlwGyAbABA5cBsAHMAQP/AQACmQEDsQH/" +
        "AZkBA2sBagGAAQVrAYABgwEFbwF8AVwBBm8BXAFbAQZYAXwBcAEGAAIBApoBA5kBAAKaAQPRAYEBmQEDZwFrAYEBBGsBZwFXAQRx" +
        "AVgBWQEGfgFxAVkBBn8BewGdAQOdAX8B3QEDagF2AfkBBVsBdQF6AQVbAXoBdgEFdAFoAW4BBlgBaAF0AQaaAQEC+gEDmgH6AZgB" +
        "A8gBmgGYAQOCAWYBZwEDggFnAdEBA1oBWQFpAQZyAVcB0gEEcgHSAYUBBFkBaAFeAQZlAfwB+AEDZQH4AYQBA2YBZQGEAQRiAV4B" +
        "ZAEAYgFkAXgBAHkBYwFdAQBjAV8BXQEAXgFdAV8BAF4BYgFdAQC7AcEBwgEHuwHCAdMBB9YAwgGmAAfHAZ4BxgEHxwHGAcQBB54B" +
        "vQHGAQeeAScBKgEJKQEqAScBCSkBJwErAQktASkBKwEJRwFEAUgBAEcBSAESAQDYASQBIAEAGwEcASEBAFYBEAEPAQtWAQ8BVQEL" +
        "1AFAAU8BCKcBqAHUAQinAdQBTwEIrQHVAagBCK0BqAGnAQirAKMB1QEIqwDVAa0BCKkBowGrAAKnAKIBqQECpwCpAbUAArcBogGn" +
        "AALJALYBogECyQCiAbcBAsUAtQG2AQLFALYByQACuAG1AcwAAq4BywG4AQKuAbgBzAACrgHOAcsBAM4B0AHPAQAIASUBCQEAOwE9" +
        "AU0BCj8BOwFNAQo/AU0BQQEKQAE/AUEBCJ8BNAFAAQijAZ8BqAEItgHXAaIBArUBfwG2AQKdAX8BtQECnQG1AbgBAssBnQG4AQLN" +
        "Ac8BnAEAzwHQAZwBABMBFQELAQsTAQsBDAELFAETAQwBC9YBEAEWAQvWARYBFAELMgE/ATQBCH8BewG0AQImASEBJAEAHAEgASEB" +
        "ACEBIAEkAQBQAUcBEgEAUAESAREBAC0BTAFJAQktAUkBQwEJLQFLAUwBCSsBSgFLAQkrAUsBLQEJKwEnASgBCSsBKAFKAQm6AMMB" +
        "wgEHugDCAdYAB48BygGLAQDJAY8BiwEBkwHJAYsBAYkBugGzAQGJAbMBlQEBugGJAYoBAbYBtAHXAQJHAVABUwEAUgFFAUYBAFIB" +
        "RgFHAQCHAboBmwEHkwGLAYwBARgBFwEjAQAtAUIBLgEJLAEpAS0BCSwBLQEuAQkqASkBLAEJKgEsATEBCZ4BOAE3AQmeATcBNgEJ" +
        "pgG9AZ4BB6YBngE2AQesAbwBvQEHrAG9AaYBB7sBvAGsAQebAboBlgEBmwGWAbsBB4oBlgG6AQEVARMBVAELFQFUASMBC1MBVAET" +
        "AQtRAVIBRwEA2gE8AT4BCT4BOQHbAQk+AdsB2gEJ3AHbATkBCdwBOQEvAQksAdwBLwEJMQEsAS8BCTEBLwEwAQkyATEBMAEJpgE2" +
        "ATMBB6YBMwGlAQesAaYBqwEHhwGsAasBB5MBiQGVAQN7AY0BlQEDnQF7Ad0BA4EBggHRAQNEAUUBQgEJSAFEAUMBCUkBSAFDAQlJ" +
        "AU4BVQEKSQFVAUgBCk4BPQFWAQpOAVYBVQEKPQE8AVIBCj0BUgFWAQo+AVIBPAEJRQFSAT4BCUUBPgFCAQkNAdYBFAELDgHWAQ0B" +
        "CxgBUwFQAQARAd8BGAEAEQEYAVABAOAB3wERAQvgAREBDgELDAHgAQ4BCwwBDgENAQsNAQwB1gELFAEMAQ0BC14BaAFkAQZ+AVkB" +
        "XgEGYwFxAX4BBlgBZAFoAQZZAVoBaAEGWAF0AW4BBnABWQFYAQZpAVkBcAEGbAFaAWkBBmwBaQGGAQZ0AW4BfAEGWAF0AXwBBm4B" +
        "bAGGAQZuAYYBXAEGfAFpAXABBiUBGgEgAQAcARsBJQEACQElARsBAB0BHwEiAQAgARoBHwEAGgEIAQoBCxoBCgEZAQseAQoBCAEL" +
        "IgEZARUBCyIBFQEjAQsXAR4BIgELFwEiASMBAAoBHgEXAQsKARcBCwELGQEKAQsBCxkBCwEVAQtnAWUB0gEEcgGFAWUBBNIBZQGF" +
        "AQRbAWsBVwEFZQFmAWcBBHIBdgF6AQVyAXoBcwEEbQFyAXMBBFcBcgFtAQRXAW0BfQEEbwFbAVcBBW8BVwF9AQV1AVsBXAEGoAEz" +
        "ATIBCKABMgE0AQgzATYBMQEJMwExATIBCTcBMQE2AQk4ASoBMQEJOAExATcBCZMBjAGJAQH3AOMB4gEA9wDiAfUAAPcA9QDzAADh" +
        "AeMB9wAA9wDzAPkAAPcA+QD8AAD8AOQB4QEA9wD8AOEBAOQB/AD+AADjAeEB5QEA5AHlAeEBAAUB5gH9AAD8APkA+gAA/AD6AP4A" +
        "AOQB/gCQAQCQAf4A5gEAkAHmAcoBAOQBkAHnAQCQAZwB5wEA5wGcAegBANAB6AGcAQCgAAAB6AEAoADoAdABAOUB4gHjAQDrAQMB" +
        "BAEA5QHrAeIBAOsBAwH1AADrAfUA4gEA6AEAAQIBAOgBAgHqAQDpAeoB5QEA6gHrAeUBAOoBAgEDAQDqAQMB6wEA5wHoAeQBAOUB" +
        "5AHnAQDpAeUB5wEA6gHnAegBAOkB5wHqAQCEAOYBBQEA5gEFAdkBAMoB5gHZAQCQAZQBlwEAnAGQAZcBAP0A5gH+AAD9AP4A+gAA" +
        "eQFdAWIBAO0B7AH6AADtAfoA/gAA7AHtAfcAAOwB9wDzAACBANcAlgABiAGWAcUBAZYAiwCGAAHjAIUAgwAB4wCDAOQAAeQAgwCG" +
        "AAHkAIsAhgAB5ADXAIEAAeMAigGOAQHjAIoBhQABlgGKAY4BAY4B4wDFAQGOAcUBlgEB1wDkAIsAAdcAiwCWAAGXAIMA5AABlwDk" +
        "AIEAAeMAgwCXAAHjAJcAiAEB4wCIAcUBAQ=="
}
