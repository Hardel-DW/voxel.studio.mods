package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.BooleanWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.IntegerWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.RawJsonWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.UnitWidget
import fr.hardel.asset_editor.data.component.ComponentWidget

@Composable
fun WidgetEditor(
    widget: ComponentWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    when (widget) {
        is ComponentWidget.IntegerWidget -> IntegerWidget(widget, value, onValueChange, modifier)
        is ComponentWidget.BooleanWidget -> BooleanWidget(widget, value, onValueChange, modifier)
        is ComponentWidget.UnitWidget -> UnitWidget(modifier)
        else -> RawJsonWidget(value, onValueChange, modifier)
    }
}

fun defaultJsonFor(widget: ComponentWidget): JsonElement = when (widget) {
    is ComponentWidget.IntegerWidget -> JsonPrimitive(widget.defaultValue().orElse(0))
    is ComponentWidget.FloatWidget -> JsonPrimitive(widget.defaultValue().orElse(0f))
    is ComponentWidget.BooleanWidget -> JsonPrimitive(widget.defaultValue().orElse(false))
    is ComponentWidget.UnitWidget -> JsonObject()
    is ComponentWidget.StringWidget -> JsonPrimitive(widget.defaultValue().orElse(""))
    is ComponentWidget.IdentifierWidget -> JsonPrimitive(widget.defaultValue().map { it.toString() }.orElse(""))
    is ComponentWidget.TextComponentWidget -> JsonPrimitive("")
    is ComponentWidget.EnumWidget -> JsonPrimitive(widget.defaultValue().orElse(widget.values().firstOrNull() ?: ""))
    is ComponentWidget.HolderWidget -> JsonPrimitive("")
    is ComponentWidget.HolderSetWidget -> JsonArray()
    is ComponentWidget.TagWidget -> JsonPrimitive("")
    is ComponentWidget.ObjectWidget -> JsonObject()
    is ComponentWidget.ListWidget -> JsonArray()
    is ComponentWidget.MapWidget -> JsonObject()
    is ComponentWidget.DispatchedWidget -> JsonObject()
    is ComponentWidget.EitherWidget -> defaultJsonFor(widget.left())
    is ComponentWidget.RawJsonWidget -> JsonObject()
    else -> JsonNull.INSTANCE
}
