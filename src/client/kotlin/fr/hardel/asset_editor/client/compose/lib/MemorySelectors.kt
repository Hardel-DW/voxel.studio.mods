package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import fr.hardel.asset_editor.client.memory.core.ReadableMemory
import fr.hardel.asset_editor.client.memory.core.ServerDataStore
import net.minecraft.resources.Identifier

@Composable
fun <S, T> rememberMemoryValue(
    memory: ReadableMemory<S>,
    vararg keys: Any?,
    selector: (S) -> T
): T {
    val initial = remember(memory, *keys) { selector(memory.snapshot()) }
    val flow = remember(memory, *keys) { memory.selectAsFlow(selector) }
    val value by flow.collectAsState(initial)
    return value
}

@Composable
fun <T> rememberServerData(slot: ServerDataStore.DataSlot<T>): List<T> {
    remember(slot) { ServerDataStore.requestIfAbsent(slot.key()); true }
    return rememberMemoryValue(slot.memory()) { it }
}

@Composable
fun <T> rememberServerDataItem(
    slot: ServerDataStore.DataSlot<T>,
    id: Identifier,
    idOf: (T) -> Identifier
): T? {
    LaunchedEffect(slot, id) {
        ServerDataStore.requestIfMissing(slot, listOf(id))
    }
    return rememberMemoryValue(slot.memory(), id) { entries ->
        entries.firstOrNull { idOf(it) == id }
    }
}
