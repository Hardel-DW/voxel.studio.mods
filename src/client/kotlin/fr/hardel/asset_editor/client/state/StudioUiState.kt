package fr.hardel.asset_editor.client.state

import androidx.compose.runtime.Immutable
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.data.StudioSidebarView
import fr.hardel.asset_editor.client.compose.lib.data.StudioViewMode
import fr.hardel.asset_editor.client.selector.MutableSelectorStore
import fr.hardel.asset_editor.client.selector.SelectorEquality
import fr.hardel.asset_editor.client.selector.SelectorStore
import fr.hardel.asset_editor.client.selector.StoreSelection
import fr.hardel.asset_editor.client.selector.Subscription
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentSet

@Immutable
data class ConceptUiSnapshot(
    val search: String = "",
    val filterPath: String = "",
    val viewMode: StudioViewMode = StudioViewMode.LIST,
    val sidebarView: StudioSidebarView = StudioSidebarView.SLOTS,
    val expandedTreePaths: ImmutableSet<String> = persistentSetOf()
)

class StudioUiState : SelectorStore<StudioUiState.Snapshot> {

    data class Snapshot(
        val concepts: ImmutableMap<StudioConcept, ConceptUiSnapshot> = persistentHashMapOf()
    )

    private val store = MutableSelectorStore(Snapshot())

    override fun getState(): Snapshot = store.state

    override fun subscribe(listener: Runnable): Subscription =
        store.subscribe(listener)

    fun <R> select(selector: (Snapshot) -> R): StoreSelection<Snapshot, R> =
        store.select(selector)

    fun <R> select(
        selector: (Snapshot) -> R,
        equality: SelectorEquality<in R>
    ): StoreSelection<Snapshot, R> =
        store.select(selector, equality)

    fun snapshot(): Snapshot = store.state

    fun conceptSnapshot(concept: StudioConcept): ConceptUiSnapshot =
        store.state.concepts[concept] ?: defaultSnapshot(concept)

    fun updateSearch(concept: StudioConcept, value: String) {
        updateConcept(concept) { it.copy(search = value) }
    }

    fun updateFilterPath(concept: StudioConcept, value: String) {
        updateConcept(concept) { it.copy(filterPath = value) }
    }

    fun updateViewMode(concept: StudioConcept, value: StudioViewMode) {
        updateConcept(concept) { it.copy(viewMode = value) }
    }

    fun updateSidebarView(concept: StudioConcept, value: StudioSidebarView) {
        updateConcept(concept) {
            it.copy(
                sidebarView = value,
                filterPath = "",
                expandedTreePaths = persistentSetOf()
            )
        }
    }

    fun setTreeExpanded(concept: StudioConcept, path: String, expanded: Boolean) {
        updateConcept(concept) { snapshot ->
            val next = if (expanded) {
                snapshot.expandedTreePaths.toPersistentSet().add(path)
            } else {
                snapshot.expandedTreePaths.toPersistentSet().remove(path)
            }
            snapshot.copy(expandedTreePaths = next)
        }
    }

    fun resetConcept(concept: StudioConcept) {
        store.update { state ->
            if (!state.concepts.containsKey(concept)) {
                return@update state
            }
            state.copy(
                concepts = state.concepts
                    .filterKeys { it != concept }
                    .toImmutableMap()
            )
        }
    }

    fun reset() {
        store.setState(Snapshot())
    }

    private fun updateConcept(
        concept: StudioConcept,
        updater: (ConceptUiSnapshot) -> ConceptUiSnapshot
    ) {
        store.update { state ->
            val current = state.concepts[concept] ?: defaultSnapshot(concept)
            val next = updater(current)
            if (next == current) {
                return@update state
            }
            state.copy(concepts = state.concepts.toPersistentHashMap().put(concept, next))
        }
    }

    private fun defaultSnapshot(concept: StudioConcept): ConceptUiSnapshot =
        when (concept) {
            StudioConcept.ENCHANTMENT -> ConceptUiSnapshot(sidebarView = StudioSidebarView.SLOTS)
            else -> ConceptUiSnapshot()
        }
}
