package fr.hardel.asset_editor.client.compose.components.page.enchantment

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import fr.hardel.asset_editor.AssetEditor
import fr.hardel.asset_editor.client.compose.VoxelColors
import fr.hardel.asset_editor.client.compose.VoxelTypography
import fr.hardel.asset_editor.client.compose.components.ui.Category
import fr.hardel.asset_editor.client.compose.components.ui.BreakpointRule
import fr.hardel.asset_editor.client.compose.components.ui.LayoutSpec
import fr.hardel.asset_editor.client.compose.components.ui.ResponsiveGrid
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import fr.hardel.asset_editor.client.compose.lib.StudioText
import fr.hardel.asset_editor.client.compose.lib.rememberRegistryEntries
import fr.hardel.asset_editor.client.compose.lib.data.ExclusiveSetGroup
import fr.hardel.asset_editor.client.compose.lib.data.StudioBreakpoint
import fr.hardel.asset_editor.store.adapter.EnchantmentFlushAdapter
import java.util.ArrayList
import java.util.LinkedHashMap
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier

@Composable
fun ExclusiveGroupSection(
    context: StudioContext,
    currentExclusiveTag: String,
    currentTags: Set<Identifier>,
    onTargetToggle: (Identifier, Boolean) -> Unit,
    onMembershipToggle: (Identifier, Boolean) -> Unit
) {
    val allEntries = rememberRegistryEntries(context, Registries.ENCHANTMENT)
    val membersByTag = remember(allEntries) { tagMembers(allEntries) }
    val customTags = remember(allEntries) { EnchantmentFlushAdapter.customExclusiveTags(allEntries) }
    val labelResolver: (String) -> String = { value ->
        if (value.isBlank()) {
            ""
        } else {
            val parsed = Identifier.tryParse(value.removePrefix("#"))
            if (parsed == null) {
                value
            } else {
                context.resolveHolder(Registries.ENCHANTMENT, parsed)
                    .map { holder -> holder.value().description().string }
                    .orElseGet { StudioText.resolve(Registries.ENCHANTMENT, parsed) }
            }
        }
    }

    Category(I18n.get("enchantment.exclusive:vanilla")) {
        ResponsiveGrid(
            items = ExclusiveSetGroup.ALL.map { group ->
                {
                    EnchantmentTags(
                        title = StudioText.resolve("enchantment_tag", group.tagId),
                        description = I18n.get("enchantment_tag:${group.tagId}.desc"),
                        imageId = group.image(),
                        values = membersByTag[group.tagId].orEmpty(),
                        isTarget = currentExclusiveTag == group.tagId.toString(),
                        isMember = currentTags.contains(group.tagId),
                        locked = false,
                        onTargetToggle = { checked -> onTargetToggle(group.tagId, checked) },
                        onMembershipToggle = { checked -> onMembershipToggle(group.tagId, checked) },
                        labelResolver = labelResolver
                    )
                }
            },
            defaultSpec = LayoutSpec.AutoFit(256.dp),
            rules = listOf(BreakpointRule(maxWidth = StudioBreakpoint.XL.px.dp, spec = LayoutSpec.Fixed(floatArrayOf(1f))))
        )
    }

    Category(I18n.get("enchantment.exclusive:custom")) {
        if (customTags.isEmpty()) {
            Text(
                text = I18n.get("enchantment.exclusive:custom.fallback"),
                style = VoxelTypography.regular(13),
                color = VoxelColors.Zinc400
            )
        } else {
            ResponsiveGrid(
                items = customTags.map { tagId ->
                    {
                        EnchantmentTags(
                            title = StudioText.resolve("enchantment_tag", tagId),
                            description = tagId.toString(),
                            imageId = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/logo.svg"),
                            values = membersByTag[tagId].orEmpty(),
                            isTarget = currentExclusiveTag == tagId.toString(),
                            isMember = currentTags.contains(tagId),
                            locked = false,
                            onTargetToggle = { checked -> onTargetToggle(tagId, checked) },
                            onMembershipToggle = { checked -> onMembershipToggle(tagId, checked) },
                            labelResolver = labelResolver
                        )
                    }
                },
                defaultSpec = LayoutSpec.AutoFit(256.dp),
                rules = listOf(BreakpointRule(maxWidth = StudioBreakpoint.XL.px.dp, spec = LayoutSpec.Fixed(floatArrayOf(1f))))
            )
        }
    }
}

private fun tagMembers(
    entries: List<fr.hardel.asset_editor.store.ElementEntry<net.minecraft.world.item.enchantment.Enchantment>>
): Map<Identifier, List<String>> {
    val members = LinkedHashMap<Identifier, MutableList<String>>()
    entries.forEach { entry ->
        val elementId = entry.id().toString()
        entry.tags().forEach { tagId ->
            members.getOrPut(tagId) { ArrayList() }.add(elementId)
        }
    }
    return members
}
