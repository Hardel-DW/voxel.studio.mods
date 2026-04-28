package fr.hardel.asset_editor.client.compose.components.mcdoc.heads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.BooleanLiteral
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.LiteralType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.NumericLiteral
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StringLiteral

@Composable
fun LiteralHead(type: LiteralType, modifier: Modifier = Modifier) {
    val text = when (val v = type.value()) {
        is StringLiteral -> "\"${v.value()}\""
        is BooleanLiteral -> v.value().toString()
        is NumericLiteral -> v.value().toString()
    }
    val shape = RoundedCornerShape(McdocTokens.Radius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(McdocTokens.RowHeight)
            .clip(shape)
            .background(McdocTokens.LabelBg, shape)
            .border(1.dp, McdocTokens.Border, shape)
            .padding(horizontal = McdocTokens.PaddingX),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, style = StudioTypography.regular(13), color = McdocTokens.TextDimmed)
    }
}
