package fr.hardel.asset_editor.client.compose.components.page.recipe.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeGuiAsset
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeSlot
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeTemplateBase
import net.minecraft.resources.Identifier

private val BURN_PROGRESS_LOCATION = Identifier.fromNamespaceAndPath("minecraft", "textures/studio/gui/burn_progres.png")

@Composable
fun SmeltingTemplate(
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
        // TSX: div.flex.flex-col.gap-1.w-full.h-full
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxHeight()
        ) {
            RecipeSlot(item = slots["0"].orEmpty())

            // TSX: div.size-12.flex.items-center.justify-center > img.size-8.pixelated
            Box(
                contentAlignment = androidx.compose.ui.Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                RecipeGuiAsset(
                    location = BURN_PROGRESS_LOCATION,
                    width = 14,
                    height = 14,
                    size = 32.dp
                )
            }

            RecipeSlot(isEmpty = true)
        }
    }
}
