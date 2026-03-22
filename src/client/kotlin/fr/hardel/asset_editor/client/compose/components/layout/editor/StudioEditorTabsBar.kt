package fr.hardel.asset_editor.client.compose.components.layout.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.layout.loading.WindowControls
import fr.hardel.asset_editor.client.compose.components.ui.ResourceImageIcon
import fr.hardel.asset_editor.client.compose.components.ui.SvgIcon
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.StudioText
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.data.StudioElementId
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import net.minecraft.resources.Identifier

private val CLOSE_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/close.svg")

@Composable
fun StudioEditorTabsBar(context: StudioContext, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(start = 8.dp)
    ) {
        PackSelector(context = context)
        Box(
            modifier = Modifier
                .size(width = 1.dp, height = 24.dp)
                .background(Color(0xFF3F3F46))
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            context.openTabs.forEachIndexed { index, tab ->
                StudioEditorTabItem(
                    context = context,
                    tab = tab,
                    index = index,
                    active = index == context.activeTabIndex
                )
            }
        }

        Spacer(Modifier.weight(1f))

        WindowControls(buttonWidth = 48.dp, buttonHeight = 48.dp)
    }
}

@Composable
private fun StudioEditorTabItem(
    context: StudioContext,
    tab: StudioContext.OpenTab,
    index: Int,
    active: Boolean
) {
    val concept = StudioConcept.byRoute(tab.route)
    val parsed = StudioElementId.parse(tab.elementId)
    val label = if (parsed != null) {
        StudioText.resolve(concept.registryKey, parsed.identifier)
    } else {
        tab.elementId
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(
                color = if (active) Color(0xFF232328) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                context.tabsState().switchTab(index)
                context.router.navigate(tab.route)
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        ResourceImageIcon(concept.icon, 16.dp)
        Text(
            text = label,
            style = VoxelTypography.medium(14),
            color = if (active) VoxelColors.Zinc100 else VoxelColors.Zinc400,
            maxLines = 1
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(16.dp)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    context.tabsState().closeTab(index)
                    val next = context.tabsState().activeTab()
                    val fallback = StudioRoute.overviewOf(context.router.currentRoute.concept)
                    context.router.navigate(next?.route()?.toComposeRoute() ?: fallback)
                }
        ) {
            SvgIcon(
                location = CLOSE_ICON,
                size = 10.dp,
                tint = if (active) VoxelColors.Zinc200 else VoxelColors.Zinc400
            )
        }
    }
}

private fun fr.hardel.asset_editor.client.javafx.routes.StudioRoute.toComposeRoute(): StudioRoute =
    when (this) {
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_OVERVIEW -> StudioRoute.EnchantmentOverview
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_MAIN -> StudioRoute.EnchantmentMain
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_FIND -> StudioRoute.EnchantmentFind
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_SLOTS -> StudioRoute.EnchantmentSlots
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_ITEMS -> StudioRoute.EnchantmentItems
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_EXCLUSIVE -> StudioRoute.EnchantmentExclusive
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_TECHNICAL -> StudioRoute.EnchantmentTechnical
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.ENCHANTMENT_SIMULATION -> StudioRoute.EnchantmentSimulation
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.LOOT_TABLE_OVERVIEW -> StudioRoute.LootTableOverview
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.LOOT_TABLE_MAIN -> StudioRoute.LootTableMain
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.LOOT_TABLE_POOLS -> StudioRoute.LootTablePools
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.RECIPE_OVERVIEW -> StudioRoute.RecipeOverview
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.RECIPE_MAIN -> StudioRoute.RecipeMain
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.CHANGES_MAIN -> StudioRoute.ChangesMain
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.DEBUG -> StudioRoute.Debug
        fr.hardel.asset_editor.client.javafx.routes.StudioRoute.NO_PERMISSION -> StudioRoute.NoPermission
    }
