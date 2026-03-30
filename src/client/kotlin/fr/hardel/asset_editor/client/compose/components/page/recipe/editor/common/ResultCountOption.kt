package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.runtime.Composable
import net.minecraft.client.resources.language.I18n

@Composable
fun ResultCountOption(
    value: Int,
    max: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit
) {
    val description = if (!enabled && max == 1) {
        I18n.get("recipe:section.result_count_locked")
    } else {
        I18n.get("recipe:section.result_count_description")
    }

    RecipeCounter(
        title = I18n.get("recipe:section.result_count"),
        description = description,
        value = value,
        max = max,
        enabled = enabled,
        onValueChange = onValueChange
    )
}
