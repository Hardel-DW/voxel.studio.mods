package fr.hardel.asset_editor.client.state

import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.navigation.ConceptChangesDestination
import fr.hardel.asset_editor.client.navigation.DebugDestination
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import fr.hardel.asset_editor.client.navigation.NoPermissionDestination
import fr.hardel.asset_editor.client.navigation.StudioDestination
import fr.hardel.asset_editor.client.navigation.StudioTabEntry
import fr.hardel.asset_editor.client.selector.MutableSelectorStore
import fr.hardel.asset_editor.client.selector.SelectorEquality
import fr.hardel.asset_editor.client.selector.SelectorStore
import fr.hardel.asset_editor.client.selector.StoreSelection
import fr.hardel.asset_editor.client.selector.Subscription
import fr.hardel.asset_editor.permission.StudioPermissions
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class StudioNavigationState(
    private val permissionSupplier: () -> StudioPermissions
) : SelectorStore<StudioNavigationState.Snapshot> {

    data class Snapshot(
        val current: StudioDestination = NoPermissionDestination,
        val tabs: ImmutableList<StudioTabEntry> = persistentListOf(),
        val activeTabId: String? = null
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

    fun navigate(destination: StudioDestination) {
        val normalized = normalize(destination)
        store.update { state -> state.copy(current = normalized) }
    }

    fun openElement(destination: ElementEditorDestination) {
        val normalized = normalize(destination)
        if (normalized !is ElementEditorDestination) {
            navigate(normalized)
            return
        }

        val tabId = tabIdOf(normalized)
        store.update { state ->
            val existingIndex = state.tabs.indexOfFirst { it.tabId == tabId }
            val nextTabs = state.tabs.toMutableList()
            if (existingIndex >= 0) {
                nextTabs[existingIndex] = StudioTabEntry(tabId, normalized)
            } else {
                nextTabs += StudioTabEntry(tabId, normalized)
            }
            state.copy(
                current = normalized,
                tabs = nextTabs.toImmutableList(),
                activeTabId = tabId
            )
        }
    }

    fun switchTab(tabId: String) {
        store.update { state ->
            val entry = state.tabs.firstOrNull { it.tabId == tabId } ?: return@update state
            state.copy(
                current = entry.destination,
                activeTabId = entry.tabId
            )
        }
    }

    fun closeTab(tabId: String) {
        store.update { state ->
            val index = state.tabs.indexOfFirst { it.tabId == tabId }
            if (index < 0) {
                return@update state
            }

            val nextTabs = state.tabs.toMutableList().apply { removeAt(index) }
            if (nextTabs.isEmpty()) {
                val fallback = overviewFallback(state.current)
                return@update state.copy(
                    current = normalize(fallback),
                    tabs = persistentListOf(),
                    activeTabId = null
                )
            }

            val nextActiveId = when {
                state.activeTabId != tabId -> state.activeTabId
                index >= nextTabs.size -> nextTabs.last().tabId
                else -> nextTabs[index].tabId
            }
            val nextCurrent = nextTabs.first { it.tabId == nextActiveId }.destination
            state.copy(
                current = nextCurrent,
                tabs = nextTabs.toImmutableList(),
                activeTabId = nextActiveId
            )
        }
    }

    fun replaceCurrentTab(destination: ElementEditorDestination) {
        val normalized = normalize(destination)
        if (normalized !is ElementEditorDestination) {
            navigate(normalized)
            return
        }

        store.update { state ->
            val activeId = state.activeTabId
            if (activeId == null) {
                val tabId = tabIdOf(normalized)
                return@update state.copy(
                    current = normalized,
                    tabs = (state.tabs + StudioTabEntry(tabId, normalized)).toImmutableList(),
                    activeTabId = tabId
                )
            }

            val nextTabs = state.tabs.map { entry ->
                if (entry.tabId == activeId) entry.copy(destination = normalized) else entry
            }.toImmutableList()
            state.copy(
                current = normalized,
                tabs = nextTabs
            )
        }
    }

    fun revalidate(permissions: StudioPermissions = permissionSupplier()) {
        store.update { state ->
            val normalized = normalize(state.current, permissions)
            if (normalized == state.current) {
                state
            } else {
                state.copy(current = normalized)
            }
        }
    }

    fun reset() {
        store.setState(Snapshot())
    }

    fun activeTab(): StudioTabEntry? {
        val snapshot = snapshot()
        val activeId = snapshot.activeTabId ?: return null
        return snapshot.tabs.firstOrNull { it.tabId == activeId }
    }

    private fun normalize(
        destination: StudioDestination,
        permissions: StudioPermissions = permissionSupplier()
    ): StudioDestination {
        if (destination is NoPermissionDestination) {
            return destination
        }
        if (permissions.isNone) {
            return NoPermissionDestination
        }
        if (destination is DebugDestination && !permissions.isAdmin) {
            return NoPermissionDestination
        }
        return destination
    }

    private fun overviewFallback(destination: StudioDestination): StudioDestination =
        when (destination) {
            is ElementEditorDestination -> destination.concept.overview()
            is ConceptChangesDestination -> destination.concept.overview()
            else -> StudioConcept.firstAccessible(permissionSupplier())?.overview() ?: NoPermissionDestination
        }

    private fun tabIdOf(destination: ElementEditorDestination): String =
        "${destination.concept.name}:${destination.elementId}"
}
