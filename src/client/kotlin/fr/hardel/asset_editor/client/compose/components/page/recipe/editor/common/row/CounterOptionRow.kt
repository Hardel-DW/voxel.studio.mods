package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.row

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import fr.hardel.asset_editor.client.compose.components.ui.Counter

@Composable
fun CounterOptionRow(
    title: String,
    description: String,
    value: Int,
    max: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    step: Int = 1,
    displayValue: String = value.toString(),
    editValue: String = displayValue,
    sanitizeInput: (String) -> String = { input -> input.filter(Char::isDigit) },
    parseInput: (String) -> Int? = String::toIntOrNull,
    keyboardType: KeyboardType = KeyboardType.Number
) {
    OptionRowLayout(
        title = title,
        description = description,
        modifier = modifier
    ) {
        Counter(
            value = value,
            onValueChange = onValueChange,
            min = min,
            max = max,
            step = step,
            enabled = enabled,
            displayValue = displayValue,
            editValue = editValue,
            sanitizeInput = sanitizeInput,
            parseInput = parseInput,
            keyboardType = keyboardType
        )
    }
}
