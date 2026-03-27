package fr.hardel.asset_editor.client.compose.components.page.recipe.template

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeSlot
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeTemplateBase

@Composable
fun StoneCuttingTemplate(
    slots: Map<String, List<String>>,
    resultItemId: String,
    resultCount: Int,
    modifier: Modifier = Modifier
) {
    RecipeTemplateBase(
        resultItemId = resultItemId,
        resultCount = resultCount,
        modifier = modifier
    ) {
        // TSX: RecipeSlot item={slots["0"]}
        RecipeSlot(item = slots["0"].orEmpty())
    }
}
