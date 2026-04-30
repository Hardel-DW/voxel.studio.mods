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
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.mcdoc.Body
import fr.hardel.asset_editor.client.compose.components.mcdoc.Head
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocBody
import fr.hardel.asset_editor.client.compose.components.mcdoc.hasMcdocHead
import fr.hardel.asset_editor.client.compose.components.mcdoc.rememberSimplified
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.FieldRowHeight
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.IndentBox
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocIcons
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.McdocTokens
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.RemoveIconButton
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.standardCollapseEnter
import fr.hardel.asset_editor.client.compose.standardCollapseExit
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.ListType
import net.minecraft.client.resources.language.I18n

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
        verticalArrangement = Arrangement.spacedBy(McdocTokens.Gap)
    ) {
        items.forEachIndexed { index, item ->
            key(index) {
                ListItem(
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
private fun ListItem(
    index: Int,
    itemType: McdocType,
    value: JsonElement,
    onChange: (JsonElement) -> Unit,
    onRemove: () -> Unit
) {
    val simplified = rememberSimplified(itemType, value)
    val complex = hasMcdocBody(simplified, value)

    if (!complex) {
        InlineItemRow(simplifiedType = simplified, value = value, onChange = onChange, onRemove = onRemove)
        return
    }

    var expanded by remember(index) { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        EntryHeader(index = index, expanded = expanded, onToggle = { expanded = !expanded }, onRemove = onRemove)
        AnimatedVisibility(visible = expanded, enter = standardCollapseEnter(), exit = standardCollapseExit()) {
            IndentBox {
                if (hasMcdocHead(simplified)) Head(simplified, value, onChange)
                Body(simplified, value, onChange)
            }
        }
    }
}

@Composable
private fun InlineItemRow(
    simplifiedType: McdocType,
    value: JsonElement,
    onChange: (JsonElement) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(McdocTokens.Gap),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.weight(1f)) { Head(simplifiedType, value, onChange) }
        RemoveIconButton(onClick = onRemove)
    }
}

@Composable
private fun EntryHeader(index: Int, expanded: Boolean, onToggle: () -> Unit, onRemove: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(McdocTokens.Radius)
    val bg by animateColorAsState(
        targetValue = if (hovered) McdocTokens.HoverBg else McdocTokens.LabelBg,
        animationSpec = StudioMotion.hoverSpec(),
        label = "list-entry-bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (hovered) McdocTokens.BorderStrong else McdocTokens.Border,
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
            .padding(start = McdocTokens.PaddingX, end = 4.dp)
    ) {
        SvgIcon(
            location = McdocIcons.ChevronDown,
            size = 12.dp,
            tint = if (hovered) McdocTokens.Text else McdocTokens.TextDimmed,
            modifier = Modifier.rotate(if (expanded) 0f else -90f)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = I18n.get("mcdoc:list.entry", index + 1),
            style = StudioTypography.medium(12),
            color = if (hovered) McdocTokens.Text else McdocTokens.TextDimmed,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        EntryTrash(onClick = onRemove)
    }
}

@Composable
private fun EntryTrash(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(McdocTokens.Radius)
    val bg by animateColorAsState(
        targetValue = if (hovered) McdocTokens.Remove else Color.Transparent,
        animationSpec = StudioMotion.hoverSpec(),
        label = "list-trash-bg"
    )
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
        SvgIcon(location = McdocIcons.Trash, size = 12.dp, tint = if (hovered) McdocTokens.Text else McdocTokens.TextDimmed)
    }
}
