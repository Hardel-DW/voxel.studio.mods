package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay
import fr.hardel.asset_editor.client.compose.components.ui.topLeftBorder

@Composable
fun RecipeSectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .topLeftBorder(2.dp, StudioColors.Zinc900, 12.dp)
    ) {
        ShineOverlay(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(240.dp),
            opacity = 0.12f
        )

        Column(modifier = Modifier.padding(24.dp), content = content)
    }
}
