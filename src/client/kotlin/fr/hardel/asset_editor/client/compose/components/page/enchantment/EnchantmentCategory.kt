package fr.hardel.asset_editor.client.compose.components.page.enchantment

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.client.compose.components.ui.Category
import fr.hardel.asset_editor.client.compose.components.ui.InlineCard
import fr.hardel.asset_editor.client.compose.components.ui.LayoutSpec
import fr.hardel.asset_editor.client.compose.components.ui.ResponsiveGrid
import fr.hardel.asset_editor.client.compose.components.ui.BreakpointRule
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.StudioText
import fr.hardel.asset_editor.client.compose.lib.data.StudioBreakpoint
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier

@Composable
fun EnchantmentCategory(
    title: String,
    identifiers: List<Identifier>,
    directExclusiveIds: Set<String>,
    onToggleExclusive: (Identifier) -> Unit,
    context: StudioContext
) {
    Category(title) {
        ResponsiveGrid(
            items = identifiers.map { id ->
                {
                    val name = context.elementStore().get(Registries.ENCHANTMENT, id)?.data()?.description()?.string
                        ?: StudioText.resolve(Registries.ENCHANTMENT, id)
                    InlineCard(
                        title = name,
                        description = id.namespace,
                        active = directExclusiveIds.contains(id.toString()),
                        onActiveChange = { onToggleExclusive(id) }
                    )
                }
            },
            defaultSpec = LayoutSpec.AutoFit(256.dp),
            rules = listOf(BreakpointRule(maxWidth = StudioBreakpoint.XL.px.dp, spec = LayoutSpec.Fixed(floatArrayOf(1f))))
        )
    }
}
