package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.WidgetEditor
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.defaultJsonFor
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.AddFieldButton
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.FieldControlShape
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.ListEntryHeader
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.common.RemoveIconButton
import fr.hardel.asset_editor.client.compose.standardCollapseEnter
import fr.hardel.asset_editor.client.compose.standardCollapseExit
import fr.hardel.asset_editor.data.component.ComponentWidget
import net.minecraft.client.resources.language.I18n

private val itemShape = RoundedCornerShape(6.dp)

@Composable
fun ListWidget(
    widget: ComponentWidget.ListWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier,
    showAddButton: Boolean = true
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

        if (showAddButton && items.size < maxSize) {
            AddRow(
                label = I18n.get("recipe:components.list.add"),
                onClick = { onValueChange(addListItem(widget, array)) }
            )
        }
    }
}

fun addListItem(widget: ComponentWidget.ListWidget, value: JsonElement?): JsonArray {
    val current = (value as? JsonArray) ?: JsonArray()
    return current.deepCopy().also { it.add(defaultJsonFor(widget.item())) }
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
    var expanded by remember(index) { mutableStateOf(true) }

    if (!complex) {
        PrimitiveItemRow(
            widget = widget,
            value = value,
            onChange = onChange,
            onRemove = onRemove
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(StudioColors.Zinc900.copy(alpha = 0.24f), itemShape)
            .border(1.dp, StudioColors.Zinc800.copy(alpha = 0.42f), itemShape)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ListEntryHeader(
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
            Box(modifier = Modifier.padding(top = 2.dp)) {
                WidgetEditor(widget = widget, value = value, onValueChange = onChange)
            }
        }
    }
}

@Composable
private fun PrimitiveItemRow(
    widget: ComponentWidget,
    value: JsonElement,
    onChange: (JsonElement) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            WidgetEditor(widget = widget, value = value, onValueChange = onChange)
        }
        RemoveIconButton(onClick = onRemove)
    }
}

@Composable
internal fun AddRow(label: String, onClick: () -> Unit) {
    AddFieldButton(
        label = label,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = FieldControlShape
    )
}
