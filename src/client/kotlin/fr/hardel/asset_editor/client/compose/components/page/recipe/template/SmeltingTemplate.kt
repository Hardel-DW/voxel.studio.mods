package fr.hardel.asset_editor.client.compose.components.page.recipe.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeGuiAsset
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeSlot
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.RecipeTemplateBase
import net.minecraft.resources.Identifier

private val BURN_PROGRESS_LOCATION =
    Identifier.fromNamespaceAndPath(Identifier.DEFAULT_NAMESPACE, "textures/studio/gui/burn_progres.png")

@Composable
fun SmeltingTemplate(
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            RecipeSlot(
                slotIndex = "0",
                item = slots["0"].orEmpty(),
                interactive = interactive,
                onPointerDown = onSlotPointerDown?.let { cb -> { button -> cb("0", button) } },
                onPointerEnter = onSlotPointerEnter?.let { cb -> { cb("0") } }
            )

            Box(
                contentAlignment = Alignment.Center,
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
