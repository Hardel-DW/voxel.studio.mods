package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import fr.hardel.asset_editor.client.network.ClientPayloadSender
import fr.hardel.asset_editor.network.structure.StructureAssemblyRequestPayload
import fr.hardel.asset_editor.network.structure.StructureAssemblyResponsePayload
import fr.hardel.asset_editor.network.structure.StructureAssemblySnapshot
import net.minecraft.resources.Identifier
import java.util.concurrent.ConcurrentHashMap

private const val MAX_CACHE_ENTRIES = 5

object StructureAssemblyMemory {
    private val cache = mutableStateMapOf<Identifier, StructureAssemblySnapshot>()
    private val insertionOrder = ArrayDeque<Identifier>()
    private val orderLock = Any()
    private val pending = ConcurrentHashMap.newKeySet<Identifier>()

    fun get(id: Identifier): StructureAssemblySnapshot? = cache[id]

    fun request(id: Identifier) {
        if (cache.containsKey(id) || !pending.add(id)) return
        ClientPayloadSender.send(StructureAssemblyRequestPayload(id))
    }

    fun invalidate(id: Identifier) {
        cache.remove(id)
        pending.remove(id)
        synchronized(orderLock) { insertionOrder.remove(id) }
    }

    @JvmStatic
    fun invalidateAll() {
        cache.clear()
        pending.clear()
        synchronized(orderLock) { insertionOrder.clear() }
    }

    @JvmStatic
    fun receiveResponse(payload: StructureAssemblyResponsePayload) {
        val id = payload.structureId()
        pending.remove(id)
        payload.snapshot().ifPresent { snapshot ->
            synchronized(orderLock) {
                insertionOrder.remove(id)
                insertionOrder.addLast(id)
                cache[id] = snapshot
                while (insertionOrder.size > MAX_CACHE_ENTRIES) {
                    val evict = insertionOrder.removeFirst()
                    cache.remove(evict)
                }
            }
        }
    }
}

@Composable
fun rememberStructureAssembly(id: Identifier): StructureAssemblySnapshot? {
    LaunchedEffect(id) { StructureAssemblyMemory.request(id) }
    return StructureAssemblyMemory.get(id)
}
