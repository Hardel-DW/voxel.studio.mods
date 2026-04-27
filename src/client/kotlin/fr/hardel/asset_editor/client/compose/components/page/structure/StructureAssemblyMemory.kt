package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import fr.hardel.asset_editor.client.network.ClientPayloadSender
import fr.hardel.asset_editor.network.structure.StructureAssemblyRequestPayload
import fr.hardel.asset_editor.network.structure.StructureAssemblyResponsePayload
import fr.hardel.asset_editor.network.structure.StructureAssemblySnapshot
import net.minecraft.resources.Identifier

private const val MAX_CACHE_ENTRIES = 5

sealed interface StructureAssemblyState {
    object Idle : StructureAssemblyState
    object Loading : StructureAssemblyState
    object Empty : StructureAssemblyState
    data class Ready(val snapshot: StructureAssemblySnapshot) : StructureAssemblyState
}

object StructureAssemblyMemory {
    private val cache = mutableStateMapOf<Identifier, StructureAssemblyState>()
    private val insertionOrder = ArrayDeque<Identifier>()
    private val lock = Any()

    fun state(id: Identifier): StructureAssemblyState = cache[id] ?: StructureAssemblyState.Idle

    fun request(id: Identifier) {
        synchronized(lock) {
            val current = cache[id]
            if (current != null && current !== StructureAssemblyState.Idle) return
            store(id, StructureAssemblyState.Loading)
        }
        
        ClientPayloadSender.send(StructureAssemblyRequestPayload(id))
    }

    fun invalidate(id: Identifier) {
        synchronized(lock) {
            cache.remove(id)
            insertionOrder.remove(id)
        }
    }

    @JvmStatic
    fun invalidateAll() {
        synchronized(lock) {
            cache.clear()
            insertionOrder.clear()
        }
    }

    @JvmStatic
    fun receiveResponse(payload: StructureAssemblyResponsePayload) {
        synchronized(lock) {
            val id = payload.structureId()
            if (cache[id] !== StructureAssemblyState.Loading) return
            val next = payload.snapshot()
                .map<StructureAssemblyState> { StructureAssemblyState.Ready(it) }
                .orElse(StructureAssemblyState.Empty)
            store(id, next)
        }
    }

    private fun store(id: Identifier, state: StructureAssemblyState) {
        insertionOrder.remove(id)
        insertionOrder.addLast(id)
        cache[id] = state
        while (insertionOrder.size > MAX_CACHE_ENTRIES) {
            val evict = insertionOrder.removeFirst()
            cache.remove(evict)
        }
    }
}

@Composable
fun rememberStructureAssemblyState(id: Identifier): StructureAssemblyState {
    LaunchedEffect(id) { StructureAssemblyMemory.request(id) }
    return StructureAssemblyMemory.state(id)
}
