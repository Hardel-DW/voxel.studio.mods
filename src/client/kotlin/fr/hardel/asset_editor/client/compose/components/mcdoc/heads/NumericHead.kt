package fr.hardel.asset_editor.client.compose.components.mcdoc.heads

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fr.hardel.asset_editor.client.compose.components.mcdoc.orNull
import fr.hardel.asset_editor.client.compose.components.mcdoc.toPlaceholder
import fr.hardel.asset_editor.client.compose.components.mcdoc.widget.NumberStepper
import fr.hardel.asset_editor.client.mcdoc.ast.McdocType.NumericType
import fr.hardel.asset_editor.client.mcdoc.ast.NumericRange
import kotlin.jvm.optionals.getOrNull
import net.minecraft.client.resources.language.I18n

@Composable
fun NumericHead(
    type: NumericType,
    value: JsonElement?,
    onValueChange: (JsonElement) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    val isInt = type.kind().isInteger
    val keyboard = KeyboardOptions(keyboardType = if (isInt) KeyboardType.Number else KeyboardType.Decimal)
    val current = remember(value) { numericText(value, isInt) }
    val range = type.valueRange().getOrNull()
    val error = remember(current, isInt) { unparseableError(current, isInt) }

    NumberStepper(
        value = current,
        onValueChange = { next ->
            if (next.isEmpty()) {
                onClear?.invoke()
                return@NumberStepper
            }
            val parsed = parseNumeric(next, isInt) ?: return@NumberStepper
            onValueChange(JsonPrimitive(if (isInt) parsed.toLong() else parsed.toDouble()))
        },
        onStep = { delta ->
            val base = parseNumeric(current, isInt)?.toDouble() ?: 0.0
            val stepped = base + delta
            val clamped = clampToRange(stepped, range)
            onValueChange(JsonPrimitive(if (isInt) clamped.toLong() else clamped))
        },
        placeholder = range?.toPlaceholder().orEmpty(),
        keyboardOptions = keyboard,
        modifier = modifier,
        error = error
    )
}

private fun clampToRange(value: Double, range: NumericRange?): Double {
    if (range == null) return value
    val min = range.min().orNull()
    val max = range.max().orNull()
    val low = if (min != null && value < min) min else value
    return if (max != null && low > max) max else low
}

private fun numericText(value: JsonElement?, isInt: Boolean): String {
    val prim = value as? JsonPrimitive ?: return ""
    if (!prim.isNumber) return ""
    return runCatching { if (isInt) prim.asLong.toString() else prim.asDouble.toString() }.getOrDefault("")
}

internal fun parseNumeric(text: String, isInt: Boolean): Number? =
    if (isInt) text.toLongOrNull() else text.toDoubleOrNull()

private fun unparseableError(text: String, isInt: Boolean): String? {
    if (text.isEmpty()) return null
    if (parseNumeric(text, isInt) != null) return null
    return I18n.get("mcdoc:error.invalid_number")
}
