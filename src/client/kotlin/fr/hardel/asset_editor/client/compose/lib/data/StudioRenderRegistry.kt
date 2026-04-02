package fr.hardel.asset_editor.client.compose.lib.data

import androidx.compose.runtime.Composable
import fr.hardel.asset_editor.client.compose.lib.StudioContext
import java.util.LinkedHashMap
import java.util.ServiceLoader

object StudioRenderRegistry {
    private val bindings: Map<String, StudioConceptRenderer> by lazy {
        val loaded = LinkedHashMap<String, StudioConceptRenderer>()
        ServiceLoader.load(StudioConceptRenderer::class.java).forEach { binding ->
            val key = binding.conceptId.toString()
            require(loaded.putIfAbsent(key, binding) == null) {
                "Duplicate studio concept renderer registration: $key"
            }
        }
        loaded
    }

    fun hasLayout(concept: StudioConcept): Boolean =
        bindings.containsKey(concept.id.toString())

    fun supportsSimulation(concept: StudioConcept): Boolean =
        bindings[concept.id.toString()]?.supportsSimulation == true

    fun shouldPrefetchAtlas(concept: StudioConcept): Boolean =
        bindings[concept.id.toString()]?.shouldPrefetchAtlas == true

    fun supportedConcepts(): List<StudioConcept> =
        StudioConcepts.entries().filter(::hasLayout)

    fun firstSupportedConcept(): StudioConcept? =
        supportedConcepts().firstOrNull()

    @Composable
    fun RenderConceptLayout(context: StudioContext, concept: StudioConcept) {
        bindings[concept.id.toString()]?.Render(context)
    }
}
