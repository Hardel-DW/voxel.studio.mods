package fr.hardel.asset_editor.client.compose.components.mcdoc.bodies

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.AddFieldButton
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.FieldControlShape
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.FieldRowHeight
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.IndentBox
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.RemoveIconButton
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocDefaults
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.orNull
import fr.hardel.asset_editor.client.compose.components.mcdoc.rememberSimplified
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.standardCollapseEnter
import fr.hardel.asset_editor.client.compose.standardCollapseExit
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.ListType
import kotlin.jvm.optionals.getOrNull
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val CHEVRON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")

@Composable
fun ListHead(
    type: ListType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val size = (value as? JsonArray)?.size() ?: 0
    val maxSize = type.lengthRange().getOrNull()?.max()?.orNull()?.toInt()
    val canAdd = maxSize == null || size < maxSize

    AddFieldButton(
        label = I18n.get("codec:list.add"),
        enabled = canAdd,
        onClick = { onValueChange(addItem(type, value)) },
        modifier = modifier.fillMaxWidth(),
        shape = FieldControlShape
    )
}

@Composable
fun ListBody(
    type: ListType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val array = remember(value) { (value as? JsonArray) ?: JsonArray() }
    val items = remember(array) { List(array.size()) { array.get(it) } }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CodecTokens.Gap)
    ) {
        items.forEachIndexed { index, item ->
            key(index) {
                ListItemRow(
                    index = index,
                    itemType = type.item(),
                    value = item,
                    onChange = { newValue -> onValueChange(items.replaceAt(index, newValue)) },
                    onRemove = { onValueChange(items.removeAt(index)) }
                )
            }
        }
    }
}

private fun addItem(type: ListType, value: JsonElement?): JsonArray {
    val current = (value as? JsonArray) ?: JsonArray()
    return current.deepCopy().also { it.add(McdocDefaults.defaultFor(type.item())) }
}

private fun List<JsonElement>.replaceAt(index: Int, value: JsonElement): JsonArray {
    val next = JsonArray()
    forEachIndexed { i, v -> next.add(if (i == index) value else v) }
    return next
}

private fun List<JsonElement>.removeAt(index: Int): JsonArray {
    val next = JsonArray()
    forEachIndexed { i, v -> if (i != index) next.add(v) }
    return next
}

@Composable
private fun ListItemRow(
    index: Int,
    itemType: McdocType,
    value: JsonElement,
    onChange: (JsonElement) -> Unit,
    onRemove: () -> Unit
) {
    val simplified = rememberSimplified(itemType, value)
    val complex = hasMcdocBody(simplified, value)

    if (!complex) {
        PrimitiveItemRow(simplifiedType = simplified, value = value, onChange = onChange, onRemove = onRemove)
        return
    }

    var expanded by remember(index) { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        EntryHeaderBar(
            index = index,
            expanded = expanded,
            onToggle = { expanded = !expanded },
            onRemove = onRemove
        )
        AnimatedVisibility(
            visible = expanded,
            enter = standardCollapseEnter(),
            exit = standardCollapseExit()
        ) {
            IndentBox {
                if (hasMcdocHead(simplified)) McdocHead(simplified, value, onChange)
                McdocBody(simplified, value, onChange)
            }
        }
    }
}

@Composable
private fun PrimitiveItemRow(
    simplifiedType: McdocType,
    value: JsonElement,
    onChange: (JsonElement) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CodecTokens.Gap),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            McdocHead(simplifiedType, value, onChange)
        }
        RemoveIconButton(onClick = onRemove)
    }
}

@Composable
private fun EntryHeaderBar(
    index: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(CodecTokens.Radius)
    val bg by animateColorAsState(
        targetValue = if (hovered) CodecTokens.HoverBg else CodecTokens.LabelBg,
        animationSpec = StudioMotion.hoverSpec(),
        label = "list-entry-bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (hovered) CodecTokens.BorderStrong else CodecTokens.Border,
        animationSpec = StudioMotion.hoverSpec(),
        label = "list-entry-border"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(FieldRowHeight)
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, borderColor, shape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
            .padding(start = CodecTokens.PaddingX, end = 4.dp)
    ) {
        SvgIcon(
            location = CHEVRON,
            size = 12.dp,
            tint = if (hovered) CodecTokens.Text else CodecTokens.TextDimmed,
            modifier = Modifier.rotate(if (expanded) 0f else -90f)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = I18n.get("codec:list.entry", index + 1),
            style = StudioTypography.medium(12),
            color = if (hovered) CodecTokens.Text else CodecTokens.TextDimmed,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        BarTrashIcon(onClick = onRemove)
    }
}

@Composable
private fun BarTrashIcon(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(CodecTokens.Radius)
    val bg by animateColorAsState(
        targetValue = if (hovered) CodecTokens.Remove else Color.Transparent,
        animationSpec = StudioMotion.hoverSpec(),
        label = "list-trash-bg"
    )
    val trashIcon = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/trash.svg")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(FieldRowHeight - 6.dp)
            .clip(shape)
            .background(bg, shape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        SvgIcon(location = trashIcon, size = 12.dp, tint = if (hovered) CodecTokens.Text else CodecTokens.TextDimmed)
    }
}
