package fr.hardel.asset_editor.client.compose.components.page.enchantment

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier

@Composable
fun ExclusiveSingleSection(
    context: StudioContext,
    directExclusiveIds: Set<String>,
    onToggleExclusive: (Identifier) -> Unit
) {
    val allIds = context.allTypedEntries(Registries.ENCHANTMENT).map { entry -> entry.id() }
    val custom = allIds.filter { id -> id.namespace != "minecraft" }
    val vanilla = allIds.filter { id -> id.namespace == "minecraft" }

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
