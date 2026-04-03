package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.RecipeVisualModel
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeTreeData

@Composable
fun RecipeRenderer(
    element: RecipeVisualModel,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onSlotPointerDown: ((String, PointerButton) -> Unit)? = null,
    onSlotPointerEnter: ((String) -> Unit)? = null,
    onResultPointerDown: ((PointerButton) -> Unit)? = null,
    onResultPointerEnter: (() -> Unit)? = null
) {
    RecipeTemplateRegistry.Render(
        kind = RecipeTreeData.getTemplateKind(element.type),
        element = element,
        modifier = modifier,
        interactive = interactive,
        onSlotPointerDown = onSlotPointerDown,
        onSlotPointerEnter = onSlotPointerEnter,
        onResultPointerDown = onResultPointerDown,
        onResultPointerEnter = onResultPointerEnter
    )
}
