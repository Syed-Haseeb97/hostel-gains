package com.hostelgains.domain.nutrition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Production AI nutrition parser utilizing Google Gemini REST API with robust offline
 * fallback to [FixtureNutritionParser].
 *
 * Implements resilient parsing:
 * 1. Validates input text for emptiness and whitespace.
 * 2. Checks if [apiKey] is configured. If missing or blank, seamlessly falls back to [fallbackParser].
 * 3. Calls Gemini REST API using [client] with structured JSON output request.
 * 4. Extracts candidates, strips optional markdown fences, and parses macronutrient fields.
 * 5. On any network timeout, HTTP error, or parsing exception, gracefully falls back to [fallbackParser].
 */
class GeminiNutritionParser(
    private val apiKey: String? = null,
    private val apiKeyProvider: (() -> String?)? = null,
    private val client: OkHttpClient = defaultClient,
    private val fallbackParser: NutritionParser = FixtureNutritionParser(),
    private val baseUrl: String = DEFAULT_BASE_URL
) : NutritionParser {

    companion object {
        const val DEFAULT_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

        val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
        }
    }

    override suspend fun parseMeal(rawDescription: String): Result<MealNutrition> {
        val trimmed = rawDescription.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Meal description cannot be empty or whitespace"))
        }

        // Check apiKey parameter, dynamic apiKeyProvider lambda, or environment variable
        val key = (apiKey ?: apiKeyProvider?.invoke() ?: System.getenv("GEMINI_API_KEY"))?.trim()
        if (key.isNullOrEmpty()) {
            return fallbackParser.parseMeal(rawDescription)
        }

        return withContext(Dispatchers.IO) {
            try {
                executeGeminiRequest(rawDescription, key)
            } catch (e: Exception) {
                // Graceful fallback on network failure, socket timeout, or unexpected error
                fallbackParser.parseMeal(rawDescription)
            }
        }
    }

    private suspend fun executeGeminiRequest(rawDescription: String, key: String): Result<MealNutrition> {
        val prompt = buildPrompt(rawDescription)
        val payload = buildRequestBodyJson(prompt)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = payload.toRequestBody(mediaType)

        val targetUrl = if (baseUrl.contains("key=")) baseUrl else {
            val delimiter = if (baseUrl.contains("?")) "&" else "?"
            "$baseUrl${delimiter}key=$key"
        }

        val request = Request.Builder()
            .url(targetUrl)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", key)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                // Non-200 HTTP code -> fall back gracefully
                return fallbackParser.parseMeal(rawDescription)
            }

            val responseBody = response.body.string()
            if (responseBody.isNullOrBlank()) {
                return fallbackParser.parseMeal(rawDescription)
            }

            val parsedMeal = parseGeminiResponse(responseBody, rawDescription)
            return if (parsedMeal != null) {
                Result.success(parsedMeal)
            } else {
                fallbackParser.parseMeal(rawDescription)
            }
        }
    }

    private fun buildPrompt(rawDescription: String): String {
        return "You are an expert sports nutritionist specialized in Indian university hostel mess diets.\n" +
                "Analyze the student's logged meal: \"$rawDescription\".\n" +
                "Note: Input may contain typos, misspellings, or phonetic Indian English / Hinglish slang (e.g. 'tee' for tea, 'chiken' for chicken, 'omlet' for omelette, 'roty' for roti, 'panir' for paneer, 'biskit' for biscuit, 'maggi' for noodles). Understand the user's intent, correct typos automatically, and calculate realistic estimated macronutrients: protein (g), carbohydrates (g), fats (g), and total calories.\n" +
                "Rules:\n" +
                "- isHighCarb = true if total carbohydrates >= 60g.\n" +
                "- isLowProtein = true if total protein < 20g.\n" +
                "Return strictly a JSON object with fields: " +
                "mealName (string), proteinGrams (float), carbsGrams (float), fatsGrams (float), calories (int), " +
                "isHighCarb (boolean), isLowProtein (boolean), dietaryNotes (string), " +
                "items (array of objects with name, quantityDescription, proteinGrams, carbsGrams, fatsGrams, calories)."
    }

    private fun buildRequestBodyJson(prompt: String): String {
        val escapedPrompt = escapeJsonString(prompt)
        return """
            {
              "contents": [
                {
                  "parts": [
                    { "text": "$escapedPrompt" }
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

    private fun parseGeminiResponse(responseJson: String, originalQuery: String): MealNutrition? {
        val rootNode = SimpleJsonParser.parse(responseJson) ?: return null

        // Navigate: candidates[0].content.parts[0].text
        val candidates = (rootNode["candidates"] as? SimpleJsonParser.Node.ArrayNode)?.list ?: return null
        if (candidates.isEmpty()) return null

        val firstCandidate = candidates[0] as? SimpleJsonParser.Node.ObjectNode ?: return null
        val content = firstCandidate["content"] as? SimpleJsonParser.Node.ObjectNode ?: return null
        val parts = (content["parts"] as? SimpleJsonParser.Node.ArrayNode)?.list ?: return null
        if (parts.isEmpty()) return null

        val firstPart = parts[0] as? SimpleJsonParser.Node.ObjectNode ?: return null
        val rawText = (firstPart["text"] as? SimpleJsonParser.Node.StringNode)?.value ?: return null

        // Clean optional markdown code blocks (e.g. ```json ... ```)
        val cleanedText = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val mealNode = SimpleJsonParser.parse(cleanedText) as? SimpleJsonParser.Node.ObjectNode ?: return null

        val mealName = (mealNode["mealName"] as? SimpleJsonParser.Node.StringNode)?.value
            ?: "Parsed Meal: $originalQuery"
        val protein = (mealNode["proteinGrams"] as? SimpleJsonParser.Node.NumberNode)?.value?.toFloat() ?: 0.0f
        val carbs = (mealNode["carbsGrams"] as? SimpleJsonParser.Node.NumberNode)?.value?.toFloat() ?: 0.0f
        val fats = (mealNode["fatsGrams"] as? SimpleJsonParser.Node.NumberNode)?.value?.toFloat() ?: 0.0f
        val calories = (mealNode["calories"] as? SimpleJsonParser.Node.NumberNode)?.value?.toInt()
            ?: (protein * 4 + carbs * 4 + fats * 9).toInt()

        val isHighCarb = (mealNode["isHighCarb"] as? SimpleJsonParser.Node.BooleanNode)?.value
            ?: (carbs >= 60.0f)
        val isLowProtein = (mealNode["isLowProtein"] as? SimpleJsonParser.Node.BooleanNode)?.value
            ?: (protein < 20.0f)
        val dietaryNotes = (mealNode["dietaryNotes"] as? SimpleJsonParser.Node.StringNode)?.value ?: ""

        val items = mutableListOf<FoodItemNutrition>()
        val itemsNode = (mealNode["items"] as? SimpleJsonParser.Node.ArrayNode)?.list
        if (itemsNode != null) {
            for (elem in itemsNode) {
                if (elem is SimpleJsonParser.Node.ObjectNode) {
                    val name = (elem["name"] as? SimpleJsonParser.Node.StringNode)?.value ?: "Item"
                    val qty = (elem["quantityDescription"] as? SimpleJsonParser.Node.StringNode)?.value ?: "1 serving"
                    val p = (elem["proteinGrams"] as? SimpleJsonParser.Node.NumberNode)?.value?.toFloat() ?: 0.0f
                    val c = (elem["carbsGrams"] as? SimpleJsonParser.Node.NumberNode)?.value?.toFloat() ?: 0.0f
                    val f = (elem["fatsGrams"] as? SimpleJsonParser.Node.NumberNode)?.value?.toFloat() ?: 0.0f
                    val cal = (elem["calories"] as? SimpleJsonParser.Node.NumberNode)?.value?.toInt()
                        ?: (p * 4 + c * 4 + f * 9).toInt()
                    items.add(FoodItemNutrition(name, qty, p, c, f, cal))
                }
            }
        }

        return MealNutrition(
            rawQuery = originalQuery,
            mealName = mealName,
            proteinGrams = protein,
            carbsGrams = carbs,
            fatsGrams = fats,
            calories = calories,
            items = items,
            isHighCarb = isHighCarb,
            isLowProtein = isLowProtein,
            dietaryNotes = dietaryNotes
        )
    }

    /**
     * Pure Kotlin lightweight JSON parser.
     * Operates completely independent of Android framework stubs or external libraries.
     */
    internal object SimpleJsonParser {

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
                val parser = Parser(json)
                parser.parseValue()
            } catch (e: Exception) {
                null
            }
        }

        private class Parser(private val src: String) {
            private var pos = 0

            fun parseValue(): Node? {
                skipWhitespace()
                if (pos >= src.length) return null

                return when (val c = src[pos]) {
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
                pos++ // consume initial '"'
                val sb = StringBuilder()
                while (pos < src.length) {
                    val c = src[pos++]
                    if (c == '"') {
                        break
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
                                    sb.append(hex.toInt(16).toChar())
                                    pos += 4
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
                while (pos < src.length && (src[pos] in '0'..'9' || src[pos] == '.' || src[pos] == 'e' || src[pos] == 'E' || src[pos] == '+' || src[pos] == '-')) {
                    pos++
                }
                val numStr = src.substring(start, pos)
                val d = numStr.toDoubleOrNull() ?: 0.0
                return Node.NumberNode(d)
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
                while (pos < src.length && (src[pos] == ' ' || src[pos] == '\t' || src[pos] == '\n' || src[pos] == '\r')) {
                    pos++
                }
            }
        }
    }
}
