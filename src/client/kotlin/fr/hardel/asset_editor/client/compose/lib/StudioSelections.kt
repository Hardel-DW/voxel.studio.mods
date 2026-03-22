package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.navigation.ElementEditorDestination
import fr.hardel.asset_editor.client.navigation.StudioDestination
import fr.hardel.asset_editor.client.navigation.StudioTabEntry
import fr.hardel.asset_editor.client.state.ClientPackInfo
import fr.hardel.asset_editor.client.state.ConceptUiSnapshot
import fr.hardel.asset_editor.client.state.StudioNavigationState
import fr.hardel.asset_editor.client.state.StudioUiState
import fr.hardel.asset_editor.permission.StudioPermissions
import fr.hardel.asset_editor.store.ElementEntry
import kotlinx.collections.immutable.ImmutableList
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey

@Composable
fun rememberCurrentDestination(context: StudioContext): StudioDestination {
    val value by context.navigationState().collectSelectionAsState(selector = { it.current })
    return value
}

@Composable
fun rememberOpenTabs(context: StudioContext): ImmutableList<StudioTabEntry> {
    val value by context.navigationState().collectSelectionAsState(selector = { it.tabs })
    return value
}

@Composable
fun rememberActiveTabId(context: StudioContext): String? {
    val value by context.navigationState().collectSelectionAsState(selector = { it.activeTabId })
    return value
}

@Composable
fun rememberActiveTabEntry(context: StudioContext): StudioTabEntry? {
    val value by context.navigationState().collectSelectionAsState(
        selector = { snapshot ->
            snapshot.activeTabId?.let { activeId -> snapshot.tabs.firstOrNull { it.tabId == activeId } }
        }
    )
    return value
}

@Composable
fun rememberPermissions(context: StudioContext): StudioPermissions {
    var value by remember(context) { mutableStateOf(context.sessionState().permissions()) }
    DisposableEffect(context) {
        val subscription = context.sessionState()
            .select({ snapshot -> snapshot.permissions() })
            .subscribe({ next -> value = next }, true)
        onDispose { subscription.unsubscribe() }
    }
    return value
}

@Composable
fun rememberAvailablePacks(context: StudioContext): List<ClientPackInfo> {
    var value by remember(context) { mutableStateOf(context.sessionState().availablePacks()) }
    DisposableEffect(context) {
        val subscription = context.sessionState()
            .select({ snapshot -> snapshot.availablePacks() })
            .subscribe({ next -> value = next }, true)
        onDispose { subscription.unsubscribe() }
    }
    return value
}

@Composable
fun rememberSelectedPack(context: StudioContext): ClientPackInfo? {
    var value by remember(context) { mutableStateOf(context.packState().selectedPack()) }
    DisposableEffect(context) {
        val subscription = context.packState()
            .select({ snapshot -> snapshot.selectedPack() })
            .subscribe({ next -> value = next }, true)
        onDispose { subscription.unsubscribe() }
    }
    return value
}

@Composable
fun rememberConceptUi(
    context: StudioContext,
    concept: StudioConcept
): ConceptUiSnapshot {
    val uiState = context.uiState()
    val value by uiState.collectSelectionAsState(
        selector = { snapshot -> snapshot.concepts[concept] ?: uiState.conceptSnapshot(concept) }
    )
    return value
}

@Composable
fun rememberCurrentElementDestination(
    context: StudioContext,
    concept: StudioConcept? = null
): ElementEditorDestination? {
    val destination = rememberCurrentDestination(context)
    return remember(destination, concept) {
        val editor = destination as? ElementEditorDestination
        if (editor != null && (concept == null || editor.concept == concept)) editor else null
    }
}

@Composable
fun <T : Any> rememberCurrentEntry(
    context: StudioContext,
    registry: ResourceKey<Registry<T>>,
    destination: ElementEditorDestination?
): ElementEntry<T>? {
    val version = rememberRegistryVersion(context, registry)
    return remember(context, registry, destination?.elementId, version) {
        context.entryById(registry, destination?.elementId)
    }
}

@Composable
fun rememberNavigationSnapshot(context: StudioContext): StudioNavigationState.Snapshot {
    val value by context.navigationState().collectSelectionAsState(selector = { it })
    return value
}

@Composable
fun rememberUiSnapshot(context: StudioContext): StudioUiState.Snapshot {
    val value by context.uiState().collectSelectionAsState(selector = { it })
    return value
}
