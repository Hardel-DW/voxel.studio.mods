package fr.hardel.asset_editor.client.compose.components.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.routes.StudioRoute

@Composable
fun NoPermissionPage() {
    PlaceholderContent("No Permission")
}

@Composable
fun DebugPage() {
    PlaceholderContent("Debug")
}

@Composable
fun ChangesPage() {
    PlaceholderContent("Changes")
}

@Composable
fun EnchantmentPage(route: StudioRoute) {
    PlaceholderContent("Enchantment — ${route::class.simpleName}")
}

@Composable
fun LootTablePage(route: StudioRoute) {
    PlaceholderContent("Loot Table — ${route::class.simpleName}")
}

@Composable
fun RecipePage(route: StudioRoute) {
    PlaceholderContent("Recipe — ${route::class.simpleName}")
}

@Composable
private fun PlaceholderContent(label: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = VoxelTypography.semiBold(24),
                color = VoxelColors.Zinc400
            )
            Text(
                text = "Placeholder",
                style = VoxelTypography.regular(14),
                color = VoxelColors.Zinc600
            )
        }
    }
}
