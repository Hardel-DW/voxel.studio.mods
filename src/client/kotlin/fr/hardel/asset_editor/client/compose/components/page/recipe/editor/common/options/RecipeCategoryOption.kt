package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.row.DropdownOptionRow
import net.minecraft.client.resources.language.I18n

@Composable
fun RecipeCategoryOption(
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    DropdownOptionRow(
        title = I18n.get("recipe:section.category"),
        description = I18n.get("recipe:section.category_description"),
        options = options,
        selected = value,
        onSelect = onValueChange
    )
}
