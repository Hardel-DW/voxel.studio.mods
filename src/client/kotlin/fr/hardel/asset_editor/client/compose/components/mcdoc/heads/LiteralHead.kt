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
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.mcdoc.defaultFor
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.AddFieldButton
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.FieldControlShape
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.BooleanLiteral
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.LiteralType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.NumericLiteral
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.StringLiteral
import net.minecraft.client.resources.language.I18n

@Composable
fun LiteralHead(
    type: LiteralType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val absent = value == null || value is JsonNull
    if (absent) {
        Box(modifier = modifier.fillMaxWidth()) {
            AddFieldButton(
                label = I18n.get("mcdoc:field.add") + ": " + literalText(type),
                onClick = { onValueChange(defaultFor(type)) },
                modifier = Modifier.fillMaxWidth(),
                shape = FieldControlShape
            )
        }
        return
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
        Text(text = literalText(type), style = StudioTypography.regular(13), color = McdocTokens.TextDimmed)
    }
}

private fun literalText(type: LiteralType): String = when (val v = type.value()) {
    is StringLiteral -> "\"${v.value()}\""
    is BooleanLiteral -> v.value().toString()
    is NumericLiteral -> v.value().toString()
}
