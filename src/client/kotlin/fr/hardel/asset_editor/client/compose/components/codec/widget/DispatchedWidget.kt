package fr.hardel.asset_editor.client.compose.components.codec.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import fr.hardel.asset_editor.client.compose.components.codec.WidgetBody
import fr.hardel.asset_editor.client.compose.components.codec.defaultJsonFor
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.CodecSelect
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.SelectOption
import fr.hardel.asset_editor.data.codec.CodecWidget

@Composable
fun DispatchedHead(
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
    val options = remember(widget) {
        widget.cases().keys.sorted().map { SelectOption(value = it, label = humanizeCase(it)) }
    }

    CodecSelect(
        options = options,
        selected = currentCase,
        onSelect = { picked ->
            val caseWidget = widget.cases()[picked]
            val defaultBody = (caseWidget?.let(::defaultJsonFor) as? JsonObject) ?: JsonObject()
            val next = defaultBody.deepCopy()
            next.addProperty(discriminator, picked)
            onValueChange(next)
        },
        modifier = modifier,
        placeholder = "— pick —"
    )
}

@Composable
fun DispatchedBody(
    widget: CodecWidget.DispatchedWidget,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val obj = (value as? JsonObject) ?: return
    val discriminator = widget.discriminator()
    val case = runCatching { obj.get(discriminator)?.asString }.getOrNull() ?: return
    val caseWidget = widget.cases()[case] ?: return

    val bodyOnly = remember(obj, discriminator) {
        obj.deepCopy().apply { remove(discriminator) }
    }

    WidgetBody(
        widget = caseWidget,
        value = bodyOnly,
        onValueChange = { newBody ->
            val merged = (newBody as? JsonObject)?.deepCopy() ?: JsonObject()
            merged.addProperty(discriminator, case)
            onValueChange(merged)
        }
    )
}

private fun humanizeCase(value: String): String =
    value.split('_', '/').joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }
