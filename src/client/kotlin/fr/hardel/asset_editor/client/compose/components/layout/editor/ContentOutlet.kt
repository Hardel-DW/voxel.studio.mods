package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.hardel.asset_editor.client.compose.components.page.changes.ChangesLayout
import fr.hardel.asset_editor.client.compose.components.page.enchantment.EnchantmentLayout
import fr.hardel.asset_editor.client.compose.components.page.loot_table.LootTableLayout
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeLayout
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import fr.hardel.asset_editor.client.compose.routes.debug.DebugLayout

@Composable
fun ContentOutlet(context: StudioContext, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        when (val route = context.router.currentRoute) {
            StudioRoute.NoPermission -> NoPermissionPage()
            StudioRoute.Debug -> DebugLayout(context)
            StudioRoute.ChangesMain -> ChangesLayout()
            else -> when (route.concept()) {
                "enchantment" -> EnchantmentLayout(context)
                "loot_table" -> LootTableLayout(context)
                "recipe" -> RecipeLayout(context)
                else -> NoPermissionPage()
            }
        }
    }
}
