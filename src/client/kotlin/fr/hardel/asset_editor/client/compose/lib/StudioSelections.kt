package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import fr.hardel.asset_editor.client.memory.session.ui.NavigationMemory
import fr.hardel.asset_editor.client.memory.session.ui.ConceptUiSnapshot
import fr.hardel.asset_editor.client.memory.session.ui.UiMemory
import fr.hardel.asset_editor.client.memory.session.ui.ClientPackInfo
import fr.hardel.asset_editor.permission.StudioPermissions
import fr.hardel.asset_editor.workspace.ElementEntry
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey

@Composable
fun rememberCurrentDestination(context: StudioContext): StudioDestination {
    return rememberMemoryValue(context.navigationMemory(), selector = NavigationMemory.Snapshot::current)
}

@Composable
fun rememberOpenTabs(context: StudioContext): List<StudioTabEntry> {
    return rememberMemoryValue(context.navigationMemory(), selector = NavigationMemory.Snapshot::tabs)
}

@Composable
fun rememberActiveTabId(context: StudioContext): String? {
    return rememberMemoryValue(context.navigationMemory(), selector = NavigationMemory.Snapshot::activeTabId)
}

@Composable
fun rememberPermissions(context: StudioContext): StudioPermissions {
    return rememberMemoryValue(context.sessionMemory(), selector = { it.permissions })
}

@Composable
fun rememberAvailablePacks(context: StudioContext): List<ClientPackInfo> {
    return rememberMemoryValue(context.sessionMemory(), selector = { it.availablePacks })
}

@Composable
fun rememberSelectedPack(context: StudioContext): ClientPackInfo? {
    return rememberMemoryValue(context.packSelectionMemory(), selector = { it.selectedPack })
}

@Composable
fun rememberConceptUi(
    context: StudioContext,
    conceptId: Identifier
): ConceptUiSnapshot {
    return rememberMemoryValue(context.uiMemory(), conceptId) { snapshot ->
        snapshot.concepts[conceptId] ?: context.uiMemory().conceptSnapshot(conceptId)
    }
}

@Composable
fun rememberCurrentElementDestination(
    context: StudioContext,
    conceptId: Identifier? = null
): ElementEditorDestination? {
    val destination = rememberCurrentDestination(context)
    val editor = destination as? ElementEditorDestination
    return if (editor != null && (conceptId == null || editor.conceptId == conceptId)) editor else null
}

@Composable
fun <T : Any> rememberCurrentEntry(
    context: StudioContext,
    registry: ResourceKey<Registry<T>>,
    destination: ElementEditorDestination?
): ElementEntry<T>? {
    val registryMemory = remember(context, registry) { context.registryMemory().observeTypedRegistry(registry) }
    return rememberMemoryValue(registryMemory, registry, destination?.elementId) { entries ->
        val identifier = destination?.elementId?.let(Identifier::tryParse) ?: return@rememberMemoryValue null
        entries[identifier]
    }
}

@Composable
fun rememberNavigationSnapshot(context: StudioContext): NavigationMemory.Snapshot {
    return rememberMemoryValue(context.navigationMemory()) { it }
}

@Composable
fun rememberUiSnapshot(context: StudioContext): UiMemory.Snapshot {
    return rememberMemoryValue(context.uiMemory()) { it }
}
