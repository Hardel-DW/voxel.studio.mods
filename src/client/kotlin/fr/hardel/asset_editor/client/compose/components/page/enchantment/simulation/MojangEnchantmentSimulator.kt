package fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation

import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistries
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import net.minecraft.client.Minecraft
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.EnchantmentTags
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantable
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper

enum class SimulationMode { ALL_REGISTRIES, CURRENT_PACK_ONLY }

data class SimulationSlotRange(val slot: Int, val minLevel: Int, val maxLevel: Int)

data class SimulationEntry(val enchantmentId: Identifier, val level: Int)

data class SimulationOption(val cost: Int, val entries: ImmutableList<SimulationEntry>)

data class SimulationStats(
    val enchantmentId: Identifier,
    val probability: Double,
    val averageLevel: Double,
    val minLevel: Int,
    val maxLevel: Int
)

object MojangEnchantmentSimulator {

    private const val MAX_BOOKSHELVES = 15
    private const val DEFAULT_ITERATIONS = 5000

    fun slotRanges(bookshelves: Int): ImmutableList<SimulationSlotRange> {
        val clamped = bookshelves.coerceIn(0, MAX_BOOKSHELVES)
        val minBase = 1 + clamped / 2
        val maxBase = 8 + clamped / 2 + clamped
        val topMin = (minBase / 3).coerceAtLeast(1)
        val topMax = (maxBase / 3).coerceAtLeast(1)
        val midMin = minBase * 2 / 3 + 1
        val midMax = maxBase * 2 / 3 + 1
        val botMin = maxOf(minBase, clamped * 2)
        val botMax = maxOf(maxBase, clamped * 2)
        return persistentListOf(
            SimulationSlotRange(0, topMin, topMax),
            SimulationSlotRange(1, midMin, midMax),
            SimulationSlotRange(2, botMin, botMax)
        )
    }

    fun runOnce(
        itemId: Identifier,
        enchantability: Int,
        bookshelves: Int,
        slot: Int,
        mode: SimulationMode
    ): SimulationOption {
        val stack = makeStack(itemId, enchantability) ?: return SimulationOption(0, persistentListOf())
        val holders = enchantmentHolders(mode)
        if (holders.isEmpty()) return SimulationOption(0, persistentListOf())
        val random = RandomSource.create()
        val cost = EnchantmentHelper.getEnchantmentCost(random, slot, bookshelves.coerceIn(0, MAX_BOOKSHELVES), stack)
        if (cost <= 0) return SimulationOption(0, persistentListOf())
        val results = EnchantmentHelper.selectEnchantment(random, stack, cost, holders.stream())
        val entries = results.mapNotNull { instance ->
            val id = holderId(instance.enchantment()) ?: return@mapNotNull null
            SimulationEntry(id, instance.level())
        }
        return SimulationOption(cost = cost, entries = entries.toImmutableList())
    }

    fun monteCarlo(
        itemId: Identifier,
        enchantability: Int,
        bookshelves: Int,
        slot: Int,
        mode: SimulationMode,
        iterations: Int = DEFAULT_ITERATIONS
    ): ImmutableList<SimulationStats> {
        val stack = makeStack(itemId, enchantability) ?: return persistentListOf()
        val holders = enchantmentHolders(mode)
        if (holders.isEmpty()) return persistentListOf()
        val random = RandomSource.create()
        val clamped = bookshelves.coerceIn(0, MAX_BOOKSHELVES)
        val acc = HashMap<Identifier, Accumulator>()

        repeat(iterations) {
            val cost = EnchantmentHelper.getEnchantmentCost(random, slot, clamped, stack)
            if (cost <= 0) return@repeat
            val results = EnchantmentHelper.selectEnchantment(random, stack, cost, holders.stream())
            for (instance in results) {
                val id = holderId(instance.enchantment()) ?: continue
                val cell = acc.getOrPut(id) { Accumulator() }
                cell.count++
                cell.totalLevel += instance.level()
                if (instance.level() < cell.minLevel) cell.minLevel = instance.level()
                if (instance.level() > cell.maxLevel) cell.maxLevel = instance.level()
            }
        }

        return acc.entries
            .asSequence()
            .map { (id, cell) ->
                SimulationStats(
                    enchantmentId = id,
                    probability = cell.count.toDouble() / iterations.toDouble() * 100.0,
                    averageLevel = cell.totalLevel.toDouble() / cell.count.toDouble(),
                    minLevel = cell.minLevel,
                    maxLevel = cell.maxLevel
                )
            }
            .sortedByDescending(SimulationStats::probability)
            .toList()
            .toImmutableList()
    }

    fun availableItems(mode: SimulationMode): ImmutableList<Identifier> {
        val registryAccess = Minecraft.getInstance().connection?.registryAccess() ?: return persistentListOf()
        val enchantments = registryAccess.lookupOrThrow(Registries.ENCHANTMENT)
        val editedIds: Set<Identifier> = ClientWorkspaceRegistries.ENCHANTMENT.modifiedIdsSnapshot()
        val items = sortedSetOf<Identifier>(compareBy(Identifier::toString))
        enchantments.listElements().forEach { holder ->
            val id = holderId(holder) ?: return@forEach
            if (mode == SimulationMode.CURRENT_PACK_ONLY && id !in editedIds) return@forEach
            val definition = holder.value().definition()
            val source = definition.primaryItems().orElse(definition.supportedItems())
            source.stream().forEach { itemHolder ->
                itemHolder.unwrapKey().ifPresent { key -> items += key.identifier() }
            }
        }
        return items.toImmutableList()
    }

    fun enchantabilityOf(itemId: Identifier): Int? {
        val stack = lookupStack(itemId) ?: return null
        return stack.get(DataComponents.ENCHANTABLE)?.value()
    }

    private fun enchantmentHolders(mode: SimulationMode): List<Holder<Enchantment>> {
        val registryAccess = Minecraft.getInstance().connection?.registryAccess() ?: return emptyList()
        val registry = registryAccess.lookupOrThrow(Registries.ENCHANTMENT)
        val tag = registry.get(EnchantmentTags.IN_ENCHANTING_TABLE).orElse(null) ?: return emptyList()
        val all = tag.stream().toList()
        if (mode == SimulationMode.ALL_REGISTRIES) return all
        val editedIds: Set<Identifier> = ClientWorkspaceRegistries.ENCHANTMENT.modifiedIdsSnapshot()
        return all.filter { holder ->
            holder.unwrapKey().map { editedIds.contains(it.identifier()) }.orElse(false)
        }
    }

    private fun makeStack(itemId: Identifier, enchantability: Int): ItemStack? {
        val stack = lookupStack(itemId) ?: return null
        stack.set(DataComponents.ENCHANTABLE, Enchantable(enchantability.coerceAtLeast(1)))
        return stack
    }

    private fun lookupStack(itemId: Identifier): ItemStack? {
        val registryAccess = Minecraft.getInstance().connection?.registryAccess() ?: return null
        val key = ResourceKey.create(Registries.ITEM, itemId)
        val holder = registryAccess.lookupOrThrow(Registries.ITEM).get(key).orElse(null) ?: return null
        return ItemStack(holder.value())
    }

    private fun holderId(holder: Holder<Enchantment>): Identifier? =
        holder.unwrapKey().map { it.identifier() }.orElse(null)

    private class Accumulator {
        var count = 0
        var totalLevel = 0
        var minLevel = Int.MAX_VALUE
        var maxLevel = 0
    }
}
