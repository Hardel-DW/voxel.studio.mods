package fr.hardel.asset_editor.client.compose.components.codec.widget

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTranslation
import fr.hardel.asset_editor.client.compose.components.codec.WidgetEditor
import fr.hardel.asset_editor.client.compose.components.codec.defaultJsonFor
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.FieldLabel
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.RequiredFieldFrame
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.data.codec.CodecWidget
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

private val rowShape = RoundedCornerShape(6.dp)
private val TRASH = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/trash.svg")

@Composable
fun MapWidget(
    widget: CodecWidget.MapWidget,
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
                    keyWidget = widget.key(),
                    keyText = k,
                    valueWidget = widget.value(),
                    valueElement = v,
                    onKeyChange = { newKey ->
                        if (newKey.isBlank() || newKey == k) return@EntryRow
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
            label = I18n.get("codec:map.add"),
            onClick = {
                if (obj.has("")) return@AddRow
                val next = obj.deepCopy()
                next.add("", defaultJsonFor(widget.value()))
                onValueChange(next)
            }
        )
    }
}

@Composable
private fun EntryRow(
    keyWidget: CodecWidget,
    keyText: String,
    valueWidget: CodecWidget,
    valueElement: JsonElement,
    onKeyChange: (String) -> Unit,
    onValueChange: (JsonElement) -> Unit,
    onRemove: () -> Unit
) {
    val keyJson: JsonElement = if (keyText.isEmpty()) JsonNull.INSTANCE else JsonPrimitive(keyText)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(StudioColors.Zinc800.copy(alpha = 0.28f), rowShape)
            .border(1.dp, StudioColors.Zinc700.copy(alpha = 0.42f), rowShape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldLabel(
                text = mapKeyLabel(keyWidget),
                color = StudioColors.Zinc200
            )
            RequiredFieldFrame(requiredMissing = keyText.isBlank(), modifier = Modifier.weight(1f)) {
                WidgetEditor(
                    widget = keyWidget,
                    value = keyJson,
                    onValueChange = { newKeyJson ->
                        val asString = extractKeyString(newKeyJson)
                        if (asString != null) onKeyChange(asString)
                    }
                )
            }
            Spacer(Modifier.width(8.dp))
            RemoveButton(onClick = onRemove)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldLabel(
                text = I18n.get("codec:map.value"),
                color = StudioColors.Zinc300
            )
            Box(modifier = Modifier.weight(1f)) {
                WidgetEditor(widget = valueWidget, value = valueElement, onValueChange = onValueChange)
            }
        }
    }
}

private fun extractKeyString(json: JsonElement): String? {
    if (!json.isJsonPrimitive) return null
    val p = json.asJsonPrimitive
    return if (p.isString) p.asString else p.toString()
}

private fun mapKeyLabel(widget: CodecWidget): String = when (widget) {
    is CodecWidget.HolderWidget -> registryLabel(widget.registry())
    is CodecWidget.TagWidget -> registryLabel(widget.registry())
    else -> I18n.get("codec:map.key")
}

private fun registryLabel(registry: Identifier): String {
    return StudioTranslation.resolveRegistry(registry)
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
