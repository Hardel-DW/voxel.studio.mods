package fr.hardel.asset_editor.client.compose.routes.enchantment.simulation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation.MojangEnchantmentSimulator
import fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation.SimulationMode
import fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation.SimulationOption
import fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation.SimulationSlotRange
import fr.hardel.asset_editor.client.compose.components.page.enchantment.simulation.SimulationStats
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.resources.Identifier

private val DEFAULT_ITEM: Identifier = Identifier.withDefaultNamespace("diamond_sword")
private const val DEFAULT_BOOKSHELVES = 15
private const val DEFAULT_ENCHANTABILITY = 10
private const val MAX_BOOKSHELVES = 15
private const val MIN_ENCHANTABILITY = 1
private const val MAX_ENCHANTABILITY = 80

@Stable
class EnchantmentSimulationState(private val scope: CoroutineScope) {

    var bookshelves by mutableIntStateOf(DEFAULT_BOOKSHELVES)
        private set
    var enchantability by mutableIntStateOf(DEFAULT_ENCHANTABILITY)
        private set
    var itemId by mutableStateOf(DEFAULT_ITEM)
        private set
    var mode by mutableStateOf(SimulationMode.ALL_REGISTRIES)
        private set
    var showTooltip by mutableStateOf(false)
        private set
    var slotRanges: ImmutableList<SimulationSlotRange> by mutableStateOf(
        MojangEnchantmentSimulator.slotRanges(DEFAULT_BOOKSHELVES)
    )
        private set
    var currentOption by mutableStateOf<SimulationOption?>(null)
        private set
    var stats: ImmutableList<SimulationStats> by mutableStateOf(persistentListOf())
        private set
    var selectedSlot by mutableStateOf<Int?>(null)
        private set
    var isSimulating by mutableStateOf(false)
        private set

    private var pendingJob: Job? = null

    fun updateBookshelves(value: Int) {
        val coerced = value.coerceIn(0, MAX_BOOKSHELVES)
        if (coerced == bookshelves) return
        bookshelves = coerced
        slotRanges = MojangEnchantmentSimulator.slotRanges(coerced)
    }

    fun updateEnchantability(value: Int) {
        enchantability = value.coerceIn(MIN_ENCHANTABILITY, MAX_ENCHANTABILITY)
    }

    fun updateItem(id: Identifier) {
        itemId = id
        MojangEnchantmentSimulator.enchantabilityOf(id)?.let { enchantability = it }
    }

    fun updateMode(value: SimulationMode) {
        if (value == mode) return
        mode = value
        selectedSlot?.let { runSimulation(it) }
    }

    fun toggleTooltip() {
        showTooltip = !showTooltip
    }

    fun updateTooltip(value: Boolean) {
        showTooltip = value
    }

    fun runSimulation(slot: Int) {
        pendingJob?.cancel()
        selectedSlot = slot
        isSimulating = true
        val capturedItem = itemId
        val capturedEnchantability = enchantability
        val capturedBookshelves = bookshelves
        val capturedMode = mode
        pendingJob = scope.launch {
            val option = withContext(Dispatchers.Default) {
                MojangEnchantmentSimulator.runOnce(capturedItem, capturedEnchantability, capturedBookshelves, slot, capturedMode)
            }
            val computedStats = withContext(Dispatchers.Default) {
                MojangEnchantmentSimulator.monteCarlo(capturedItem, capturedEnchantability, capturedBookshelves, slot, capturedMode)
            }
            currentOption = option
            stats = computedStats
            isSimulating = false
        }
    }
}

@Composable
fun rememberEnchantmentSimulationState(): EnchantmentSimulationState {
    val scope = rememberCoroutineScope()
    return remember { EnchantmentSimulationState(scope) }
}
