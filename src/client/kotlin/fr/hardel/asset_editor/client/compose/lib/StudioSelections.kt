package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.memory.asFlow
import fr.hardel.asset_editor.client.memory.navigation.NavigationMemory
import fr.hardel.asset_editor.client.memory.ui.ConceptUiSnapshot
import fr.hardel.asset_editor.client.memory.ui.UiMemory
import fr.hardel.asset_editor.client.memory.ClientPackInfo
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import fr.hardel.asset_editor.client.navigation.StudioDestination
import fr.hardel.asset_editor.client.navigation.StudioTabEntry
import fr.hardel.asset_editor.permission.StudioPermissions
import fr.hardel.asset_editor.store.ElementEntry
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey

@Composable
fun rememberCurrentDestination(context: StudioContext): StudioDestination {
    val flow = remember(context) { context.navigationMemory().asFlow() }
    val snapshot by flow.collectAsState(context.navigationMemory().snapshot())
    return snapshot.current
}

@Composable
fun rememberOpenTabs(context: StudioContext): List<StudioTabEntry> {
    val flow = remember(context) { context.navigationMemory().asFlow() }
    val snapshot by flow.collectAsState(context.navigationMemory().snapshot())
    return snapshot.tabs
}

@Composable
fun rememberActiveTabId(context: StudioContext): String? {
    val flow = remember(context) { context.navigationMemory().asFlow() }
    val snapshot by flow.collectAsState(context.navigationMemory().snapshot())
    return snapshot.activeTabId
}

@Composable
fun rememberActiveTabEntry(context: StudioContext): StudioTabEntry? {
    val flow = remember(context) { context.navigationMemory().asFlow() }
    val snapshot by flow.collectAsState(context.navigationMemory().snapshot())
    return snapshot.activeTabId?.let { activeId -> snapshot.tabs.firstOrNull { it.tabId == activeId } }
}

@Composable
fun rememberPermissions(context: StudioContext): StudioPermissions {
    val flow = remember(context) { context.sessionMemory().asFlow() }
    val snapshot by flow.collectAsState(context.sessionMemory().snapshot())
    return snapshot.permissions
}

@Composable
fun rememberAvailablePacks(context: StudioContext): List<ClientPackInfo> {
    val flow = remember(context) { context.sessionMemory().asFlow() }
    val snapshot by flow.collectAsState(context.sessionMemory().snapshot())
    return snapshot.availablePacks
}

@Composable
fun rememberSelectedPack(context: StudioContext): ClientPackInfo? {
    val flow = remember(context) { context.packSelectionMemory().asFlow() }
    val snapshot by flow.collectAsState(context.packSelectionMemory().snapshot())
    return snapshot.selectedPack
}

@Composable
fun rememberConceptUi(
    context: StudioContext,
    concept: StudioConcept
): ConceptUiSnapshot {
    val flow = remember(context) { context.uiMemory().asFlow() }
    val snapshot by flow.collectAsState(context.uiMemory().snapshot())
    return snapshot.concepts[concept] ?: context.uiMemory().conceptSnapshot(concept)
}

@Composable
fun rememberCurrentElementDestination(
    context: StudioContext,
    concept: StudioConcept? = null
): ElementEditorDestination? {
    val destination = rememberCurrentDestination(context)
    val editor = destination as? ElementEditorDestination
    return if (editor != null && (concept == null || editor.concept == concept)) editor else null
}

@Composable
fun <T : Any> rememberCurrentEntry(
    context: StudioContext,
    registry: ResourceKey<Registry<T>>,
    destination: ElementEditorDestination?
): ElementEntry<T>? {
    val flow = remember(context) { context.registryMemory().asFlow() }
    val snapshot by flow.collectAsState(context.registryMemory().snapshot())
    return remember(snapshot, registry, destination?.elementId) {
        context.entryById(registry, destination?.elementId)
    }
}

@Composable
fun rememberNavigationSnapshot(context: StudioContext): NavigationMemory.Snapshot {
    val flow = remember(context) { context.navigationMemory().asFlow() }
    val snapshot by flow.collectAsState(context.navigationMemory().snapshot())
    return snapshot
}

@Composable
fun rememberUiSnapshot(context: StudioContext): UiMemory.Snapshot {
    val flow = remember(context) { context.uiMemory().asFlow() }
    val snapshot by flow.collectAsState(context.uiMemory().snapshot())
    return snapshot
}
