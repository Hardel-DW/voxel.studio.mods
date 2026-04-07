package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import androidx.compose.ui.graphics.Color
import fr.hardel.asset_editor.client.compose.lib.highlight.Highlight
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightPalette
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightRegistry
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightStyle
import fr.hardel.asset_editor.client.compose.lib.utils.JsonTokenizer

class JsonCodeBlockHighlighter : CodeBlockHighlighter {

    override fun apply(text: String, registry: HighlightRegistry) {
        val highlightsByType = LinkedHashMap<JsonTokenizer.TokenType, Highlight>()

        for (token in JsonTokenizer.tokenize(text)) {
            if (token.type == JsonTokenizer.TokenType.WHITESPACE) {
                continue
            }

            val highlight = highlightsByType.getOrPut(token.type) { Highlight() }
            highlight.add(token.start, token.end)
        }

        for ((type, highlight) in highlightsByType) {
            val name = type.highlightName ?: continue
            registry.set(name, highlight)
        }
    }

    companion object {
        const val STRING = "json-string"
        const val NUMBER = "json-number"
        const val BOOLEAN = "json-boolean"
        const val NULL = "json-null"
        const val PROPERTY = "json-property"
        const val PUNCTUATION = "json-punctuation"

        fun installDefaultPalette(palette: HighlightPalette) {
            palette.set(STRING, HighlightStyle.foreground(Color(0xFF98C379)))
            palette.set(NUMBER, HighlightStyle.foreground(Color(0xFFD19A66)))
            palette.set(BOOLEAN, HighlightStyle.foreground(Color(0xFF56B6C2)))
            palette.set(NULL, HighlightStyle.foreground(Color(0xFFC678DD)))
            palette.set(PROPERTY, HighlightStyle.foreground(Color(0xFF61AFEF)))
            palette.set(PUNCTUATION, HighlightStyle.foreground(Color(0xFFABB2BF)))
        }
    }
}
