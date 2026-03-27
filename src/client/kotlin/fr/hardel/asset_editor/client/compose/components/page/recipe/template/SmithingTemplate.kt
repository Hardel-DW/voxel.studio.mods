package fr.hardel.asset_editor.client.compose.components.page.recipe.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeSlot
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeTemplateBase

@Composable
fun SmithingTemplate(
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
        // TSX: div.flex.gap-1.w-full.h-full
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxHeight()
        ) {
            for (slotIndex in 0..2) {
                RecipeSlot(item = slots[slotIndex.toString()].orEmpty())
            }
        }
    }
}
