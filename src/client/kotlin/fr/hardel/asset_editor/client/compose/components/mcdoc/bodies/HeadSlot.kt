package fr.hardel.asset_editor.client.compose.components.mcdoc.bodies

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import fr.hardel.asset_editor.client.compose.components.mcdoc.Head
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.isCompactInline
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType

@Composable
internal fun RowScope.HeadSlot(
    simplified: McdocType,
    value: JsonElement?,
    onChange: (JsonElement) -> Unit,
    onClear: (() -> Unit)? = null,
    showError: Boolean = false
) {
    if (!hasMcdocHead(simplified) && simplified !is McdocType.StructType) {
        Spacer(Modifier.weight(1f))
        return
    }
    val sizing = if (isCompactInline(simplified)) {
        Modifier.widthIn(max = McdocTokens.CompactHeadMaxWidth)
    } else {
        Modifier.weight(1f)
    }
    Box(modifier = sizing.errorOverlay(showError)) {
        Head(simplified, value, onChange, onClear = onClear)
    }
    if (isCompactInline(simplified)) Spacer(Modifier.weight(1f))
}

private fun Modifier.errorOverlay(error: Boolean): Modifier {
    if (!error) return this
    return drawWithContent {
        drawContent()
        val stroke = 1.dp.toPx()
        val radius = McdocTokens.Radius.toPx()
        val inset = stroke / 2f
        drawRoundRect(
            color = McdocTokens.Error,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = stroke)
        )
    }
}
