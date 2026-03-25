package fr.hardel.asset_editor.client.compose.lib

import fr.hardel.asset_editor.client.ClientSessionDispatch
import fr.hardel.asset_editor.client.compose.lib.action.EditorActionGateway
import fr.hardel.asset_editor.client.compose.lib.assets.DefaultStudioAssetCache
import fr.hardel.asset_editor.client.compose.lib.assets.DefaultStudioPrefetcher
import fr.hardel.asset_editor.client.compose.lib.assets.StudioAssetCache
import fr.hardel.asset_editor.client.compose.lib.assets.StudioPrefetcher
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.debug.ClientDebugTelemetry
import fr.hardel.asset_editor.client.navigation.NoPermissionDestination
import fr.hardel.asset_editor.client.selector.Subscription
import fr.hardel.asset_editor.client.state.ClientSessionState
import fr.hardel.asset_editor.client.state.ClientWorkspaceState
import fr.hardel.asset_editor.client.state.RegistryElementStore
import fr.hardel.asset_editor.client.state.StudioNavigationState
import fr.hardel.asset_editor.client.state.StudioUiState
import fr.hardel.asset_editor.store.ElementEntry
import fr.hardel.asset_editor.store.EnchantmentFlushAdapter
import java.util.Optional
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagKey

class StudioContext(
    private val sessionState: ClientSessionState,
    private val dispatch: ClientSessionDispatch
) {

    private val workspaceState = ClientWorkspaceState(sessionState)
    private val subscriptions = mutableListOf<Subscription>()
    private val navigationState = StudioNavigationState(sessionState::permissions)
    private val uiState = StudioUiState()
    private val assetCache = DefaultStudioAssetCache()
    private val prefetcher = DefaultStudioPrefetcher(assetCache)

    val gateway = EditorActionGateway(sessionState, workspaceState)

    init {
        workspaceState.setWorldSessionKey(sessionState.worldSessionKey())
        snapshotRegistries()

        subscriptions += sessionState.select({ snapshot -> snapshot.permissions() })
            .subscribe({ permissions ->
                navigationState.revalidate(permissions)
                if (navigationState.snapshot().current is NoPermissionDestination) {
                    StudioConcept.firstAccessible(permissions)?.let { concept ->
                        navigationState.navigate(concept.overview())
                    }
                }
            }, true)

        subscriptions += workspaceState.packSelectionState()
            .select({ snapshot -> snapshot.selectedPack() })
            .subscribe({ refreshSelectedPackWorkspace() }, true)

        dispatch.setGateway(gateway)
    }

    fun sessionState(): ClientSessionState = sessionState

    fun workspaceState(): ClientWorkspaceState = workspaceState

    fun packState() = workspaceState.packSelectionState()

    fun elementStore(): RegistryElementStore = workspaceState.elementStore()

    fun navigationState(): StudioNavigationState = navigationState

    fun uiState(): StudioUiState = uiState

    fun assetCache(): StudioAssetCache = assetCache

    fun prefetcher(): StudioPrefetcher = prefetcher

    fun <T : Any> allTypedEntries(registryKey: ResourceKey<Registry<T>>): List<ElementEntry<T>> =
        workspaceState.elementStore().allTypedElements(registryKey)

    fun <T : Any> entryById(
        registryKey: ResourceKey<Registry<T>>,
        elementId: String?
    ): ElementEntry<T>? {
        if (elementId.isNullOrBlank()) {
            return null
        }
        val identifier = Identifier.tryParse(elementId) ?: return null
        return workspaceState.elementStore().get(registryKey, identifier)
    }

    fun <T : Any> registryElements(registryKey: ResourceKey<Registry<T>>): List<Holder.Reference<T>> {
        val connection = Minecraft.getInstance().connection ?: return emptyList()
        return connection.registryAccess()
            .lookup(registryKey)
            .map { registry -> registry.listElements().toList() }
            .orElse(emptyList())
    }

    fun <T : Any> resolveTag(registryKey: ResourceKey<Registry<T>>, tagKey: TagKey<T>): Optional<HolderSet<T>> {
        val connection = Minecraft.getInstance().connection ?: return Optional.empty()
        return connection.registryAccess()
            .lookup(registryKey)
            .flatMap { registry -> registry.get(tagKey) }
            .map { named -> named as HolderSet<T> }
    }

    fun <T : Any> resolveHolder(registryKey: ResourceKey<Registry<T>>, id: Identifier): Optional<Holder.Reference<T>> {
        val connection = Minecraft.getInstance().connection ?: return Optional.empty()
        return connection.registryAccess()
            .lookup(registryKey)
            .flatMap { registry -> registry.get(ResourceKey.create(registryKey, id)) }
    }

    fun resyncWorldSession() {
        val nextKey = sessionState.worldSessionKey()
        if (workspaceState.worldSessionKey() == nextKey) {
            return
        }

        ClientDebugTelemetry.lifecycle(
            I18n.get("debug:telemetry.world_session_changed"),
            mapOf(
                "previousWorldSessionKey" to workspaceState.worldSessionKey(),
                "nextWorldSessionKey" to nextKey
            )
        )

        workspaceState.setWorldSessionKey(nextKey)
        workspaceState.resetForWorldSync()
        snapshotRegistries()
    }

    fun resetForWorldClose() {
        workspaceState.resetForWorldClose()
        uiState.reset()
        navigationState.reset()
        assetCache.invalidateAll()
        dispatch.clearGateway(gateway)
    }

    fun dispose() {
        subscriptions.forEach(Subscription::unsubscribe)
        assetCache.invalidateAll()
        dispatch.clearGateway(gateway)
        workspaceState.dispose()
    }

    private fun snapshotRegistries() {
        val connection = Minecraft.getInstance().connection ?: return
        connection.registryAccess().lookup(Registries.ENCHANTMENT).ifPresent { registry ->
            workspaceState.elementStore().snapshotFromRegistry(
                Registries.ENCHANTMENT,
                registry
            ) { entry ->
                EnchantmentFlushAdapter.initializeCustom(entry.data(), entry.tags())
            }
        }
    }

    private fun refreshSelectedPackWorkspace() {
        workspaceState.clearPendingActions()
        workspaceState.issueState().clear()
        val pack = workspaceState.packSelectionState().selectedPack() ?: return

        ClientDebugTelemetry.sync(
            I18n.get("debug:telemetry.requested_workspace_refresh"),
            mapOf(
                "packId" to pack.packId(),
                "registry" to Registries.ENCHANTMENT.identifier().toString()
            )
        )

        gateway.requestPackWorkspace(Registries.ENCHANTMENT)
    }
}
