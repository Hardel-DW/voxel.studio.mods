package fr.hardel.asset_editor.client.compose.components.mcdoc.heads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.codec.CodecTokens
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.CodecSelect
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.RegistryCommandPalette
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.RegistryPickerMode
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.RegistryTrigger
import fr.hardel.asset_editor.client.compose.components.codec.widget.common.SelectOption
import fr.hardel.asset_editor.client.compose.components.mcdoc.McdocAttributes
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.*
import kotlin.jvm.optionals.getOrNull
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

@Composable
fun NumericHead(
    type: NumericType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    val isInt = type.kind().isInteger
    val keyboard = KeyboardOptions(
        keyboardType = if (isInt) KeyboardType.Number else KeyboardType.Decimal
    )
    val current = remember(value) { numericText(value, isInt) }
    val placeholder = numericPlaceholder(type)

    NumericTextField(
        text = current,
        placeholder = placeholder,
        keyboardOptions = keyboard,
        modifier = modifier,
        onChange = { next ->
            if (next.isEmpty()) {
                onClear?.invoke()
                return@NumericTextField
            }
            val parsed = parseNumeric(next, isInt) ?: return@NumericTextField
            onValueChange(JsonPrimitive(coerce(parsed, type)))
        }
    )
}

@Composable
fun BooleanHead(
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    val current = (value as? JsonPrimitive)?.let { runCatching { it.asBoolean }.getOrNull() }
    Row(
        modifier = modifier.height(CodecTokens.RowHeight),
    ) {
        ToggleSegment(label = "true", selected = current == true) { onValueChange(JsonPrimitive(true)) }
        ToggleSegment(label = "false", selected = current == false) { onValueChange(JsonPrimitive(false)) }
    }
}

@Composable
fun StringHead(
    type: StringType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    val registry = McdocAttributes.idRegistry(type.attributes())
    if (registry != null) {
        IdentifierHead(registry, value, onValueChange, modifier)
        return
    }
    val current = remember(value) { (value as? JsonPrimitive)?.asString.orEmpty() }
    NumericTextField(
        text = current,
        placeholder = "",
        keyboardOptions = KeyboardOptions.Default,
        modifier = modifier,
        onChange = { next ->
            if (next.isEmpty() && onClear != null) onClear()
            else onValueChange(JsonPrimitive(next))
        }
    )
}

@Composable
private fun IdentifierHead(
    registry: String,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier
) {
    val current = remember(value) {
        runCatching { (value as? JsonPrimitive)?.asString?.let(Identifier::tryParse) }.getOrNull()
    }
    var open by remember { mutableStateOf(false) }
    val registryId = remember(registry) { Identifier.tryParse(registry) }

    Box(modifier = modifier.fillMaxWidth()) {
        RegistryTrigger(
            label = current?.toString(),
            placeholder = I18n.get("codec:widget.unset"),
            onClick = { open = !open },
            modifier = Modifier.fillMaxWidth()
        )
        if (registryId != null) {
            RegistryCommandPalette(
                visible = open,
                registryId = registryId,
                mode = RegistryPickerMode.ELEMENTS,
                selected = current,
                onPick = { id ->
                    onValueChange(JsonPrimitive(id.toString()))
                    open = false
                },
                onDismiss = { open = false }
            )
        }
    }
}

@Composable
fun EnumHead(
    type: EnumType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    val options = remember(type) {
        type.values().map { field ->
            val enumValueText = when (val v = field.value()) {
                is StringEnumValue -> v.value()
                is NumericEnumValue -> v.value().toString()
            }
            SelectOption(value = enumValueText, label = humanize(field.identifier()))
        }
    }
    val selected = (value as? JsonPrimitive)?.let { p ->
        runCatching {
            if (p.isString) p.asString
            else p.asNumber.toString()
        }.getOrNull()
    }
    CodecSelect(
        options = options,
        selected = selected,
        onSelect = { picked ->
            val field = type.values().find { fieldValueText(it) == picked }
            val json = field?.let { fieldToJson(it) } ?: JsonPrimitive(picked)
            onValueChange(json)
        },
        modifier = modifier,
        placeholder = I18n.get("codec:widget.unset")
    )
}

