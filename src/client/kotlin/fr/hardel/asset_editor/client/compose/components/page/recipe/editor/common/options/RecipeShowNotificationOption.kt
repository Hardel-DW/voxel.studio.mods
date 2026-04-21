package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.row.BooleanOptionRow
import net.minecraft.client.resources.language.I18n

@Composable
fun RecipeShowNotificationOption(
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    BooleanOptionRow(
        title = I18n.get("recipe:section.show_notification"),
        description = I18n.get("recipe:section.show_notification_description"),
        value = value,
        onValueChange = onValueChange
    )
}
