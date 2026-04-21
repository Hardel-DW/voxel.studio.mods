package fr.hardel.asset_editor.client.compose.components.page.recipe.editor.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import fr.hardel.asset_editor.client.compose.components.page.recipe.RecipeQuickSwap
import fr.hardel.asset_editor.client.compose.components.page.recipe.utils.RecipePageState
import fr.hardel.asset_editor.client.compose.components.ui.AnimatedTabs
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier

@Composable
fun QuickSwapTabs(state: RecipePageState) {
    val recipeType = state.editor.model.type
    val quickSwap = remember(recipeType) {
        Identifier.tryParse(recipeType)?.let(RecipeQuickSwap::pairOf)
    } ?: return

    val partnerType = quickSwap.partner.toString()
    val stableKey = if (recipeType < partnerType) "$recipeType|$partnerType"
    else "$partnerType|$recipeType"

    val tabs = remember(stableKey) {
        val entries = listOf(
            recipeType to I18n.get(quickSwap.currentLabelKey),
            partnerType to I18n.get(quickSwap.partnerLabelKey)
        ).sortedBy { it.first }
        linkedMapOf(*entries.toTypedArray())
    }

    AnimatedTabs(
        options = tabs,
        selectedValue = recipeType,
        onValueChange = { type -> if (type != recipeType) state.onSelectionChange(type) }
    )
}