@Composable
fun LiteralHead(type: LiteralType, modifier: Modifier = Modifier) {
    val text = when (val v = type.value()) {
        is StringLiteral -> "\"${v.value()}\""
        is BooleanLiteral -> v.value().toString()
        is NumericLiteral -> v.value().toString()
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CodecTokens.RowHeight)
            .clip(RoundedCornerShape(CodecTokens.Radius))
            .background(CodecTokens.LabelBg)
            .border(1.dp, CodecTokens.Border, RoundedCornerShape(CodecTokens.Radius))
            .padding(horizontal = CodecTokens.PaddingX),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = StudioTypography.regular(13),
            color = CodecTokens.TextDimmed
        )
    }
}

@Composable
fun AnyHead(
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = remember(value) { value?.toString().orEmpty() }
    NumericTextField(
        text = current,
        placeholder = "{ raw json }",
        keyboardOptions = KeyboardOptions.Default,
        modifier = modifier,
        onChange = { next ->
            val parsed = runCatching { com.google.gson.JsonParser.parseString(next) }.getOrNull()
            if (parsed != null) onValueChange(parsed)
        }
    )
}

@Composable
fun UnsafeHead(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CodecTokens.RowHeight)
            .clip(RoundedCornerShape(CodecTokens.Radius))
            .background(CodecTokens.LabelBg)
            .border(1.dp, CodecTokens.Error, RoundedCornerShape(CodecTokens.Radius))
            .padding(horizontal = CodecTokens.PaddingX),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = I18n.get("codec:widget.unsupported"),
            style = StudioTypography.regular(13),
            color = CodecTokens.Error
        )
    }
}

@Composable
private fun ToggleSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(CodecTokens.Radius)
    val bg = when {
        selected -> CodecTokens.Selected
        hovered -> CodecTokens.HoverBg
        else -> CodecTokens.InputBg
    }
    Box(
        modifier = Modifier
            .height(CodecTokens.RowHeight)
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, if (selected) CodecTokens.SelectedBorder else CodecTokens.Border, shape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = StudioTypography.medium(12),
            color = if (selected) CodecTokens.Text else CodecTokens.TextDimmed
        )
    }
}

@Composable
private fun NumericTextField(
    text: String,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier,
    onChange: (String) -> Unit
) {
    fr.hardel.asset_editor.client.compose.components.codec.widget.common.CodecTextInput(
        value = text,
        onValueChange = onChange,
        placeholder = placeholder,
        keyboardOptions = keyboardOptions,
        modifier = modifier
    )
}

private fun numericText(value: JsonElement?, isInt: Boolean): String {
    val prim = value as? JsonPrimitive ?: return ""
    if (!prim.isNumber) return ""
    return if (isInt) runCatching { prim.asLong.toString() }.getOrDefault("")
    else runCatching { prim.asDouble.toString() }.getOrDefault("")
}

private fun parseNumeric(text: String, isInt: Boolean): Number? =
    if (isInt) text.toLongOrNull() else text.toDoubleOrNull()

private fun coerce(parsed: Number, type: NumericType): Number {
    val range = type.valueRange().getOrNull() ?: return parsed
    val min = range.min().let { if (it.isPresent) it.asDouble else null }
    val max = range.max().let { if (it.isPresent) it.asDouble else null }
    var v = parsed.toDouble()
    if (min != null && v < min) v = min
    if (max != null && v > max) v = max
    return if (type.kind().isInteger) v.toLong() else v
}

private fun numericPlaceholder(type: NumericType): String {
    val range = type.valueRange().getOrNull() ?: return ""
    val min = range.min().let { if (it.isPresent) it.asDouble else null }
    val max = range.max().let { if (it.isPresent) it.asDouble else null }
    return when {
        min != null && max != null -> "$min – $max"
        min != null -> "≥ $min"
        max != null -> "≤ $max"
        else -> ""
    }
}

private fun humanize(text: String): String =
    text.replace('_', ' ').split(' ').joinToString(" ") {
        if (it.isEmpty()) it else it[0].uppercase() + it.substring(1).lowercase()
    }

private fun fieldValueText(field: EnumField): String = when (val v = field.value()) {
    is StringEnumValue -> v.value()
    is NumericEnumValue -> v.value().toString()
}

private fun fieldToJson(field: EnumField): JsonElement = when (val v = field.value()) {
    is StringEnumValue -> JsonPrimitive(v.value())
    is NumericEnumValue -> JsonPrimitive(v.value())
}

private fun JsonElement.orNullObject(): JsonObject = (this as? JsonObject) ?: JsonObject()
