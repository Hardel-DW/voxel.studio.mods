package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common.options

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.ui.CollapsibleSection
import net.minecraft.client.resources.language.I18n

@Composable
fun RecipeAdvancedOptions(
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    CollapsibleSection(
        title = I18n.get("generic:advanced_options"),
        subtitle = I18n.get("generic:advanced_options.description"),
        modifier = modifier,
        initiallyExpanded = initiallyExpanded,
        content = content
    )
}
