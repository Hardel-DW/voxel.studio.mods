package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeQuickSwap
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeQuickSwapSwitch
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeSelector
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import fr.hardel.asset_editor.client.compose.components.ui.topLeftBorder
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

@Composable
fun RecipeSection(
    selection: String,
    recipeCounts: Map<String, Int>,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .topLeftBorder(2.dp, StudioColors.Zinc900, 12.dp)
    ) {
        // Shine is pinned to the top with a fixed height so expanding collapsible sections
        // (e.g. advanced options) don't stretch the highlight along with the content.
        ShineOverlay(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(240.dp),
            opacity = 0.12f
        )

        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = I18n.get("recipe:section.title"),
                        style = StudioTypography.bold(20),
                        color = Color.White
                    )
                    Text(
                        text = I18n.get("recipe:section.description"),
                        style = StudioTypography.regular(14),
                        color = StudioColors.Zinc400
                    )
                }

                val quickSwap = remember(selection) {
                    Identifier.tryParse(selection)?.let(RecipeQuickSwap::pairOf)
                }
                if (quickSwap != null) {
                    RecipeQuickSwapSwitch(
                        currentLabel = quickSwap.currentLabelKey,
                        partnerLabel = quickSwap.partnerLabelKey,
                        onSwap = { onSelectionChange(quickSwap.partner.toString()) }
                    )
                }

                RecipeSelector(
                    value = selection,
                    onChange = onSelectionChange,
                    recipeCounts = recipeCounts,
                    selectMode = true
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 16.dp),
                content = content
            )
        }
    }
}
