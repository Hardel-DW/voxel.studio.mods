package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.page.ChangesPage
import fr.hardel.asset_editor.client.compose.components.page.DebugPage
import fr.hardel.asset_editor.client.compose.components.page.EnchantmentPage
import fr.hardel.asset_editor.client.compose.components.page.LootTablePage
import fr.hardel.asset_editor.client.compose.components.page.NoPermissionPage
import fr.hardel.asset_editor.client.compose.components.page.RecipePage
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import fr.hardel.asset_editor.client.compose.routes.StudioRouter

@Composable
fun ContentOutlet(router: StudioRouter, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        when (val route = router.currentRoute) {
            is StudioRoute.NoPermission -> NoPermissionPage()
            is StudioRoute.Debug -> DebugPage()
            is StudioRoute.ChangesMain -> ChangesPage()

            is StudioRoute.EnchantmentOverview,
            is StudioRoute.EnchantmentMain,
            is StudioRoute.EnchantmentFind,
            is StudioRoute.EnchantmentSlots,
            is StudioRoute.EnchantmentItems,
            is StudioRoute.EnchantmentExclusive,
            is StudioRoute.EnchantmentTechnical,
            is StudioRoute.EnchantmentSimulation -> EnchantmentPage(route)

            is StudioRoute.LootTableOverview,
            is StudioRoute.LootTableMain,
            is StudioRoute.LootTablePools -> LootTablePage(route)

            is StudioRoute.RecipeOverview,
            is StudioRoute.RecipeMain -> RecipePage(route)
        }
    }
}
