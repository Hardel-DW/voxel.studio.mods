package fr.hardel.asset_editor.client.compose.components.codec

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.components.codec.widget.BooleanWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.DispatchedWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.EitherWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.EnumWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.FloatWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.HolderSetWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.HolderWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.IdentifierWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.IntegerWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.ListWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.MapWidget
import fr.hardel.asset_editor.client.compose.components.codec.widget.ObjectWidget
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
    when (widget) {
        is CodecWidget.IntegerWidget -> IntegerWidget(widget, value, onValueChange, modifier)
        is CodecWidget.FloatWidget -> FloatWidget(widget, value, onValueChange, modifier)
        is CodecWidget.BooleanWidget -> BooleanWidget(widget, value, onValueChange, modifier)
        is CodecWidget.UnitWidget -> UnitWidget(modifier)
        is CodecWidget.StringWidget -> StringWidget(widget, value, onValueChange, modifier)
        is CodecWidget.IdentifierWidget -> IdentifierWidget(value, onValueChange, modifier)
        is CodecWidget.TextCodecWidget -> TextCodecWidget(value, onValueChange, modifier)
        is CodecWidget.EnumWidget -> EnumWidget(widget, value, onValueChange, modifier)
        is CodecWidget.HolderWidget -> HolderWidget(widget, value, onValueChange, modifier)
        is CodecWidget.HolderSetWidget -> HolderSetWidget(widget, value, onValueChange, modifier)
        is CodecWidget.TagWidget -> TagWidget(widget, value, onValueChange, modifier)
        is CodecWidget.ObjectWidget -> ObjectWidget(widget, value, onValueChange, modifier)
        is CodecWidget.ListWidget -> ListWidget(widget, value, onValueChange, modifier)
        is CodecWidget.MapWidget -> MapWidget(widget, value, onValueChange, modifier)
        is CodecWidget.DispatchedWidget -> DispatchedWidget(widget, value, onValueChange, modifier)
        is CodecWidget.EitherWidget -> EitherWidget(widget, value, onValueChange, modifier)
        is CodecWidget.RawJsonWidget -> RawJsonWidget(value, onValueChange, modifier)
        is CodecWidget.ReferenceWidget -> RawJsonWidget(value, onValueChange, modifier)
    }
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
