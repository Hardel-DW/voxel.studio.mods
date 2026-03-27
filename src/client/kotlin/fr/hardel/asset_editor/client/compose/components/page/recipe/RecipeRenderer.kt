package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.CraftingTemplate
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.SmeltingTemplate
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.SmithingTemplate
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.StoneCuttingTemplate
import fr.hardel.asset_editor.client.compose.lib.data.RecipeTreeData

@Composable
fun RecipeRenderer(
    element: RecipeVisualModel,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onSlotClick: ((String) -> Unit)? = null
) {
    // TSX: RecipeRenderer switch on recipe block kind -> dedicated template component
    when (RecipeTreeData.getBlockByRecipeType(element.type).blockId.toString()) {
        "minecraft:furnace",
        "minecraft:blast_furnace",
        "minecraft:smoker",
        "minecraft:campfire" -> SmeltingTemplate(
            slots = element.slots,
            resultItemId = element.resultItemId,
            resultCount = element.resultCount,
            modifier = modifier
        )

        "minecraft:stonecutter" -> StoneCuttingTemplate(
            slots = element.slots,
            resultItemId = element.resultItemId,
            resultCount = element.resultCount,
            modifier = modifier
        )

        "minecraft:smithing_table" -> SmithingTemplate(
            slots = element.slots,
            resultItemId = element.resultItemId,
            resultCount = element.resultCount,
            modifier = modifier
        )

        else -> CraftingTemplate(
            slots = element.slots,
            resultItemId = element.resultItemId,
            resultCount = element.resultCount,
            modifier = modifier,
            interactive = interactive,
            onSlotClick = onSlotClick
        )
    }
}
