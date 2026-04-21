package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.row.CounterOptionRow
import net.minecraft.client.resources.language.I18n
import java.math.BigDecimal
import java.math.RoundingMode

private const val EXPERIENCE_SCALE = 100

@Composable
fun RecipeExperienceOption(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    val scaledValue = (value * EXPERIENCE_SCALE).toInt().coerceAtLeast(0)

    CounterOptionRow(
        title = I18n.get("recipe:section.experience"),
        description = I18n.get("recipe:section.experience_description"),
        value = scaledValue,
        min = 0,
        max = Int.MAX_VALUE,
        step = 5,
        enabled = true,
        displayValue = formatExperience(value),
        editValue = formatExperience(value),
        sanitizeInput = ::sanitizeExperienceInput,
        parseInput = ::parseExperienceInput,
        keyboardType = KeyboardType.Decimal,
        onValueChange = { scaled -> onValueChange(scaled / EXPERIENCE_SCALE.toFloat()) }
    )
}

private fun sanitizeExperienceInput(value: String): String {
    val builder = StringBuilder()
    var dotSeen = false

    value.forEachIndexed { index, character ->
        when {
            character.isDigit() -> builder.append(character)
            character == '.' && !dotSeen -> {
                if (index == 0) builder.append('0')
                builder.append('.')
                dotSeen = true
            }
        }
    }

    return builder.toString()
}

private fun parseExperienceInput(value: String): Int? =
    value.toBigDecimalOrNull()
        ?.coerceAtLeast(BigDecimal.ZERO)
        ?.multiply(BigDecimal(EXPERIENCE_SCALE))
        ?.setScale(0, RoundingMode.HALF_UP)
        ?.toInt()

private fun formatExperience(value: Float): String =
    BigDecimal(value.toString())
        .stripTrailingZeros()
        .toPlainString()
