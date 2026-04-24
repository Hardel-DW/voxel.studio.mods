package fr.hardel.asset_editor.client.compose.components.codec.widget

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
import fr.hardel.asset_editor.client.compose.components.codec.WidgetBody
import fr.hardel.asset_editor.client.compose.components.codec.WidgetHead
import fr.hardel.asset_editor.client.compose.components.codec.defaultJsonFor
import fr.hardel.asset_editor.client.compose.components.codec.hasBody
import fr.hardel.asset_editor.client.compose.components.codec.hasHead
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.AddFieldButton
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.FieldControlShape
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.FieldRowHeight
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.IndentBox
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.RemoveIconButton
import fr.hardel.asset_editor.client.compose.standardCollapseEnter
import fr.hardel.asset_editor.client.compose.standardCollapseExit
import fr.hardel.asset_editor.data.codec.CodecWidget
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val CHEVRON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/chevron-down.svg")
private val TRASH = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/trash.svg")

@Composable
fun ListHead(
    widget: CodecWidget.ListWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val size = (value as? JsonArray)?.size() ?: 0
    val canAdd = size < widget.maxSize().orElse(Int.MAX_VALUE)

    AddFieldButton(
        label = I18n.get("codec:list.add"),
        enabled = canAdd,
        onClick = { onValueChange(addListItem(widget, value)) },
        modifier = modifier.fillMaxWidth(),
        shape = FieldControlShape
    )
}

@Composable
fun ListBody(
    widget: CodecWidget.ListWidget,
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
                    widget = widget.item(),
                    value = item,
                    onChange = { newVal ->
                        val next = JsonArray()
                        items.forEachIndexed { i, v -> next.add(if (i == index) newVal else v) }
                        onValueChange(next)
                    },
                    onRemove = {
                        val next = JsonArray()
                        items.forEachIndexed { i, v -> if (i != index) next.add(v) }
                        onValueChange(next)
                    }
                )
            }
        }
    }
}

private fun addListItem(widget: CodecWidget.ListWidget, value: JsonElement?): JsonArray {
    val current = (value as? JsonArray) ?: JsonArray()
    return current.deepCopy().also { it.add(defaultJsonFor(widget.item())) }
}

@Composable
private fun ListItemRow(
    index: Int,
    widget: CodecWidget,
    value: JsonElement,
    onChange: (JsonElement) -> Unit,
    onRemove: () -> Unit
) {
    val complex = hasBody(widget, value)

    if (!complex) {
        PrimitiveItemRow(widget = widget, value = value, onChange = onChange, onRemove = onRemove)
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
                if (hasHead(widget)) {
                    WidgetHead(widget = widget, value = value, onValueChange = onChange)
                }
                WidgetBody(widget = widget, value = value, onValueChange = onChange)
            }
        }
    }
}

@Composable
private fun PrimitiveItemRow(
    widget: CodecWidget,
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
            WidgetHead(widget = widget, value = value, onValueChange = onChange)
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
        label = "entry-bar-bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (hovered) CodecTokens.BorderStrong else CodecTokens.Border,
        animationSpec = StudioMotion.hoverSpec(),
        label = "entry-bar-border"
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
            text = "${I18n.get("codec:list.entry", index + 1)}",
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
        label = "bar-trash-bg"
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
        SvgIcon(
            location = TRASH,
            size = 12.dp,
            tint = if (hovered) CodecTokens.Text else CodecTokens.TextDimmed
        )
    }
}
