package fr.hardel.asset_editor.client.compose.components.page.structure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import fr.hardel.asset_editor.network.structure.StructureAssemblyParameters
import net.minecraft.resources.Identifier
import kotlin.random.Random

class StructureVariantState {
    var requestedParameters: StructureAssemblyParameters? by mutableStateOf(null)
        private set

    var counter: Int by mutableStateOf(0)
        private set

    fun reroll(currentChunkX: Int, currentChunkZ: Int) {
        counter++
        requestedParameters = StructureAssemblyParameters(Random.nextLong(), currentChunkX, currentChunkZ)
    }

    fun apply(seed: Long, chunkX: Int, chunkZ: Int) {
        counter++
        requestedParameters = StructureAssemblyParameters(seed, chunkX, chunkZ)
    }

    fun reset() {
        counter = 0
        requestedParameters = null
    }
}

@Composable
fun rememberStructureVariantState(id: Identifier): StructureVariantState =
    remember(id) { StructureVariantState() }
