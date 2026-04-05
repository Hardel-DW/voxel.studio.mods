package fr.hardel.asset_editor.client.compose.lib.highlight

import androidx.compose.ui.graphics.Color

class HighlightStyle(
    private val foreground: Color?,
    private val background: Color?,
    private val underline: Color?
) {

    companion object {
        @JvmStatic
        fun foreground(foreground: Color): HighlightStyle =
            HighlightStyle(foreground, null, null)

        @JvmStatic
        fun background(background: Color): HighlightStyle =
            HighlightStyle(null, background, null)
    }

    fun foreground(): Color? = foreground

    fun background(): Color? = background

    fun underline(): Color? = underline

    fun hasForeground(): Boolean = foreground != null

}
