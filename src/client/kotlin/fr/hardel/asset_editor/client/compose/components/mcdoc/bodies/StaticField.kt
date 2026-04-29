package fr.hardel.asset_editor.client.compose.components.mcdoc.bodies

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.hardel.asset_editor.client.compose.components.mcdoc.Body
import fr.hardel.asset_editor.client.compose.components.mcdoc.CollapsibleKey
import fr.hardel.asset_editor.client.compose.components.mcdoc.ErrorIndicator
import fr.hardel.asset_editor.client.compose.components.mcdoc.Head
import fr.hardel.asset_editor.client.compose.components.mcdoc.Key
import fr.hardel.asset_editor.client.compose.components.mcdoc.fieldError
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.isCompactInline
import fr.hardel.asset_editor.client.compose.components.mcdoc.isSelfClearable
import fr.hardel.asset_editor.client.compose.components.mcdoc.isStructural
import fr.hardel.asset_editor.client.compose.components.mcdoc.rememberSimplified
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.IndentBox
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RemoveIconButton
import fr.hardel.asset_editor.client.compose.standardCollapseEnter
import fr.hardel.asset_editor.client.compose.standardCollapseExit
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType

private val CompactHeadMaxWidth = 280.dp

private enum class RemovePlacement { None, Inline, External }

@Composable
fun StaticField(
    fieldName: String,
    fieldType: McdocType,
    optional: Boolean,
    deprecated: Boolean,
    doc: String?,
    obj: JsonObject,
    onObjectChange: (JsonElement) -> Unit
) {
    val fieldValue = obj.get(fieldName)
    val simplified = rememberSimplified(fieldType, fieldValue)
    val present = fieldValue != null && !fieldValue.isJsonNull
    val updateField = { next: JsonElement -> onObjectChange(obj.deepCopy().apply { add(fieldName, next) }) }
    val removeField: () -> Unit = { onObjectChange(obj.deepCopy().apply { remove(fieldName) }) }
    val absentComplex = optional && !present && isStructural(simplified)
    val placement = removePlacement(optional, present, simplified)
    val onClear = if (placement == RemovePlacement.Inline) removeField else null
    val hasBody = !absentComplex && hasMcdocBody(simplified, fieldValue)
    val errorMessage = remember(simplified, fieldValue, optional) {
        fieldError(simplified, fieldValue, optional)
    }
    var expanded by remember(fieldName) { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (hasBody) {
                CollapsibleKey(label = fieldName, expanded = expanded, onToggle = { expanded = !expanded }, deprecated = deprecated)
            } else {
                Key(label = fieldName, doc = doc, deprecated = deprecated)
            }
            HeadSlot(simplified, fieldValue, updateField, onClear, errorMessage != null)
            if (placement == RemovePlacement.External) RemoveIconButton(onClick = removeField)
            if (errorMessage != null) {
                Spacer(Modifier.widthIn(min = 6.dp))
                ErrorIndicator(message = errorMessage)
            }
        }
        if (hasBody) {
            AnimatedVisibility(visible = expanded, enter = standardCollapseEnter(), exit = standardCollapseExit()) {
                IndentBox {
                    Body(simplified, fieldValue, updateField)
                }
            }
        }
    }
}

@Composable
private fun RowScope.HeadSlot(
    simplified: McdocType,
    fieldValue: JsonElement?,
    updateField: (JsonElement) -> Unit,
    onClear: (() -> Unit)?,
    error: Boolean
) {
    if (!hasMcdocHead(simplified) && simplified !is McdocType.StructType) {
        Spacer(Modifier.weight(1f))
        return
    }
    if (isCompactInline(simplified)) {
        Box(modifier = Modifier.widthIn(max = CompactHeadMaxWidth).errorOverlay(error)) {
            Head(simplified, fieldValue, updateField, onClear = onClear)
        }
        Spacer(Modifier.weight(1f))
        return
    }
    Box(modifier = Modifier.weight(1f).errorOverlay(error)) {
        Head(simplified, fieldValue, updateField, onClear = onClear)
    }
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

private fun removePlacement(optional: Boolean, present: Boolean, simplified: McdocType): RemovePlacement {
    if (!optional || !present) return RemovePlacement.None
    if (isSelfClearable(simplified)) return RemovePlacement.Inline
    if (simplified is McdocType.StructType) return RemovePlacement.Inline
    return RemovePlacement.External
}
