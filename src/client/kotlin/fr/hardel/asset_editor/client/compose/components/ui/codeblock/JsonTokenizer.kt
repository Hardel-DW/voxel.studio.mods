package fr.hardel.asset_editor.client.compose.components.ui.codeblock

enum class JsonTokenType { STRING, NUMBER, BOOLEAN, NULL, PROPERTY, PUNCTUATION, WHITESPACE }

data class JsonToken(val type: JsonTokenType, val start: Int, val end: Int)

fun tokenizeJson(source: String): List<JsonToken> {
    val tokens = mutableListOf<JsonToken>()
    var i = 0

    while (i < source.length) {
        val ch = source[i]

        if (ch.isWhitespace()) {
            val start = i
            while (i < source.length && source[i].isWhitespace()) i++
            tokens += JsonToken(JsonTokenType.WHITESPACE, start, i)
            continue
        }

        if (ch == '"') {
            val start = i
            i++
            while (i < source.length && source[i] != '"') {
                if (source[i] == '\\') { i++; if (i < source.length) i++ }
                else i++
            }
            if (i < source.length) i++

            var j = i
            while (j < source.length && source[j].isWhitespace()) j++
            val isProperty = j < source.length && source[j] == ':'

            tokens += JsonToken(if (isProperty) JsonTokenType.PROPERTY else JsonTokenType.STRING, start, i)
            continue
        }

        if (ch in "{}[]:,") {
            tokens += JsonToken(JsonTokenType.PUNCTUATION, i, i + 1)
            i++
            continue
        }

        if (ch.isDigit() || ch == '-') {
            val start = i
            while (i < source.length && source[i].isNumberChar()) i++
            tokens += JsonToken(JsonTokenType.NUMBER, start, i)
            continue
        }

        if (source.startsWith("true", i)) { tokens += JsonToken(JsonTokenType.BOOLEAN, i, i + 4); i += 4; continue }
        if (source.startsWith("false", i)) { tokens += JsonToken(JsonTokenType.BOOLEAN, i, i + 5); i += 5; continue }
        if (source.startsWith("null", i)) { tokens += JsonToken(JsonTokenType.NULL, i, i + 4); i += 4; continue }

        tokens += JsonToken(JsonTokenType.PUNCTUATION, i, i + 1)
        i++
    }

    return tokens
}

private fun Char.isNumberChar() = isDigit() || this in ".eE+-"