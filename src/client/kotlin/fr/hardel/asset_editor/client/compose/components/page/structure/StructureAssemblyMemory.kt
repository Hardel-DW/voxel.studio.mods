package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import fr.hardel.asset_editor.client.network.ClientPayloadSender
import fr.hardel.asset_editor.network.structure.StructureAssemblyParameters
import fr.hardel.asset_editor.network.structure.StructureAssemblyRequestPayload
import fr.hardel.asset_editor.network.structure.StructureAssemblyResponsePayload
import fr.hardel.asset_editor.network.structure.StructureAssemblySnapshot
import net.minecraft.resources.Identifier
import java.util.Optional

private const val MAX_CACHE_ENTRIES = 10

sealed interface StructureAssemblyState {
    object Idle : StructureAssemblyState
    object Loading : StructureAssemblyState
    object Empty : StructureAssemblyState
    data class Ready(val snapshot: StructureAssemblySnapshot) : StructureAssemblyState
}

private data class AssemblyKey(val id: Identifier, val parameters: StructureAssemblyParameters?)

object StructureAssemblyMemory {
    private val cache = mutableStateMapOf<AssemblyKey, StructureAssemblyState>()
    private val insertionOrder = ArrayDeque<AssemblyKey>()
    private val lock = Any()

    fun state(id: Identifier, parameters: StructureAssemblyParameters?): StructureAssemblyState =
        cache[AssemblyKey(id, parameters)] ?: StructureAssemblyState.Idle

    fun request(id: Identifier, parameters: StructureAssemblyParameters?) {
        val key = AssemblyKey(id, parameters)
        synchronized(lock) {
            val current = cache[key]
            if (current != null && current !== StructureAssemblyState.Idle) return
            store(key, StructureAssemblyState.Loading)
        }

        ClientPayloadSender.send(StructureAssemblyRequestPayload(id, Optional.ofNullable(parameters)))
    }

    fun invalidate(id: Identifier) {
        synchronized(lock) {
            val toRemove = cache.keys.filter { it.id == id }
            toRemove.forEach { key ->
                cache.remove(key)
                insertionOrder.remove(key)
            }
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
            val key = AssemblyKey(payload.structureId(), payload.requestedParameters().orElse(null))
            if (cache[key] !== StructureAssemblyState.Loading) return
            val next = payload.snapshot()
                .map<StructureAssemblyState> { StructureAssemblyState.Ready(it) }
                .orElse(StructureAssemblyState.Empty)
            store(key, next)
        }
    }

    private fun store(key: AssemblyKey, state: StructureAssemblyState) {
        insertionOrder.remove(key)
        insertionOrder.addLast(key)
        cache[key] = state
        while (insertionOrder.size > MAX_CACHE_ENTRIES) {
            val evict = insertionOrder.removeFirst()
            cache.remove(evict)
        }
    }
}

@Composable
fun rememberStructureAssemblyState(id: Identifier, parameters: StructureAssemblyParameters?): StructureAssemblyState {
    LaunchedEffect(id, parameters) { StructureAssemblyMemory.request(id, parameters) }
    return StructureAssemblyMemory.state(id, parameters)
}
