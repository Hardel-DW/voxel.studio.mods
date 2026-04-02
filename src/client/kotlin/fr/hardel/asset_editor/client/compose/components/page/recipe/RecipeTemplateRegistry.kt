package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import fr.hardel.asset_editor.client.compose.components.page.recipe.editor.utils.RecipeVisualModel
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.CraftingTemplate
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.SmeltingTemplate
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.SmithingTemplate
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.StoneCuttingTemplate
import fr.hardel.asset_editor.client.compose.lib.data.RecipeTreeData

typealias RecipeTemplateRenderer = @Composable (
    element: RecipeVisualModel,
    modifier: Modifier,
    interactive: Boolean,
    onSlotPointerDown: ((String, PointerButton) -> Unit)?,
    onSlotPointerEnter: ((String) -> Unit)?,
    onResultPointerDown: ((PointerButton) -> Unit)?,
    onResultPointerEnter: (() -> Unit)?
) -> Unit

object RecipeTemplateRegistry {

    private val templates = linkedMapOf<RecipeTreeData.RecipeTemplateKind, RecipeTemplateRenderer>()

    init {
        register(RecipeTreeData.RecipeTemplateKind.CRAFTING) { element, modifier, interactive, onSlotPointerDown, onSlotPointerEnter, onResultPointerDown, onResultPointerEnter ->
            CraftingTemplate(
                slots = element.slots,
                resultItemId = element.resultItemId,
                resultCount = element.resultCount,
                modifier = modifier,
                interactive = interactive,
                onSlotPointerDown = onSlotPointerDown,
                onSlotPointerEnter = onSlotPointerEnter,
                onResultPointerDown = onResultPointerDown,
                onResultPointerEnter = onResultPointerEnter
            )
        }
        register(RecipeTreeData.RecipeTemplateKind.SMELTING) { element, modifier, interactive, onSlotPointerDown, onSlotPointerEnter, onResultPointerDown, onResultPointerEnter ->
            SmeltingTemplate(
                slots = element.slots,
                resultItemId = element.resultItemId,
                resultCount = element.resultCount,
                modifier = modifier,
                interactive = interactive,
                onSlotPointerDown = onSlotPointerDown,
                onSlotPointerEnter = onSlotPointerEnter,
                onResultPointerDown = onResultPointerDown,
                onResultPointerEnter = onResultPointerEnter
            )
        }
        register(RecipeTreeData.RecipeTemplateKind.SMITHING) { element, modifier, interactive, onSlotPointerDown, onSlotPointerEnter, onResultPointerDown, onResultPointerEnter ->
            SmithingTemplate(
                slots = element.slots,
                resultItemId = element.resultItemId,
                resultCount = element.resultCount,
                modifier = modifier,
                interactive = interactive,
                onSlotPointerDown = onSlotPointerDown,
                onSlotPointerEnter = onSlotPointerEnter,
                onResultPointerDown = onResultPointerDown,
                onResultPointerEnter = onResultPointerEnter
            )
        }
        register(RecipeTreeData.RecipeTemplateKind.STONECUTTING) { element, modifier, interactive, onSlotPointerDown, onSlotPointerEnter, onResultPointerDown, onResultPointerEnter ->
            StoneCuttingTemplate(
                slots = element.slots,
                resultItemId = element.resultItemId,
                resultCount = element.resultCount,
                modifier = modifier,
                interactive = interactive,
                onSlotPointerDown = onSlotPointerDown,
                onSlotPointerEnter = onSlotPointerEnter,
                onResultPointerDown = onResultPointerDown,
                onResultPointerEnter = onResultPointerEnter
            )
        }
    }

    fun register(kind: RecipeTreeData.RecipeTemplateKind, renderer: RecipeTemplateRenderer) {
        require(templates.putIfAbsent(kind, renderer) == null) {
            "Recipe template already registered for $kind"
        }
    }

    @Composable
    fun Render(
        kind: RecipeTreeData.RecipeTemplateKind,
        element: RecipeVisualModel,
        modifier: Modifier = Modifier,
        interactive: Boolean = false,
        onSlotPointerDown: ((String, PointerButton) -> Unit)? = null,
        onSlotPointerEnter: ((String) -> Unit)? = null,
        onResultPointerDown: ((PointerButton) -> Unit)? = null,
        onResultPointerEnter: (() -> Unit)? = null
    ) {
        templates[kind]?.invoke(
            element,
            modifier,
            interactive,
            onSlotPointerDown,
            onSlotPointerEnter,
            onResultPointerDown,
            onResultPointerEnter
        )
    }
}
