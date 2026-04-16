package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import fr.hardel.asset_editor.client.compose.StudioColors
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
            palette.set(STRING, HighlightStyle.foreground(StudioColors.SyntaxString))
            palette.set(NUMBER, HighlightStyle.foreground(StudioColors.SyntaxNumber))
            palette.set(BOOLEAN, HighlightStyle.foreground(StudioColors.SyntaxBoolean))
            palette.set(NULL, HighlightStyle.foreground(StudioColors.SyntaxNull))
            palette.set(PROPERTY, HighlightStyle.foreground(StudioColors.SyntaxProperty))
            palette.set(PUNCTUATION, HighlightStyle.foreground(StudioColors.SyntaxPunctuation))
        }
    }
}
