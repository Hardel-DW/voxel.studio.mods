package fr.hardel.asset_editor.client.compose.components.page.recipe.template

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import net.minecraft.resources.Identifier

private val PROGRESS_LOCATION = Identifier.fromNamespaceAndPath("minecraft", "textures/studio/gui/progress.png")

@Composable
fun RecipeTemplateBase(
    resultItemId: String,
    resultCount: Int,
    modifier: Modifier = Modifier,
    interactiveResult: Boolean = false,
    onResultPointerDown: ((PointerButton) -> Unit)? = null,
    onResultPointerEnter: (() -> Unit)? = null,
    child: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .fillMaxWidth()
            .background(VoxelColors.Zinc900.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .border(1.dp, VoxelColors.Zinc700, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        child()

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp)
        ) {
            _root_ide_package_.fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeGuiAsset(
                location = PROGRESS_LOCATION,
                width = 24,
                height = 16,
                size = 32.dp
            )
        }

        _root_ide_package_.fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeSlot(
            item = listOf(resultItemId),
            count = resultCount,
            isResult = true,
            interactive = interactiveResult,
            onPointerDown = onResultPointerDown,
            onPointerEnter = onResultPointerEnter
        )
    }
}
