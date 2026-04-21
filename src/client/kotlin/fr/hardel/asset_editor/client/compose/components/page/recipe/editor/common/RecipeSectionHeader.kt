package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import net.minecraft.client.resources.language.I18n

@Composable
fun RecipeSectionHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
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
}
