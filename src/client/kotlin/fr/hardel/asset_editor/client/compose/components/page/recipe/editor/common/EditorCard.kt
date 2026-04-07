package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.components.ui.ShineOverlay

@Composable
fun EditorCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(Color.Transparent, RoundedCornerShape(8.dp))
            .border(1.dp, StudioColors.Zinc900, RoundedCornerShape(8.dp))
    ) {
        ShineOverlay(modifier = Modifier.matchParentSize(), opacity = 0.1f)
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
