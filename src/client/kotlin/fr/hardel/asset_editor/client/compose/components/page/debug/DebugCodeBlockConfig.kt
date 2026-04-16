package fr.hardel.asset_editor.client.compose.components.page.debug

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.GsonBuilder
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.CodeBlockState
import fr.hardel.asset_editor.client.compose.components.ui.codeblock.JsonCodeBlockHighlighter

val DEBUG_CODE_BLOCK_GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
val DEBUG_CODE_BLOCK_MONO_STYLE = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
const val DEBUG_CODE_BLOCK_GSON_INDENT = "  "

fun configureDebugCodeBlock(state: CodeBlockState) {
    JsonCodeBlockHighlighter.installDefaultPalette(state.palette)
    state.highlighter = JsonCodeBlockHighlighter()
    state.textFill = StudioColors.Zinc300
    state.backgroundFill = StudioColors.Zinc950
    state.borderFill = StudioColors.Zinc800
    state.textStyle = DEBUG_CODE_BLOCK_MONO_STYLE
    state.lineSpacing = 5.sp
    state.wrapText = false
    state.minHeight = 360.dp
    state.showLineNumbers = true
}
