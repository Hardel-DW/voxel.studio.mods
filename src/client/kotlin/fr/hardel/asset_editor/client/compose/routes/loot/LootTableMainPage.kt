package fr.hardel.asset_editor.client.compose.routes.loot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.data.BaseStudioEditorTabPage
import net.minecraft.client.resources.language.I18n

@Composable
fun LootTableMainPage() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = I18n.get("studio.coming_soon.title"),
            style = VoxelTypography.semiBold(24),
            color = VoxelColors.Zinc400
        )
    }
}

class LootTableMainTabPage : BaseStudioEditorTabPage("loot_table", "main") {
    @Composable
    override fun Render(context: StudioContext) {
        LootTableMainPage()
    }
}
