package fr.hardel.asset_editor.client.compose.components.page.enchantment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryEntries
import fr.hardel.asset_editor.client.memory.session.server.ClientWorkspaceRegistries
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.Identifier
import fr.hardel.asset_editor.AssetEditor

@Composable
fun ExclusiveSingleSection(
    context: StudioContext,
    directExclusiveIds: Set<String>,
    onToggleExclusive: (Identifier) -> Unit
) {
    val entries = rememberRegistryEntries(context, ClientWorkspaceRegistries.ENCHANTMENT)
    val allIds = remember(entries) { entries.map { entry -> entry.id() } }
    val custom = remember(allIds) { allIds.filter { id -> id.namespace != Identifier.DEFAULT_NAMESPACE } }
    val vanilla = remember(allIds) { allIds.filter { id -> id.namespace == Identifier.DEFAULT_NAMESPACE } }

    if (vanilla.isNotEmpty()) {
        EnchantmentCategory(
            title = I18n.get("enchantment.exclusive:vanilla"),
            identifiers = vanilla,
            directExclusiveIds = directExclusiveIds,
            onToggleExclusive = onToggleExclusive,
            context = context
        )
    }

    EnchantmentCategory(
        title = I18n.get("enchantment.exclusive:custom"),
        identifiers = custom,
        directExclusiveIds = directExclusiveIds,
        onToggleExclusive = onToggleExclusive,
        context = context
    )
}
