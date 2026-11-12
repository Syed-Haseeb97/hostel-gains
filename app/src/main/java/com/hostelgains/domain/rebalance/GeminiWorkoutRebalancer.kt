package com.hostelgains.domain.rebalance

import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.workout.AdjustedWorkoutPlan
import com.hostelgains.domain.workout.WorkoutDayPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * AI-powered implementation of WorkoutRebalancer utilizing the Google Gemini REST API.
 * Takes the user's natural language skip report / reason and today's baseline WorkoutDayPlan,
 * asks Gemini for structured JSON volume redistribution, and gracefully falls back to
 * [OfflineWorkoutRebalancer] whenever offline, on rate limit, or if no API key is configured.
 */
class GeminiWorkoutRebalancer(
    private val apiKey: String? = null,
    private val apiKeyProvider: (() -> String?)? = null,
    private val client: OkHttpClient = defaultClient,
    private val fallback: WorkoutRebalancer = OfflineWorkoutRebalancer(),
    private val baseUrl: String = DEFAULT_BASE_URL
) : WorkoutRebalancer {

    companion object {
        const val DEFAULT_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

        val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
        }
    }

    override suspend fun rebalanceWorkout(
        dayPlan: WorkoutDayPlan,
        skippedExerciseId: String,
        userPrompt: String
    ): Result<RebalanceResult> {
        val key = (apiKey ?: apiKeyProvider?.invoke() ?: System.getenv("GEMINI_API_KEY"))?.trim()
        if (key.isNullOrEmpty()) {
            return fallback.rebalanceWorkout(dayPlan, skippedExerciseId, userPrompt)
        }

        return withContext(Dispatchers.IO) {
            try {
                executeGeminiRebalance(dayPlan, skippedExerciseId, userPrompt, key)
            } catch (e: Exception) {
                fallback.rebalanceWorkout(dayPlan, skippedExerciseId, userPrompt)
            }
        }
    }

    override fun harmonizeWithMacros(
        rebalancedPlan: WorkoutDayPlan,
        rebalanceResult: RebalanceResult?,
        dailyMacros: DailyMacroSummary
    ): AdjustedWorkoutPlan {
        return fallback.harmonizeWithMacros(rebalancedPlan, rebalanceResult, dailyMacros)
    }

    private suspend fun executeGeminiRebalance(
        dayPlan: WorkoutDayPlan,
        skippedExerciseId: String,
        userPrompt: String,
        key: String
    ): Result<RebalanceResult> {
        val prompt = buildPrompt(dayPlan, skippedExerciseId, userPrompt)
        val payload = buildRequestBodyJson(prompt)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = payload.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", key)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return fallback.rebalanceWorkout(dayPlan, skippedExerciseId, userPrompt)
            }

            val responseBody = response.body.string()
            if (responseBody.isNullOrBlank()) {
                return fallback.rebalanceWorkout(dayPlan, skippedExerciseId, userPrompt)
            }

            val parsed = parseRebalanceJsonResponse(responseBody)
            return if (parsed != null) {
                Result.success(parsed)
            } else {
                fallback.rebalanceWorkout(dayPlan, skippedExerciseId, userPrompt)
            }
        }
    }

    private fun buildPrompt(
        dayPlan: WorkoutDayPlan,
        skippedExerciseId: String,
        userPrompt: String
    ): String {
        val exercisesList = dayPlan.exercises.joinToString("; ") {
            "${it.id} (${it.name}, target: ${it.targetMuscle.name}, category: ${it.category.name}, baselineSets: ${it.baselineSets}, reps: ${it.repRange})"
        }

        return "You are an elite bodybuilding and biomechanics coach specializing in the V-Taper PPL framework in a university hostel gym setting.\n" +
                "Today's Plan (Day ${dayPlan.dayNumber}: ${dayPlan.dayName}): $exercisesList.\n" +
                "The student athlete reports: skippedExerciseId=\"$skippedExerciseId\", userReason=\"$userPrompt\".\n" +
                "Your objective: Dynamically redistribute the lost volume to 1 or 2 synergistic exercises within today's routine to protect weekly V-taper progression.\n" +
                "Return strictly a JSON object with this exact schema:\n" +
                "{\n" +
                "  \"skippedExerciseId\": \"string id of skipped exercise\",\n" +
                "  \"reason\": \"string concise summary of user reason or equipment issue\",\n" +
                "  \"compensations\": [\n" +
                "    {\n" +
                "      \"exerciseId\": \"string id of exercise getting extra sets\",\n" +
                "      \"extraSets\": 2,\n" +
                "      \"repAdjustment\": \"8-12\",\n" +
                "      \"rationale\": \"string biomechanical justification\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"coachMessage\": \"string motivating coach explanation\"\n" +
                "}"
    }

    private fun buildRequestBodyJson(prompt: String): String {
        val escaped = escapeJsonString(prompt)
        return """
            {
              "contents": [
                {
                  "parts": [
                    { "text": "$escaped" }
                  ]
                }
              ],
              "generationConfig": {
                "responseMimeType": "application/json"
              }
            }
        """.trimIndent()
    }

    private fun escapeJsonString(raw: String): String {
        val sb = StringBuilder()
        for (c in raw) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c.code < 0x20) {
                        sb.append(String.format("\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }

    /**
     * Parses a structured JSON response from Gemini (or direct JSON) into a [RebalanceResult].
     * Handles raw JSON, markdown-fenced blocks, and the standard Gemini response envelope.
     */
    fun parseRebalanceJsonResponse(jsonString: String): RebalanceResult? {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) return null

        val cleaned = trimmed
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val rootNode = RebalanceJsonParser.parse(cleaned) ?: return null

        // Check if this is a Gemini API response wrapper: candidates[0].content.parts[0].text
        val targetNode: RebalanceJsonParser.Node.ObjectNode = if (rootNode["candidates"] != null) {
            val candidates = (rootNode["candidates"] as? RebalanceJsonParser.Node.ArrayNode)?.list
            val firstCandidate = candidates?.firstOrNull() as? RebalanceJsonParser.Node.ObjectNode
            val content = firstCandidate?.get("content") as? RebalanceJsonParser.Node.ObjectNode
            val parts = (content?.get("parts") as? RebalanceJsonParser.Node.ArrayNode)?.list
            val firstPart = parts?.firstOrNull() as? RebalanceJsonParser.Node.ObjectNode
            val rawText = (firstPart?.get("text") as? RebalanceJsonParser.Node.StringNode)?.value ?: return null

            val innerCleaned = rawText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            (RebalanceJsonParser.parse(innerCleaned) as? RebalanceJsonParser.Node.ObjectNode) ?: return null
        } else {
            (rootNode as? RebalanceJsonParser.Node.ObjectNode) ?: return null
        }

        val skippedId = (targetNode["skippedExerciseId"] as? RebalanceJsonParser.Node.StringNode)?.value ?: ""
        val reason = (targetNode["reason"] as? RebalanceJsonParser.Node.StringNode)?.value ?: "Equipment unavailable"
        val coachMessage = (targetNode["coachMessage"] as? RebalanceJsonParser.Node.StringNode)?.value ?: ""

        val compensations = mutableListOf<ExerciseCompensation>()
        val compArray = (targetNode["compensations"] as? RebalanceJsonParser.Node.ArrayNode)?.list
        if (compArray != null) {
            for (elem in compArray) {
                if (elem is RebalanceJsonParser.Node.ObjectNode) {
                    val exId = (elem["exerciseId"] as? RebalanceJsonParser.Node.StringNode)?.value ?: continue
                    val extraSets = (elem["extraSets"] as? RebalanceJsonParser.Node.NumberNode)?.value?.toInt() ?: 2
                    val repAdj = (elem["repAdjustment"] as? RebalanceJsonParser.Node.StringNode)?.value ?: "8-12"
                    val rationale = (elem["rationale"] as? RebalanceJsonParser.Node.StringNode)?.value ?: "Maintains volume"
                    compensations.add(
                        ExerciseCompensation(
                            exerciseId = exId,
                            extraSets = extraSets,
                            repAdjustment = repAdj,
                            rationale = rationale
                        )
                    )
                }
            }
        }

        if (skippedId.isBlank() && compensations.isEmpty()) {
            return null
        }

        return RebalanceResult(
            skippedExerciseId = skippedId,
            reason = reason,
            compensations = compensations,
            coachMessage = coachMessage,
            isAiGenerated = true
        )
    }

    /**
     * Pure Kotlin lightweight JSON parser for JVM/Android test immunity.
     */
    internal object RebalanceJsonParser {
        sealed class Node {
            data class ObjectNode(val map: Map<String, Node>) : Node() {
                override operator fun get(key: String): Node? = map[key]
            }

            data class ArrayNode(val list: List<Node>) : Node() {
                operator fun get(index: Int): Node? = list.getOrNull(index)
            }

            data class StringNode(val value: String) : Node()
            data class NumberNode(val value: Double) : Node()
            data class BooleanNode(val value: Boolean) : Node()
            object NullNode : Node()

            open operator fun get(key: String): Node? = (this as? ObjectNode)?.map?.get(key)
        }

        fun parse(json: String): Node? {
            return try {
                Parser(json).parseValue()
            } catch (e: Exception) {
                null
            }
        }

        private class Parser(private val src: String) {
            private var pos = 0

            fun parseValue(): Node? {
                skipWhitespace()
                if (pos >= src.length) return null

                return when (src[pos]) {
                    '{' -> parseObject()
                    '[' -> parseArray()
                    '"' -> parseString()
                    't', 'f' -> parseBoolean()
                    'n' -> parseNull()
                    '-', in '0'..'9' -> parseNumber()
                    else -> null
                }
            }

            private fun parseObject(): Node.ObjectNode {
                val map = mutableMapOf<String, Node>()
                pos++ // consume '{'
                skipWhitespace()
                if (pos < src.length && src[pos] == '}') {
                    pos++
                    return Node.ObjectNode(map)
                }

                while (pos < src.length) {
                    skipWhitespace()
                    if (pos >= src.length || src[pos] != '"') break
                    val key = parseString().value
                    skipWhitespace()
                    if (pos < src.length && src[pos] == ':') {
                        pos++
                    }
                    val value = parseValue() ?: Node.NullNode
                    map[key] = value

                    skipWhitespace()
                    if (pos < src.length && src[pos] == ',') {
                        pos++
                    } else if (pos < src.length && src[pos] == '}') {
                        pos++
                        break
                    }
                }
                return Node.ObjectNode(map)
            }

            private fun parseArray(): Node.ArrayNode {
                val list = mutableListOf<Node>()
                pos++ // consume '['
                skipWhitespace()
                if (pos < src.length && src[pos] == ']') {
                    pos++
                    return Node.ArrayNode(list)
                }

                while (pos < src.length) {
                    val value = parseValue() ?: break
                    list.add(value)
                    skipWhitespace()
                    if (pos < src.length && src[pos] == ',') {
                        pos++
                    } else if (pos < src.length && src[pos] == ']') {
                        pos++
                        break
                    }
                }
                return Node.ArrayNode(list)
            }

            private fun parseString(): Node.StringNode {
                pos++ // consume opening '"'
                val sb = StringBuilder()
                while (pos < src.length) {
                    val c = src[pos++]
                    if (c == '"') {
                        return Node.StringNode(sb.toString())
                    } else if (c == '\\' && pos < src.length) {
                        val esc = src[pos++]
                        when (esc) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 <= src.length) {
                                    val hex = src.substring(pos, pos + 4)
                                    pos += 4
                                    sb.append(hex.toInt(16).toChar())
                                }
                            }
                            else -> sb.append(esc)
                        }
                    } else {
                        sb.append(c)
                    }
                }
                return Node.StringNode(sb.toString())
            }

            private fun parseNumber(): Node.NumberNode {
                val start = pos
                if (pos < src.length && src[pos] == '-') pos++
                while (pos < src.length && (src[pos].isDigit() || src[pos] == '.' || src[pos] == 'e' || src[pos] == 'E' || src[pos] == '+' || src[pos] == '-')) {
                    pos++
                }
                val numStr = src.substring(start, pos)
                return Node.NumberNode(numStr.toDoubleOrNull() ?: 0.0)
            }

            private fun parseBoolean(): Node.BooleanNode {
                return if (src.startsWith("true", pos)) {
                    pos += 4
                    Node.BooleanNode(true)
                } else if (src.startsWith("false", pos)) {
                    pos += 5
                    Node.BooleanNode(false)
                } else {
                    pos++
                    Node.BooleanNode(false)
                }
            }

            private fun parseNull(): Node.NullNode {
                if (src.startsWith("null", pos)) {
                    pos += 4
                } else {
                    pos++
                }
                return Node.NullNode
            }

            private fun skipWhitespace() {
                while (pos < src.length && src[pos].isWhitespace()) {
                    pos++
                }
            }
        }
    }
}
