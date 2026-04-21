package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.row.TextOptionRow
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
