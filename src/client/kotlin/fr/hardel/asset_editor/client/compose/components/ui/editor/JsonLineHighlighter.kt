package fr.hardel.asset_editor.client.compose.components.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.JsonCodeBlockHighlighter
import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightPalette
import fr.hardel.asset_editor.client.compose.lib.utils.JsonTokenizer

/**
 * Tokenizes a single JSON line and returns the styled [AnnotatedString].
 *
 * Strict JSON has no multi-line tokens — strings, numbers and literals all
 * stay on a single line — so each line can be highlighted independently of
 * its neighbours, which is what enables [LineHighlightCache] to work.
 *
 * Token → colour mapping is resolved once at construction from the same
 * [JsonCodeBlockHighlighter.installDefaultPalette] used by the read-only viewer
 * so the editor stays visually identical to [CodeBlock].
 */
class JsonLineHighlighter(
    val defaultColor: Color = StudioColors.Zinc300
) {
    private val colorByType: Map<JsonTokenizer.TokenType, Color>

    init {
        val palette = HighlightPalette().also { JsonCodeBlockHighlighter.installDefaultPalette(it) }
        colorByType = JsonTokenizer.TokenType.entries.mapNotNull { type ->
            val name = type.highlightName ?: return@mapNotNull null
            val color = palette.get(name)?.foreground() ?: return@mapNotNull null
            type to color
        }.toMap()
    }

    fun highlight(line: String): AnnotatedString {
        if (line.isEmpty()) return AnnotatedString("")
        return buildAnnotatedString {
            val tokens = JsonTokenizer.tokenize(line)
            var cursor = 0
            for (token in tokens) {
                if (token.start > cursor) {
                    pushStyle(SpanStyle(color = defaultColor))
                    append(line.substring(cursor, token.start))
                    pop()
                }
                val color = colorByType[token.type] ?: defaultColor
                pushStyle(SpanStyle(color = color))
                append(line.substring(token.start, token.end))
                pop()
                cursor = token.end
            }
            if (cursor < line.length) {
                pushStyle(SpanStyle(color = defaultColor))
                append(line.substring(cursor))
                pop()
            }
        }
    }
}
