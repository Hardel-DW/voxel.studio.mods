package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import fr.hardel.asset_editor.client.compose.lib.highlight.HighlightRegistry

fun interface CodeBlockHighlighter {
    fun apply(text: String, registry: HighlightRegistry)
}
