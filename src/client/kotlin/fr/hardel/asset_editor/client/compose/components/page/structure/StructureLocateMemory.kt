package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.runtime.mutableStateMapOf
import fr.hardel.asset_editor.client.network.ClientPayloadSender
import fr.hardel.asset_editor.network.structure.StructureAssemblyParameters
import fr.hardel.asset_editor.network.structure.StructureLocateRequestPayload
import fr.hardel.asset_editor.network.structure.StructureLocateResponsePayload
import net.minecraft.resources.Identifier

sealed interface StructureLocateState {
    object Idle : StructureLocateState
    object Searching : StructureLocateState
    data class Found(val parameters: StructureAssemblyParameters) : StructureLocateState
    object NotFound : StructureLocateState
}

object StructureLocateMemory {
    private val cache = mutableStateMapOf<Identifier, StructureLocateState>()
    private val lock = Any()

    fun state(id: Identifier): StructureLocateState = cache[id] ?: StructureLocateState.Idle

    fun request(id: Identifier) {
        synchronized(lock) {
            cache[id] = StructureLocateState.Searching
        }
        ClientPayloadSender.send(StructureLocateRequestPayload(id))
    }

    fun acknowledge(id: Identifier) {
        synchronized(lock) {
            cache.remove(id)
        }
    }

    @JvmStatic
    fun invalidateAll() {
        synchronized(lock) {
            cache.clear()
        }
    }

    @JvmStatic
    fun receiveResponse(payload: StructureLocateResponsePayload) {
        synchronized(lock) {
            val id = payload.structureId()
            if (cache[id] !== StructureLocateState.Searching) return
            cache[id] = payload.parameters()
                .map<StructureLocateState> { StructureLocateState.Found(it) }
                .orElse(StructureLocateState.NotFound)
        }
    }
}
