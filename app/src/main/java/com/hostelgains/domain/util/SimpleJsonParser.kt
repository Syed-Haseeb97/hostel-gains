package com.hostelgains.domain.util

/**
 * Lightweight, zero-dependency JSON parser built in pure Kotlin.
 * Works seamlessly in both local JVM unit tests (where Android SDK's org.json is stubbed)
 * and the Android runtime.
 *
 * Security hardening:
 * - Enforces [MAX_DEPTH] recursion limit to prevent StackOverflowError on deeply nested payloads.
 * - Provides structured Gemini envelope unwrapping and safe JSON request serialization.
 */
object SimpleJsonParser {

    const val MAX_DEPTH = 32

    sealed class Node {
        data class ObjectNode(val map: Map<String, Node>) : Node() {
            override operator fun get(key: String): Node? = map[key]

            fun optString(key: String, default: String = ""): String =
                (map[key] as? StringNode)?.value ?: default

            fun optDouble(key: String, default: Double = 0.0): Double =
                (map[key] as? NumberNode)?.value?.takeIf { it.isFinite() } ?: default

            fun optFloat(key: String, default: Float = 0f): Float =
                (map[key] as? NumberNode)?.value?.toFloat()?.takeIf { it.isFinite() } ?: default

            fun optInt(key: String, default: Int = 0): Int =
                (map[key] as? NumberNode)?.value?.takeIf { it.isFinite() }?.toInt() ?: default

            fun optBoolean(key: String, default: Boolean = false): Boolean =
                (map[key] as? BooleanNode)?.value ?: default
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
            val trimmed = json.trim()
            if (trimmed.isEmpty()) return null
            val parser = Parser(trimmed)
            parser.parseValue(depth = 0)
        } catch (e: Exception) {
            null
        } catch (e: StackOverflowError) {
            null
        }
    }

    /**
     * Strips optional markdown code fences (e.g. ```json ... ```) from LLM text responses.
     */
    fun stripMarkdownFences(raw: String): String {
        return raw.trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    /**
     * Extracts `candidates[0].content.parts[0].text` from a standard Gemini REST API response
     * and strips any markdown code fences.
     */
    fun extractGeminiCandidateText(responseJson: String): String? {
        val rootNode = parse(responseJson) as? Node.ObjectNode ?: return null
        val candidates = (rootNode["candidates"] as? Node.ArrayNode)?.list ?: return null
        if (candidates.isEmpty()) return null

        val firstCandidate = candidates[0] as? Node.ObjectNode ?: return null
        val content = firstCandidate["content"] as? Node.ObjectNode ?: return null
        val parts = (content["parts"] as? Node.ArrayNode)?.list ?: return null
        if (parts.isEmpty()) return null

        val firstPart = parts[0] as? Node.ObjectNode ?: return null
        val rawText = (firstPart["text"] as? Node.StringNode)?.value ?: return null
        return stripMarkdownFences(rawText)
    }

    /**
     * Builds a Gemini `generateContent` JSON request body isolating system instructions
     * from untrusted user text to mitigate prompt injection.
     */
    fun buildGeminiRequestJson(
        userText: String,
        systemInstruction: String? = null,
        temperature: Double? = null
    ): String {
        val escapedUser = escape(userText)
        val sb = StringBuilder()
        sb.append("{\n")
        if (!systemInstruction.isNullOrBlank()) {
            val escapedSys = escape(systemInstruction)
            sb.append("  \"systemInstruction\": {\n")
            sb.append("    \"parts\": [{ \"text\": \"").append(escapedSys).append("\" }]\n")
            sb.append("  },\n")
        }
        sb.append("  \"contents\": [\n")
        sb.append("    {\n")
        sb.append("      \"role\": \"user\",\n")
        sb.append("      \"parts\": [\n")
        sb.append("        { \"text\": \"").append(escapedUser).append("\" }\n")
        sb.append("      ]\n")
        sb.append("    }\n")
        sb.append("  ],\n")
        sb.append("  \"generationConfig\": {\n")
        if (temperature != null) {
            sb.append("    \"temperature\": ").append(temperature).append(",\n")
        }
        sb.append("    \"responseMimeType\": \"application/json\"\n")
        sb.append("  }\n")
        sb.append("}")
        return sb.toString()
    }

    fun escape(raw: String): String {
        val sb = StringBuilder(raw.length + 16)
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

    private class Parser(private val src: String) {
        private var pos = 0

        fun parseValue(depth: Int): Node? {
            if (depth > MAX_DEPTH) {
                throw IllegalArgumentException("JSON nesting exceeds maximum depth of $MAX_DEPTH")
            }
            skipWhitespace()
            if (pos >= src.length) return null

            return when (src[pos]) {
                '{' -> parseObject(depth + 1)
                '[' -> parseArray(depth + 1)
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                '-', in '0'..'9' -> parseNumber()
                else -> null
            }
        }

        private fun parseObject(depth: Int): Node.ObjectNode? {
            val map = mutableMapOf<String, Node>()
            pos++ // consume '{'
            skipWhitespace()
            if (pos < src.length && src[pos] == '}') {
                pos++
                return Node.ObjectNode(map)
            }

            var closed = false
            while (pos < src.length) {
                skipWhitespace()
                if (pos < src.length && src[pos] == '}') {
                    pos++
                    closed = true
                    break
                }
                if (pos >= src.length || src[pos] != '"') break
                val key = parseString().value
                skipWhitespace()
                if (pos < src.length && src[pos] == ':') {
                    pos++
                } else {
                    return null
                }
                val value = parseValue(depth) ?: return null
                map[key] = value

                skipWhitespace()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                } else if (pos < src.length && src[pos] == '}') {
                    pos++
                    closed = true
                    break
                } else {
                    break
                }
            }
            if (!closed) return null
            return Node.ObjectNode(map)
        }

        private fun parseArray(depth: Int): Node.ArrayNode? {
            val list = mutableListOf<Node>()
            pos++ // consume '['
            skipWhitespace()
            if (pos < src.length && src[pos] == ']') {
                pos++
                return Node.ArrayNode(list)
            }

            var closed = false
            while (pos < src.length) {
                skipWhitespace()
                if (pos < src.length && src[pos] == ']') {
                    pos++
                    closed = true
                    break
                }
                val elem = parseValue(depth) ?: return null
                list.add(elem)
                skipWhitespace()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                } else if (pos < src.length && src[pos] == ']') {
                    pos++
                    closed = true
                    break
                } else {
                    break
                }
            }
            if (!closed) return null
            return Node.ArrayNode(list)
        }

        private fun parseString(): Node.StringNode {
            pos++ // consume opening '"'
            val sb = StringBuilder()
            while (pos < src.length) {
                val c = src[pos++]
                if (c == '"') {
                    return Node.StringNode(sb.toString())
                }
                if (c == '\\' && pos < src.length) {
                    when (val esc = src[pos++]) {
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
                                val codePoint = hex.toIntOrNull(16)
                                if (codePoint != null) {
                                    sb.append(codePoint.toChar())
                                }
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
