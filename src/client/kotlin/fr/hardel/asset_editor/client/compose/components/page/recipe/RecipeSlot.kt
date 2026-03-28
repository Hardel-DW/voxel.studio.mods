package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import net.minecraft.resources.Identifier

@Composable
fun RecipeSlot(
    slotIndex: String? = null,
    item: List<String> = emptyList(),
    count: Int? = null,
    isEmpty: Boolean = false,
    isResult: Boolean = false,
    interactive: Boolean = false,
    onPointerDown: ((PointerButton) -> Unit)? = null,
    onPointerEnter: (() -> Unit)? = null
) {
    val interaction = remember(slotIndex, item, interactive) { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val displayId = item.firstOrNull()?.let(Identifier::tryParse)

    Box(contentAlignment = Alignment.Center) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(VoxelColors.Zinc800.copy(alpha = 0.5f))
                .border(1.dp, if (hovered) VoxelColors.Zinc500 else VoxelColors.Zinc600, RoundedCornerShape(6.dp))
                .hoverable(interaction)
                .then(
                    if (interactive && (onPointerDown != null || onPointerEnter != null)) {
                        @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
                        Modifier
                            .pointerHoverIcon(PointerIcon.Hand)
                            .onPointerEvent(PointerEventType.Press) { event ->
                                val button = event.button ?: return@onPointerEvent
                                onPointerDown?.invoke(button)
                            }
                            .onPointerEvent(PointerEventType.Enter) {
                                onPointerEnter?.invoke()
                            }
                    } else {
                        Modifier
                    }
                )
        ) {
            when {
                isEmpty -> Unit
                displayId != null -> ItemSprite(displayId, 32.dp)
                item.isNotEmpty() -> Text(
                    text = item.first().removePrefix("#"),
                    style = VoxelTypography.bold(8).copy(fontFamily = FontFamily.Monospace),
                    color = VoxelColors.Zinc400
                )
                isResult -> Text(
                    text = "?",
                    style = VoxelTypography.bold(14),
                    color = VoxelColors.Zinc400
                )
            }
        }

        if ((count ?: item.size) > 1) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(VoxelColors.Zinc900, RoundedCornerShape(4.dp))
                    .border(1.dp, VoxelColors.Zinc600, RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = (count ?: item.size).toString(),
                    style = VoxelTypography.regular(9),
                    color = VoxelColors.Zinc300,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}
