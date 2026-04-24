package fr.hardel.asset_editor.client.compose.components.codec

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class CodecCategory(
    val labelBg: Color,
    val border: Color,
    val containerBg: Color
) {
    PREDICATE(
        labelBg = Color(0xFF306163),
        border = Color(0xFF234848),
        containerBg = Color(0xFF1A3637)
    ),
    FUNCTION(
        labelBg = Color(0xFF5F5F5F),
        border = Color(0xFF474747),
        containerBg = Color(0xFF353535)
    ),
    POOL(
        labelBg = Color(0xFF386330),
        border = Color(0xFF294A24),
        containerBg = Color(0xFF1F361B)
    )
}

val LocalCodecCategory = compositionLocalOf<CodecCategory?> { null }
