package fr.hardel.asset_editor.client.compose.lib.utils

object JsonTokenizer {

    enum class TokenType(val highlightName: String?) {
        STRING("json-string"),
        NUMBER("json-number"),
        BOOLEAN("json-boolean"),
        NULL("json-null"),
        PROPERTY("json-property"),
        PUNCTUATION("json-punctuation"),
        WHITESPACE(null)
    }

    data class Token(
        val type: TokenType,
        val value: String,
        val start: Int,
        val end: Int
    )

    fun tokenize(json: String?): List<Token> {
        val source = json ?: ""
        val tokens = ArrayList<Token>()
        var index = 0

        while (index < source.length) {
            val ch = source[index]

            if (ch.isWhitespace()) {
                val start = index
                while (index < source.length && source[index].isWhitespace()) {
                    index++
                }
                tokens += Token(TokenType.WHITESPACE, source.substring(start, index), start, index)
                continue
            }

            if (ch == '"') {
                val start = index
                index++
                var isProperty = false

                while (index < source.length && source[index] != '"') {
                    if (source[index] == '\\') {
                        index++
                        if (index < source.length) {
                            index++
                        }
                    } else {
                        index++
                    }
                }

                if (index < source.length) {
                    index++
                }

                var lookahead = index
                while (lookahead < source.length && source[lookahead].isWhitespace()) {
                    lookahead++
                }
                if (lookahead < source.length && source[lookahead] == ':') {
                    isProperty = true
                }

                tokens += Token(
                    if (isProperty) TokenType.PROPERTY else TokenType.STRING,
                    source.substring(start, index),
                    start,
                    index
                )
                continue
            }

            if ("{}[]:,".indexOf(ch) >= 0) {
                tokens += Token(TokenType.PUNCTUATION, ch.toString(), index, index + 1)
                index++
                continue
            }

            if (ch.isDigit() || ch == '-') {
                val start = index
                while (index < source.length && isNumberChar(source[index])) {
                    index++
                }
                tokens += Token(TokenType.NUMBER, source.substring(start, index), start, index)
                continue
            }

            if (source.startsWith("true", index)) {
                tokens += Token(TokenType.BOOLEAN, "true", index, index + 4)
                index += 4
                continue
            }

            if (source.startsWith("false", index)) {
                tokens += Token(TokenType.BOOLEAN, "false", index, index + 5)
                index += 5
                continue
            }

            if (source.startsWith("null", index)) {
                tokens += Token(TokenType.NULL, "null", index, index + 4)
                index += 4
                continue
            }

            tokens += Token(TokenType.PUNCTUATION, ch.toString(), index, index + 1)
            index++
        }

        return tokens.toList()
    }

    private fun isNumberChar(ch: Char): Boolean =
        ch.isDigit() || ch == '.' || ch == 'e' || ch == 'E' || ch == '+' || ch == '-'
}
