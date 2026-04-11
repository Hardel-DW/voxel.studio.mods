package fr.hardel.asset_editor.client.compose.components.page.loot_table

import fr.hardel.asset_editor.workspace.action.loot_table.EntryPath
import net.minecraft.resources.Identifier
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer
import net.minecraft.world.level.storage.loot.entries.NestedLootTable
import net.minecraft.world.level.storage.loot.entries.TagEntry
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator

enum class RewardKind { ITEM, TAG, LOOT_TABLE, UNRESOLVED }

data class FlattenedReward(
    val name: Identifier,
    val kind: RewardKind,
    val weight: Int,
    val probability: Double,
    val poolIndex: Int,
    val entryPath: EntryPath,
    val nestedSource: Identifier? = null,
    val countMin: Int = 1,
    val countMax: Int = 1
)

object LootTableFlattener {

    fun flatten(table: LootTable): List<FlattenedReward> {
        val results = mutableListOf<FlattenedReward>()
        for ((poolIndex, pool) in table.pools.withIndex()) {
            flattenPool(pool.entries, poolIndex, results)
        }
        return results
    }

    fun firstPreviewItem(table: LootTable): Identifier? {
        for (pool in table.pools) {
            val found = firstFromEntries(pool.entries)
            if (found != null) return found
        }
        return null
    }

    private fun flattenPool(
        entries: List<LootPoolEntryContainer>,
        poolIndex: Int,
        output: MutableList<FlattenedReward>
    ) {
        val weights = entries.map(::entryWeight)
        val totalWeight = weights.sum()
        if (totalWeight == 0) return
        for ((idx, entry) in entries.withIndex()) {
            val path = EntryPath.ofTopLevel(poolIndex, idx)
            flattenEntry(entry, poolIndex, weights[idx].toDouble() / totalWeight, weights[idx], path, output)
        }
    }

    private fun flattenEntry(
        entry: LootPoolEntryContainer,
        poolIndex: Int,
        probability: Double,
        weight: Int,
        path: EntryPath,
        output: MutableList<FlattenedReward>
    ) {
        val (cMin, cMax) = countRange(entry)
        when (entry) {
            is LootItem -> {
                val id = entry.item.unwrapKey().orElse(null)?.identifier()
                    ?: Identifier.withDefaultNamespace("unknown")
                output.add(FlattenedReward(id, RewardKind.ITEM, weight, probability, poolIndex, path, countMin = cMin, countMax = cMax))
            }
            is TagEntry -> {
                output.add(FlattenedReward(entry.tag.location(), RewardKind.TAG, weight, probability, poolIndex, path, countMin = cMin, countMax = cMax))
            }
            is NestedLootTable -> {
                val ref = entry.contents.left().orElse(null)?.identifier()
                val name = ref ?: Identifier.withDefaultNamespace("inline_table")
                output.add(FlattenedReward(name, RewardKind.LOOT_TABLE, weight, probability, poolIndex, path, nestedSource = ref, countMin = cMin, countMax = cMax))
            }
            is CompositeEntryBase -> flattenComposite(entry, poolIndex, probability, path, output)
        }
    }

    private fun flattenComposite(
        entry: CompositeEntryBase,
        poolIndex: Int,
        probability: Double,
        parentPath: EntryPath,
        output: MutableList<FlattenedReward>
    ) {
        val children = entry.children
        val childWeights = children.map(::entryWeight)
        val totalChild = childWeights.sum()
        if (totalChild == 0) return
        for ((idx, child) in children.withIndex()) {
            val childPath = EntryPath(parentPath.poolIndex(), parentPath.childIndices() + idx)
            flattenEntry(child, poolIndex, probability * childWeights[idx] / totalChild, childWeights[idx], childPath, output)
        }
    }

    private fun countRange(entry: LootPoolEntryContainer): Pair<Int, Int> {
        if (entry !is LootPoolSingletonContainer) return 1 to 1
        val setCount = entry.functions.filterIsInstance<SetItemCountFunction>().firstOrNull() ?: return 1 to 1
        return extractRange(setCount)
    }

    private fun extractRange(func: SetItemCountFunction): Pair<Int, Int> =
        when (val provider = func.value) {
            is ConstantValue -> provider.value().toInt().let { it to it }
            is UniformGenerator -> {
                val min = if (provider.min() is ConstantValue) (provider.min() as ConstantValue).value().toInt() else 1
                val max = if (provider.max() is ConstantValue) (provider.max() as ConstantValue).value().toInt() else 1
                min to max
            }
            else -> 1 to 1
        }

    private fun entryWeight(entry: LootPoolEntryContainer): Int =
        if (entry is LootPoolSingletonContainer) entry.weight else 1

    private fun firstFromEntries(entries: List<LootPoolEntryContainer>): Identifier? {
        for (entry in entries) {
            val found = firstFromEntry(entry)
            if (found != null) return found
        }
        return null
    }

    private fun firstFromEntry(entry: LootPoolEntryContainer): Identifier? = when (entry) {
        is LootItem -> entry.item.unwrapKey().orElse(null)?.identifier()
        is CompositeEntryBase -> firstFromEntries(entry.children)
        else -> null
    }
}
