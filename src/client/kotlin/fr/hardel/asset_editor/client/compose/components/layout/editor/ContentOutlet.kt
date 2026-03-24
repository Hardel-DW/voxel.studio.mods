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
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.rememberCurrentDestination
import fr.hardel.asset_editor.client.navigation.ConceptChangesDestination
import fr.hardel.asset_editor.client.navigation.ConceptOverviewDestination
import fr.hardel.asset_editor.client.navigation.DebugDestination
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import fr.hardel.asset_editor.client.navigation.NoPermissionDestination
import fr.hardel.asset_editor.client.compose.routes.debug.DebugLayout

@Composable
fun ContentOutlet(context: StudioContext, modifier: Modifier = Modifier) {
    val destination = rememberCurrentDestination(context)

    // Compose-only: route outlet / switch de pages, équivalent conceptuel du <Outlet /> web.
    Box(modifier = modifier.fillMaxSize()) {
        when (destination) {
            is NoPermissionDestination -> NoPermissionPage()
            is DebugDestination -> DebugLayout(context)
            is ConceptChangesDestination -> ChangesLayout()
            is ConceptOverviewDestination -> {
                when (destination.concept) {
                    StudioConcept.ENCHANTMENT -> EnchantmentLayout(context)
                    StudioConcept.LOOT_TABLE -> LootTableLayout(context)
                    StudioConcept.RECIPE -> RecipeLayout(context)
                    else -> NoPermissionPage()
                }
            }

            is ElementEditorDestination -> {
                when (destination.concept) {
                    StudioConcept.ENCHANTMENT -> EnchantmentLayout(context)
                    StudioConcept.LOOT_TABLE -> LootTableLayout(context)
                    StudioConcept.RECIPE -> RecipeLayout(context)
                    else -> NoPermissionPage()
                }
            }
        }
    }
}
