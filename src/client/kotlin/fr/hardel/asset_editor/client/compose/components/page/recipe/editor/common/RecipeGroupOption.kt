package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.runtime.Composable
import net.minecraft.client.resources.language.I18n

@Composable
fun RecipeGroupOption(
    value: String,
    onValueChange: (String) -> Unit
) {
    TextOptionRow(
        title = I18n.get("recipe:section.group"),
        description = I18n.get("recipe:section.group_description"),
        value = value,
        onValueChange = onValueChange
    )
}
