package fr.hardel.asset_editor.client.compose.components.page.recipe

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioMotion
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.ui.ItemSprite
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import fr.hardel.asset_editor.client.compose.components.ui.topLeftBorder
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.RecipeRuntimeEntry
import fr.hardel.asset_editor.client.compose.components.page.recipe.template.RecipeTemplateRegistry
import net.minecraft.client.resources.language.I18n

@Composable
fun RecipeOverviewCard(
    element: RecipeRuntimeEntry,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember(element.id) { MutableInteractionSource() }
    val entryConfig = RecipeTreeData.getEntryByRecipeType(element.type)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .topLeftBorder(2.dp, StudioColors.Zinc900, 12.dp)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onConfigure() }
    ) {
        ShineOverlay(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f), opacity = 0.15f)

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                ItemSprite(entryConfig.assetId, 32.dp, Modifier.size(32.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = I18n.get(entryConfig.translationKey),
                        style = StudioTypography.semiBold(14),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = element.id.toString(),
                        style = StudioTypography.regular(11),
                        color = StudioColors.Zinc500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            RecipeTemplateRegistry.Render(
                kind = RecipeTreeData.getRecipeSerializer(element.visual.type),
                element = element.visual,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            StudioColors.Zinc800.copy(alpha = 0.5f),
                            Offset(0f, 0f),
                            Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(top = 16.dp)
            ) {
                val btnInteraction = remember { MutableInteractionSource() }
                val btnHovered by btnInteraction.collectIsHoveredAsState()
                val btnBg by animateColorAsState(
                    targetValue = if (btnHovered) StudioColors.Zinc800 else StudioColors.Zinc800.copy(alpha = 0.3f),
                    animationSpec = StudioMotion.hoverSpec(),
                    label = "configure-bg"
                )
                val btnBorder by animateColorAsState(
                    targetValue = if (btnHovered) StudioColors.Zinc600 else StudioColors.Zinc700.copy(alpha = 0.5f),
                    animationSpec = StudioMotion.hoverSpec(),
                    label = "configure-border"
                )
                val btnText by animateColorAsState(
                    targetValue = if (btnHovered) Color.White else StudioColors.Zinc300,
                    animationSpec = StudioMotion.hoverSpec(),
                    label = "configure-text"
                )

                Text(
                    text = I18n.get("generic:configure"),
                    style = StudioTypography.medium(12),
                    color = btnText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(btnBg, RoundedCornerShape(8.dp))
                        .border(1.dp, btnBorder, RoundedCornerShape(8.dp))
                        .hoverable(btnInteraction)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
