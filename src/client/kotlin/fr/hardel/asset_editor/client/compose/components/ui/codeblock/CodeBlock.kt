package fr.hardel.asset_editor.client.compose.components.ui.codeblock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.hardel.asset_editor.client.compose.VoxelColors

private val JSON_COLORS = mapOf(
    JsonTokenType.STRING to Color(0xFF98C379),
    JsonTokenType.NUMBER to Color(0xFFD19A66),
    JsonTokenType.BOOLEAN to Color(0xFF56B6C2),
    JsonTokenType.NULL to Color(0xFFC678DD),
    JsonTokenType.PROPERTY to Color(0xFF61AFEF),
    JsonTokenType.PUNCTUATION to Color(0xFFABB2BF)
)

private val DEFAULT_TEXT_COLOR = Color(0xFFABB2BF)
private val SHAPE = RoundedCornerShape(10.dp)
private val LINE_NUMBER_COLOR = VoxelColors.Zinc600
private val LINE_BORDER_COLOR = Color(0xFF303033).copy(alpha = 0.5f)

@Composable
fun CodeBlock(
    text: String,
    modifier: Modifier = Modifier,
    highlightJson: Boolean = true
) {
    val annotated = remember(text, highlightJson) {
        if (highlightJson) highlightJsonText(text) else AnnotatedString(text)
    }
    val lineCount = remember(text) { text.count { it == '\n' } + 1 }

    Box(
        modifier = modifier
            .clip(SHAPE)
            .background(VoxelColors.Zinc950)
            .border(1.dp, VoxelColors.Zinc800, SHAPE)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            BasicText(
                text = buildLineNumbers(lineCount),
                style = codeStyle().copy(
                    color = LINE_NUMBER_COLOR,
                    textAlign = TextAlign.End
                ),
                modifier = Modifier
                    .padding(vertical = 14.dp)
                    .padding(start = 14.dp, end = 12.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp)
                    .padding(start = 4.dp, end = 14.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    BasicText(
                        text = annotated,
                        style = codeStyle()
                    )
                }
            }
        }
    }
}

private fun highlightJsonText(source: String): AnnotatedString = buildAnnotatedString {
    val tokens = tokenizeJson(source)
    for (token in tokens) {
        val color = if (token.type == JsonTokenType.WHITESPACE) DEFAULT_TEXT_COLOR
                    else JSON_COLORS[token.type] ?: DEFAULT_TEXT_COLOR
        withStyle(SpanStyle(color = color)) {
            append(source, token.start, token.end)
        }
    }
}

private fun buildLineNumbers(count: Int): AnnotatedString = buildAnnotatedString {
    for (i in 1..count) {
        append(i.toString())
        if (i < count) append('\n')
    }
}

private fun codeStyle() = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.sp,
    color = DEFAULT_TEXT_COLOR,
    lineHeight = 20.sp
)
