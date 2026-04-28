package fr.hardel.asset_editor.client.compose.components.codec.widget.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens

val FieldRowHeight: Dp = CodecTokens.RowHeight
val FieldRowRadius: Dp = CodecTokens.Radius

@Composable
fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    minWidth: Dp = CodecTokens.LabelMinWidth,
    color: Color = CodecTokens.Text
) {
    val shape = RoundedCornerShape(topStart = FieldRowRadius, bottomStart = FieldRowRadius)

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(FieldRowHeight)
            .widthIn(min = minWidth)
            .clip(shape)
            .background(CodecTokens.LabelBg, shape)
            .border(1.dp, CodecTokens.Border, shape)
            .padding(horizontal = CodecTokens.PaddingX)
    ) {
        Text(
            text = text,
            style = StudioTypography.medium(12),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun IndentBox(content: @Composable () -> Unit) {
    val borderColor = CodecTokens.IndentBorder
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = CodecTokens.Gap)
            .drawWithContent {
                val borderPx = CodecTokens.IndentBorderWidth.toPx()
                val xPos = (CodecTokens.IndentWidth.toPx() - borderPx) / 2f
                drawRect(
                    color = borderColor,
                    topLeft = Offset(xPos, 0f),
                    size = Size(borderPx, size.height)
                )
                drawContent()
            }
            .padding(start = CodecTokens.IndentWidth),
        verticalArrangement = Arrangement.spacedBy(CodecTokens.Gap)
    ) {
        content()
    }
}
