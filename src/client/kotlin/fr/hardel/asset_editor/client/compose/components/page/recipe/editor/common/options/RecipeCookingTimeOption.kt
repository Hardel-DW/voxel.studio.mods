package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.row.CounterOptionRow
import net.minecraft.client.resources.language.I18n

@Composable
fun RecipeCookingTimeOption(
    value: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    CounterOptionRow(
        title = I18n.get("recipe:section.cooking_time"),
        description = I18n.get("recipe:section.cooking_time_description"),
        value = value,
        min = 1,
        max = max,
        enabled = true,
        onValueChange = onValueChange
    )
}
