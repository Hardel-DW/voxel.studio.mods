package fr.hardel.asset_editor.client.compose.components.codec

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.components.codec.widget.BooleanWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.DispatchedBody
import fr.hardel.asset_editor.client.compose.components.codec.widget.DispatchedHead
import fr.hardel.asset_editor.client.compose.components.codec.widget.EitherBody
import fr.hardel.asset_editor.client.compose.components.codec.widget.EitherHead
import fr.hardel.asset_editor.client.compose.components.codec.widget.EnumWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.FloatWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.HolderSetBody
import fr.hardel.asset_editor.client.compose.components.codec.widget.HolderSetHead
import fr.hardel.asset_editor.client.compose.components.codec.widget.HolderWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.IdentifierWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.IntegerWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.ListBody
import fr.hardel.asset_editor.client.compose.components.codec.widget.ListHead
import fr.hardel.asset_editor.client.compose.components.codec.widget.MapBody
import fr.hardel.asset_editor.client.compose.components.codec.widget.MapHead
import fr.hardel.asset_editor.client.compose.components.codec.widget.ObjectBody
import fr.hardel.asset_editor.client.compose.components.codec.widget.RawJsonWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.StringWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.TagWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.TextCodecWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.UnitWidget
import fr.hardel.asset_editor.data.codec.CodecWidget

@Composable
fun WidgetEditor(
    widget: CodecWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CodecTokens.Gap)
    ) {
        if (hasHead(widget)) WidgetHead(widget, value, onValueChange)
        if (hasBody(widget, value)) WidgetBody(widget, value, onValueChange)
    }
}

@Composable
fun WidgetHead(
    widget: CodecWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    when (widget) {
        is CodecWidget.IntegerWidget -> IntegerWidget(widget, value, onValueChange, modifier, onClear)
        is CodecWidget.FloatWidget -> FloatWidget(widget, value, onValueChange, modifier, onClear)
        is CodecWidget.BooleanWidget -> BooleanWidget(widget, value, onValueChange, modifier, onClear)
        is CodecWidget.UnitWidget -> UnitWidget(modifier)
        is CodecWidget.StringWidget -> StringWidget(widget, value, onValueChange, modifier, onClear)
        is CodecWidget.IdentifierWidget -> IdentifierWidget(value, onValueChange, modifier, onClear)
        is CodecWidget.TextCodecWidget -> TextCodecWidget(value, onValueChange, modifier)
        is CodecWidget.EnumWidget -> EnumWidget(widget, value, onValueChange, modifier)
        is CodecWidget.HolderWidget -> HolderWidget(widget, value, onValueChange, modifier)
        is CodecWidget.HolderSetWidget -> HolderSetHead(widget, value, onValueChange, modifier)
        is CodecWidget.TagWidget -> TagWidget(widget, value, onValueChange, modifier)
        is CodecWidget.ObjectWidget -> Unit
        is CodecWidget.ListWidget -> ListHead(widget, value, onValueChange, modifier)
        is CodecWidget.MapWidget -> MapHead(widget, value, onValueChange, modifier)
        is CodecWidget.DispatchedWidget -> DispatchedHead(widget, value, onValueChange, modifier)
        is CodecWidget.EitherWidget -> EitherHead(widget, value, onValueChange, modifier)
        is CodecWidget.RawJsonWidget -> RawJsonWidget(value, onValueChange, modifier)
        is CodecWidget.ReferenceWidget -> RawJsonWidget(value, onValueChange, modifier)
    }
}

fun isSelfClearable(widget: CodecWidget): Boolean = when (widget) {
    is CodecWidget.IntegerWidget,
    is CodecWidget.FloatWidget,
    is CodecWidget.StringWidget,
    is CodecWidget.IdentifierWidget,
    is CodecWidget.BooleanWidget -> true
    else -> false
}

