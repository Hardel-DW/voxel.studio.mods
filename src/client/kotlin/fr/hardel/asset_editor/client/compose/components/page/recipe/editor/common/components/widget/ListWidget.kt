package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.WidgetEditor
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.defaultJsonFor
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.FieldRowHeight
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.data.component.ComponentWidget
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val itemShape = RoundedCornerShape(4.dp)
private val TRASH = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/trash.svg")
private val PLUS = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/plus.svg")

@Composable
fun ListWidget(
    widget: ComponentWidget.ListWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val array = remember(value) { (value as? JsonArray) ?: JsonArray() }
    val items = remember(array) { List(array.size()) { array.get(it) } }
    val maxSize = widget.maxSize().orElse(Int.MAX_VALUE)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, item ->
            key(index) {
                ItemRow(
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

        if (items.size < maxSize) {
            AddRow(
                label = I18n.get("recipe:components.list.add"),
                onClick = {
                    val next = array.deepCopy()
                    next.add(defaultJsonFor(widget.item()))
                    onValueChange(next)
                }
            )
        }
    }
}

@Composable
private fun ItemRow(
    index: Int,
    widget: ComponentWidget,
    value: JsonElement,
    onChange: (JsonElement) -> Unit,
    onRemove: () -> Unit
) {
    val complex = widget is ComponentWidget.ObjectWidget ||
        widget is ComponentWidget.MapWidget ||
        widget is ComponentWidget.ListWidget ||
        widget is ComponentWidget.DispatchedWidget

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(StudioColors.Zinc900.copy(alpha = 0.35f), itemShape)
            .border(1.dp, StudioColors.Zinc800, itemShape)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#${index + 1}",
                style = StudioTypography.medium(11),
                color = StudioColors.Zinc500,
                modifier = Modifier.width(28.dp)
            )
            if (!complex) {
                Box(modifier = Modifier.weight(1f)) {
                    WidgetEditor(widget = widget, value = value, onValueChange = onChange)
                }
                Spacer(Modifier.width(6.dp))
            } else {
                Spacer(Modifier.weight(1f))
            }
            RemoveButton(onClick = onRemove)
        }
        if (complex) {
            Box(modifier = Modifier.padding(start = 28.dp)) {
                WidgetEditor(widget = widget, value = value, onValueChange = onChange)
            }
        }
    }
}

@Composable
private fun RemoveButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = if (hovered) StudioColors.Red500.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = StudioMotion.hoverSpec(),
        label = "list-remove-bg"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(FieldRowHeight)
            .clip(RoundedCornerShape(4.dp))
            .background(bg, RoundedCornerShape(4.dp))
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        SvgIcon(TRASH, 12.dp, tint = if (hovered) StudioColors.Red400 else StudioColors.Zinc500)
    }
}

@Composable
internal fun AddRow(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val border by animateColorAsState(
        targetValue = if (hovered) StudioColors.Zinc700 else StudioColors.Zinc800.copy(alpha = 0.6f),
        animationSpec = StudioMotion.hoverSpec(),
        label = "list-add-border"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(FieldRowHeight)
            .clip(itemShape)
            .background(StudioColors.Zinc900.copy(alpha = 0.2f), itemShape)
            .border(1.dp, border, itemShape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        SvgIcon(PLUS, 12.dp, tint = if (hovered) StudioColors.Zinc200 else StudioColors.Zinc500)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = StudioTypography.regular(12),
            color = if (hovered) StudioColors.Zinc100 else StudioColors.Zinc400
        )
    }
}
