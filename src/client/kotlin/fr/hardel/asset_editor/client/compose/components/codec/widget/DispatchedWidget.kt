package fr.hardel.asset_editor.client.compose.components.codec.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.components.codec.WidgetEditor
import fr.hardel.asset_editor.client.compose.components.codec.defaultJsonFor
import fr.hardel.asset_editor.data.codec.CodecWidget
import java.util.Optional

@Composable
fun DispatchedWidget(
    widget: CodecWidget.DispatchedWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val obj = remember(value) { (value as? JsonObject) ?: JsonObject() }
    val discriminator = widget.discriminator()
    val currentCase = remember(obj) {
        runCatching { obj.get(discriminator)?.asString }.getOrNull()
    }
    val caseOptions = remember(widget) { widget.cases().keys.sorted() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EnumWidget(
                widget = CodecWidget.EnumWidget(caseOptions, Optional.ofNullable(currentCase)),
                value = currentCase?.let(::JsonPrimitive),
                onValueChange = { picked ->
                    val pickedCase = runCatching { picked.asString }.getOrNull() ?: return@EnumWidget
                    val caseWidget = widget.cases()[pickedCase]
                    val defaultBody = (caseWidget?.let(::defaultJsonFor) as? JsonObject) ?: JsonObject()
                    val next = defaultBody.deepCopy()
                    next.addProperty(discriminator, pickedCase)
                    onValueChange(next)
                },
                modifier = Modifier.weight(1f)
            )
        }

        currentCase?.let { case ->
            widget.cases()[case]?.let { caseWidget ->
                val bodyOnly = remember(obj, discriminator) {
                    obj.deepCopy().apply { remove(discriminator) }
                }
                WidgetEditor(
                    widget = caseWidget,
                    value = bodyOnly,
                    onValueChange = { newBody ->
                        val merged = (newBody as? JsonObject)?.deepCopy() ?: JsonObject()
                        merged.addProperty(discriminator, case)
                        onValueChange(merged)
                    }
                )
            }
        }
    }
}