@Composable
fun WidgetBody(
    widget: CodecWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit
) {
    when (widget) {
        is CodecWidget.ObjectWidget -> ObjectBody(widget, value, onValueChange)
        is CodecWidget.ListWidget -> ListBody(widget, value, onValueChange)
        is CodecWidget.MapWidget -> MapBody(widget, value, onValueChange)
        is CodecWidget.DispatchedWidget -> DispatchedBody(widget, value, onValueChange)
        is CodecWidget.HolderSetWidget -> HolderSetBody(widget, value, onValueChange)
        is CodecWidget.EitherWidget -> EitherBody(widget, value, onValueChange)
        else -> Unit
    }
}

fun hasHead(widget: CodecWidget): Boolean = widget !is CodecWidget.ObjectWidget

fun hasBody(widget: CodecWidget, value: JsonElement?): Boolean = when (widget) {
    is CodecWidget.ObjectWidget -> widget.fields().isNotEmpty()
    is CodecWidget.ListWidget -> ((value as? JsonArray)?.size() ?: 0) > 0
    is CodecWidget.MapWidget -> ((value as? JsonObject)?.size() ?: 0) > 0
    is CodecWidget.DispatchedWidget -> {
        val obj = value as? JsonObject
        val case = obj?.let { runCatching { it.get(widget.discriminator())?.asString }.getOrNull() }
        case != null && widget.cases()[case] != null
    }
    is CodecWidget.HolderSetWidget -> value is JsonArray && value.size() > 0
    is CodecWidget.EitherWidget -> {
        val left = detectEitherSide(widget, value)
        hasBody(if (left) widget.left() else widget.right(), value)
    }
    else -> false
}

fun defaultJsonFor(widget: CodecWidget): JsonElement = when (widget) {
    is CodecWidget.IntegerWidget -> JsonPrimitive(widget.defaultValue().orElse(0))
    is CodecWidget.FloatWidget -> JsonPrimitive(widget.defaultValue().orElse(0f))
    is CodecWidget.BooleanWidget -> JsonPrimitive(widget.defaultValue().orElse(false))
    is CodecWidget.UnitWidget -> JsonObject()
    is CodecWidget.StringWidget -> JsonPrimitive(widget.defaultValue().orElse(""))
    is CodecWidget.IdentifierWidget -> JsonPrimitive(widget.defaultValue().map { it.toString() }.orElse(""))
    is CodecWidget.TextCodecWidget -> JsonPrimitive("")
    is CodecWidget.EnumWidget -> JsonPrimitive(widget.defaultValue().orElse(widget.values().firstOrNull() ?: ""))
    is CodecWidget.HolderWidget -> JsonPrimitive("")
    is CodecWidget.HolderSetWidget -> JsonArray()
    is CodecWidget.TagWidget -> JsonPrimitive("")
    is CodecWidget.ObjectWidget -> JsonObject()
    is CodecWidget.ListWidget -> JsonArray()
    is CodecWidget.MapWidget -> JsonObject()
    is CodecWidget.DispatchedWidget -> {
        val firstCase = widget.cases().entries.firstOrNull()
        if (firstCase == null) JsonObject()
        else {
            val inner = defaultJsonFor(firstCase.value)
            val obj = if (inner is JsonObject) inner.deepCopy() else JsonObject()
            obj.addProperty(widget.discriminator(), firstCase.key)
            obj
        }
    }
    is CodecWidget.EitherWidget -> defaultJsonFor(widget.left())
    is CodecWidget.RawJsonWidget -> JsonObject()
    is CodecWidget.ReferenceWidget -> JsonObject()
}

internal fun detectEitherSide(widget: CodecWidget.EitherWidget, value: JsonElement?): Boolean {
    if (value == null) return true
    return when (widget.left()) {
        is CodecWidget.HolderWidget -> value.isJsonPrimitive
        is CodecWidget.IdentifierWidget -> value.isJsonPrimitive
        is CodecWidget.ListWidget -> value.isJsonArray
        is CodecWidget.ObjectWidget -> value.isJsonObject
        else -> true
    }
}
