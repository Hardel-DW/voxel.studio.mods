package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeSelector
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import net.minecraft.client.resources.language.I18n

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
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .border(1.dp, StudioColors.Zinc900, RoundedCornerShape(12.dp))
    ) {
        ShineOverlay(modifier = Modifier.matchParentSize(), opacity = 0.12f)

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
