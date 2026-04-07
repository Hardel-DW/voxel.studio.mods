package fr.hardel.asset_editor.client.compose.routes.loot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.StudioColors
import fr.hardel.asset_editor.client.compose.StudioTypography
import net.minecraft.client.resources.language.I18n

@Composable
fun LootTableMainPage() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = I18n.get("studio.coming_soon.title"),
            style = StudioTypography.semiBold(24),
            color = StudioColors.Zinc400
        )
    }
}
