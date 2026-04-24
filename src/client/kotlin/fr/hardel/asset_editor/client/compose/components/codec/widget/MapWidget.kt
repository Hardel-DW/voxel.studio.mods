package fr.hardel.asset_editor.client.compose.components.codec.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.StudioTranslation
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens
import fr.hardel.asset_editor.client.compose.components.codec.WidgetBody
import fr.hardel.asset_editor.client.compose.components.codec.WidgetHead
import fr.hardel.asset_editor.client.compose.components.codec.defaultJsonFor
import fr.hardel.asset_editor.client.compose.components.codec.hasBody
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.AddFieldButton
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.FieldControlShape
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.FieldLabel
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.IndentBox
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.RemoveIconButton
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.RequiredFieldFrame
import fr.hardel.asset_editor.data.codec.CodecWidget
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

@Composable
fun MapHead(
    widget: CodecWidget.MapWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val obj = remember(value) { (value as? JsonObject) ?: JsonObject() }
    AddFieldButton(
        label = I18n.get("codec:map.add"),
        onClick = {
            if (obj.has("")) return@AddFieldButton
            val next = obj.deepCopy()
            next.add("", defaultJsonFor(widget.value()))
            onValueChange(next)
        },
        modifier = modifier.fillMaxWidth(),
        shape = FieldControlShape
    )
}

@Composable
fun MapBody(
    widget: CodecWidget.MapWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val obj = remember(value) { (value as? JsonObject) ?: JsonObject() }
    val entries = remember(obj) { obj.entrySet().map { it.key to it.value }.toList() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CodecTokens.Gap)
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
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CodecTokens.Gap)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FieldLabel(text = mapKeyLabel(keyWidget))
            Box(modifier = Modifier.weight(1f)) {
                RequiredFieldFrame(requiredMissing = keyText.isBlank()) {
                    WidgetHead(
                        widget = keyWidget,
                        value = keyJson,
                        onValueChange = { newKeyJson ->
                            extractKeyString(newKeyJson)?.let(onKeyChange)
                        }
                    )
                }
            }
            RemoveIconButton(onClick = onRemove)
        }

        IndentBox {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                FieldLabel(text = I18n.get("codec:map.value"), color = CodecTokens.TextDimmed)
                Box(modifier = Modifier.weight(1f)) {
                    WidgetHead(widget = valueWidget, value = valueElement, onValueChange = onValueChange)
                }
            }
            if (hasBody(valueWidget, valueElement)) {
                IndentBox {
                    WidgetBody(widget = valueWidget, value = valueElement, onValueChange = onValueChange)
                }
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
    is CodecWidget.HolderWidget -> StudioTranslation.resolveRegistry(widget.registry())
    is CodecWidget.TagWidget -> StudioTranslation.resolveRegistry(widget.registry())
    else -> I18n.get("codec:map.key")
}
