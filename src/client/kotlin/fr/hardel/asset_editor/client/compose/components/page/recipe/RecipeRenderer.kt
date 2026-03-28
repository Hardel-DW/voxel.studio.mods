package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import fr.hardel.asset_editor.client.compose.components.page.recipe.model.RecipeVisualModel
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
    onSlotPointerDown: ((String, PointerButton) -> Unit)? = null,
    onSlotPointerEnter: ((String) -> Unit)? = null
) {
    when (RecipeTreeData.getTemplateKind(element.type)) {
        RecipeTreeData.RecipeTemplateKind.SMELTING -> SmeltingTemplate(
            slots = element.slots,
            resultItemId = element.resultItemId,
            resultCount = element.resultCount,
            interactive = interactive,
            onSlotPointerDown = onSlotPointerDown,
            onSlotPointerEnter = onSlotPointerEnter,
            modifier = modifier
        )
        RecipeTreeData.RecipeTemplateKind.STONECUTTING -> StoneCuttingTemplate(
            slots = element.slots,
            resultItemId = element.resultItemId,
            resultCount = element.resultCount,
            interactive = interactive,
            onSlotPointerDown = onSlotPointerDown,
            onSlotPointerEnter = onSlotPointerEnter,
            modifier = modifier
        )
        RecipeTreeData.RecipeTemplateKind.SMITHING -> SmithingTemplate(
            slots = element.slots,
            resultItemId = element.resultItemId,
            resultCount = element.resultCount,
            interactive = interactive,
            onSlotPointerDown = onSlotPointerDown,
            onSlotPointerEnter = onSlotPointerEnter,
            modifier = modifier
        )
        RecipeTreeData.RecipeTemplateKind.CRAFTING -> CraftingTemplate(
            slots = element.slots,
            resultItemId = element.resultItemId,
            resultCount = element.resultCount,
            interactive = interactive,
            onSlotPointerDown = onSlotPointerDown,
            onSlotPointerEnter = onSlotPointerEnter,
            modifier = modifier
        )
    }
}
