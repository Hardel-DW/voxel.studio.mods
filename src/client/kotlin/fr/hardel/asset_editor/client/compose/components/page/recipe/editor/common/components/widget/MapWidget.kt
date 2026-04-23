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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.WidgetEditor
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.defaultJsonFor
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.data.component.ComponentWidget
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val rowShape = RoundedCornerShape(8.dp)
private val inputShape = RoundedCornerShape(6.dp)
private val TRASH = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/trash.svg")

@Composable
fun MapWidget(
    widget: ComponentWidget.MapWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val obj = remember(value) { (value as? JsonObject) ?: JsonObject() }
    val entries = remember(obj) { obj.entrySet().map { it.key to it.value }.toList() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        entries.forEachIndexed { index, (k, v) ->
            key(index) {
                EntryRow(
                    keyText = k,
                    valueWidget = widget.value(),
                    valueElement = v,
                    onKeyChange = { newKey ->
                        val next = JsonObject()
                        entries.forEachIndexed { i, pair ->
                            if (i == index) next.add(newKey, pair.second)
                            else next.add(pair.first, pair.second)
                        }
                        onValueChange(next)
                    },
                    onValueChange = { newVal ->
                        val next = JsonObject()
                        entries.forEachIndexed { i, pair ->
                            if (i == index) next.add(pair.first, newVal)
                            else next.add(pair.first, pair.second)
                        }
                        onValueChange(next)
                    },
                    onRemove = {
                        val next = JsonObject()
                        entries.forEachIndexed { i, pair ->
                            if (i != index) next.add(pair.first, pair.second)
                        }
                        onValueChange(next)
                    }
                )
            }
        }

        AddRow(
            label = I18n.get("recipe:components.map.add"),
            onClick = {
                val next = obj.deepCopy()
                next.add(uniqueKey(next), defaultJsonFor(widget.value()))
                onValueChange(next)
            }
        )
    }
}

@Composable
private fun EntryRow(
    keyText: String,
    valueWidget: ComponentWidget,
    valueElement: JsonElement,
    onKeyChange: (String) -> Unit,
    onValueChange: (JsonElement) -> Unit,
    onRemove: () -> Unit
) {
    var keyField by remember(keyText) { mutableStateOf(TextFieldValue(keyText)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(StudioColors.Zinc900.copy(alpha = 0.4f), rowShape)
            .border(1.dp, StudioColors.Zinc900, rowShape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = I18n.get("recipe:components.map.key"),
                style = StudioTypography.medium(11),
                color = StudioColors.Zinc500,
                modifier = Modifier.width(48.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(inputShape)
                    .background(StudioColors.Zinc950.copy(alpha = 0.5f), inputShape)
                    .border(1.dp, StudioColors.Zinc800, inputShape)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = keyField,
                    onValueChange = { next ->
                        keyField = next
                        if (next.text.isNotBlank()) onKeyChange(next.text)
                    },
                    textStyle = StudioTypography.regular(12).copy(color = StudioColors.Zinc100),
                    cursorBrush = SolidColor(StudioColors.Zinc100),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.width(8.dp))
            RemoveButton(onClick = onRemove)
        }
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = I18n.get("recipe:components.map.value"),
                style = StudioTypography.medium(11),
                color = StudioColors.Zinc500,
                modifier = Modifier.width(48.dp).padding(top = 6.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                WidgetEditor(widget = valueWidget, value = valueElement, onValueChange = onValueChange)
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
        label = "map-remove-bg"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg, RoundedCornerShape(6.dp))
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        SvgIcon(TRASH, 12.dp, tint = if (hovered) StudioColors.Red400 else StudioColors.Zinc500)
    }
}

private fun uniqueKey(obj: JsonObject): String {
    var i = 1
    while (obj.has("key_$i")) i++
    return "key_$i"
}
