package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.Button
import fr.hardel.asset_editor.client.compose.components.ui.ButtonSize
import fr.hardel.asset_editor.client.compose.components.ui.ButtonVariant
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import net.minecraft.client.resources.language.I18n

@Composable
fun RecipeOverviewCard(
    element: RecipeRuntimeEntry,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember(element.id) { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    // TSX: div.relative.overflow-hidden.rounded-xl.border.bg-black/35
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .border(1.dp, VoxelColors.Zinc900, RoundedCornerShape(12.dp))
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onConfigure() }
    ) {
        ShineOverlay(modifier = Modifier.matchParentSize(), opacity = if (hovered) 0.18f else 0.12f)

        // TSX: div.flex.flex-col.gap-4.p-4
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = element.id.path,
                    style = VoxelTypography.semiBold(16),
                    color = Color.White
                )
                Text(
                    text = element.serializer,
                    style = VoxelTypography.regular(10).copy(fontFamily = FontFamily.Monospace),
                    color = VoxelColors.Zinc500
                )
            }

            RecipeRenderer(element = element.visual)

            // TSX: button.w-full rounded-lg border border-zinc-800/50 bg-zinc-800/30
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VoxelColors.Zinc800.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .background(VoxelColors.Zinc800.copy(alpha = if (hovered) 0.5f else 0.3f), RoundedCornerShape(8.dp))
            ) {
                Button(
                    onClick = onConfigure,
                    variant = ButtonVariant.TRANSPARENT,
                    size = ButtonSize.SM,
                    text = I18n.get("generic:configure"),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
