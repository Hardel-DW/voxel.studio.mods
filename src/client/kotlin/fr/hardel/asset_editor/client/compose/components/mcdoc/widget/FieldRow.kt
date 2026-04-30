package fr.hardel.asset_editor.client.compose.components.mcdoc.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp

val FieldRowHeight: Dp = McdocTokens.RowHeight
val FieldRowRadius: Dp = McdocTokens.Radius

@Composable
fun IndentBox(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                val borderPx = McdocTokens.IndentBorderWidth.toPx()
                val xPos = (McdocTokens.IndentWidth.toPx() - borderPx) / 2f
                drawRect(
                    color = McdocTokens.IndentBorder,
                    topLeft = Offset(xPos, 0f),
                    size = Size(borderPx, size.height)
                )
                drawContent()
            }
            .padding(start = McdocTokens.IndentWidth),
        verticalArrangement = Arrangement.spacedBy(McdocTokens.Gap)
    ) {
        content()
    }
}
