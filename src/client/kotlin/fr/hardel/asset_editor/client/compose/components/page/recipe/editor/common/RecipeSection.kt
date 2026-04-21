package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeSelector

@Composable
fun RecipeSection(
    recipeType: String,
    recipeCounts: Map<String, Int>,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    headerExtra: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    RecipeSectionCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            RecipeSectionHeader(modifier = Modifier.weight(1f))
            headerExtra()
            RecipeSelector(
                value = recipeType,
                onChange = onSelectionChange,
                recipeCounts = recipeCounts,
                selectMode = true
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 16.dp),
            content = content
        )
    }
}
