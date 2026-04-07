package fr.hardel.asset_editor.client.compose.components.page.recipe.template

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeSlot
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.RecipeTemplateBase

@Composable
fun StoneCuttingTemplate(
    slots: Map<String, List<String>>,
    resultItemId: String,
    resultCount: Int,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onSlotPointerDown: ((String, PointerButton) -> Unit)? = null,
    onSlotPointerEnter: ((String) -> Unit)? = null,
    onResultPointerDown: ((PointerButton) -> Unit)? = null,
    onResultPointerEnter: (() -> Unit)? = null
) {
    RecipeTemplateBase(
        resultItemId = resultItemId,
        resultCount = resultCount,
        modifier = modifier,
        interactiveResult = interactive,
        onResultPointerDown = onResultPointerDown,
        onResultPointerEnter = onResultPointerEnter
    ) {
        RecipeSlot(
            slotIndex = "0",
            item = slots["0"].orEmpty(),
            interactive = interactive,
            onPointerDown = onSlotPointerDown?.let { cb -> { button -> cb("0", button) } },
            onPointerEnter = onSlotPointerEnter?.let { cb -> { cb("0") } }
        )
    }
}
