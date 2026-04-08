package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.hardel.asset_editor.client.compose.StudioColors

internal val CODE_BLOCK_SHAPE = RoundedCornerShape(10.dp)
internal val CODE_LINE_NUMBER_COLOR = StudioColors.Zinc600
internal val CODE_GUTTER_BORDER = StudioColors.Zinc800.copy(alpha = 0.5f)
internal val CODE_SELECTION_BG = Color(0xFF3B82F6).copy(alpha = 0.35f)

internal val CODE_BLOCK_CONTENT_PADDING = 16.dp
internal val CODE_BLOCK_GUTTER_PADDING = PaddingValues(
    start = 12.dp,
    top = 16.dp,
    end = 12.dp,
    bottom = 16.dp
)

val CODE_TEXT_STYLE = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.sp
)

internal fun resolveLineHeight(textStyle: TextStyle, lineSpacing: TextUnit): TextUnit {
    if (textStyle.lineHeight != TextUnit.Unspecified) return textStyle.lineHeight
    if (textStyle.fontSize == TextUnit.Unspecified) return lineSpacing
    return (textStyle.fontSize.value + lineSpacing.value).sp
}
