package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.BooleanWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.DispatchedWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.EitherWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.EnumWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.FloatWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.HolderSetWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.HolderWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.IdentifierWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.IntegerWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.ListWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.MapWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.ObjectWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.RawJsonWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.StringWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.TagWidget
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.components.widget.TextComponentWidget
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
        is ComponentWidget.FloatWidget -> FloatWidget(widget, value, onValueChange, modifier)
        is ComponentWidget.BooleanWidget -> BooleanWidget(widget, value, onValueChange, modifier)
        is ComponentWidget.UnitWidget -> UnitWidget(modifier)
        is ComponentWidget.StringWidget -> StringWidget(widget, value, onValueChange, modifier)
        is ComponentWidget.IdentifierWidget -> IdentifierWidget(value, onValueChange, modifier)
        is ComponentWidget.TextComponentWidget -> TextComponentWidget(value, onValueChange, modifier)
        is ComponentWidget.EnumWidget -> EnumWidget(widget, value, onValueChange, modifier)
        is ComponentWidget.HolderWidget -> HolderWidget(widget, value, onValueChange, modifier)
        is ComponentWidget.HolderSetWidget -> HolderSetWidget(widget, value, onValueChange, modifier)
        is ComponentWidget.TagWidget -> TagWidget(widget, value, onValueChange, modifier)
        is ComponentWidget.ObjectWidget -> ObjectWidget(widget, value, onValueChange, modifier)
        is ComponentWidget.ListWidget -> ListWidget(widget, value, onValueChange, modifier)
        is ComponentWidget.MapWidget -> MapWidget(widget, value, onValueChange, modifier)
        is ComponentWidget.DispatchedWidget -> DispatchedWidget(widget, value, onValueChange, modifier)
        is ComponentWidget.EitherWidget -> EitherWidget(widget, value, onValueChange, modifier)
        is ComponentWidget.RawJsonWidget -> RawJsonWidget(value, onValueChange, modifier)
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
