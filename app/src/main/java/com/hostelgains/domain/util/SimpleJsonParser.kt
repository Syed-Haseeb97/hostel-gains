package com.hostelgains.domain.util

/**
 * Lightweight, zero-dependency JSON parser built in pure Kotlin.
 * Works seamlessly in both local JVM unit tests (where Android SDK's org.json is stubbed)
 * and the Android runtime.
 */
object SimpleJsonParser {

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
            val parser = Parser(json.trim())
            parser.parseValue()
        } catch (e: Exception) {
            null
        }
    }

    fun escape(raw: String): String {
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
                skipWhitespace()
                val elem = parseValue()
                if (elem != null) {
                    list.add(elem)
                }
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
