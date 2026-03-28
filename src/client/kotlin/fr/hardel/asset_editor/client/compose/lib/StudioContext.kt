package fr.hardel.asset_editor.client.compose.lib

import fr.hardel.asset_editor.client.ClientSessionDispatch
import fr.hardel.asset_editor.client.compose.lib.action.EditorActionGateway
import fr.hardel.asset_editor.client.compose.lib.assets.DefaultStudioAssetCache
import fr.hardel.asset_editor.client.compose.lib.assets.DefaultStudioPrefetcher
import fr.hardel.asset_editor.client.compose.lib.assets.StudioAssetCache
import fr.hardel.asset_editor.client.compose.lib.assets.StudioPrefetcher
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.debug.ClientDebugTelemetry
import fr.hardel.asset_editor.client.memory.core.Subscription
import fr.hardel.asset_editor.client.memory.debug.DebugMemory
import fr.hardel.asset_editor.client.memory.navigation.NavigationMemory
import fr.hardel.asset_editor.client.AssetEditorClient
import fr.hardel.asset_editor.client.memory.session.CatalogMemory
import fr.hardel.asset_editor.client.memory.session.SessionMemory
import fr.hardel.asset_editor.client.memory.ui.UiMemory
import fr.hardel.asset_editor.client.memory.workspace.RegistryMemory
import fr.hardel.asset_editor.client.memory.workspace.WorkspaceMemory
import fr.hardel.asset_editor.client.navigation.NoPermissionDestination
import fr.hardel.asset_editor.store.ElementEntry
import fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBindings
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
    private val sessionMemory: SessionMemory,
    private val debugMemory: DebugMemory,
    private val dispatch: ClientSessionDispatch
) {

    private val workspaceMemory = WorkspaceMemory(sessionMemory)
    private val subscriptions = mutableListOf<Subscription>()
    private val navigationMemory = NavigationMemory(sessionMemory::permissions)
    private val uiMemory = UiMemory()
    private val assetCache = DefaultStudioAssetCache()
    private val prefetcher = DefaultStudioPrefetcher(assetCache)

    val gateway = EditorActionGateway(sessionMemory, workspaceMemory)

    init {
        workspaceMemory.setWorldSessionKey(sessionMemory.worldSessionKey())
        snapshotRegistries()

        var lastPermissions = sessionMemory.permissions()
        fun handlePermissions(permissions: fr.hardel.asset_editor.permission.StudioPermissions) {
            navigationMemory.revalidate(permissions)
            if (navigationMemory.snapshot().current is NoPermissionDestination) {
                StudioConcept.firstAccessible(permissions)?.let { concept ->
                    navigationMemory.navigate(concept.overview())
                }
            }
        }
        subscriptions += sessionMemory.subscribe {
            val permissions = sessionMemory.permissions()
            if (permissions == lastPermissions) {
                return@subscribe
            }
            lastPermissions = permissions
            handlePermissions(permissions)
        }
        handlePermissions(lastPermissions)

        var lastSelectedPackId = workspaceMemory.packSelection().selectedPack()?.packId()
        subscriptions += workspaceMemory.packSelection().subscribe {
            val selectedPackId = workspaceMemory.packSelection().selectedPack()?.packId()
            if (selectedPackId == lastSelectedPackId) {
                return@subscribe
            }
            lastSelectedPackId = selectedPackId
            refreshSelectedPackWorkspace()
        }
        refreshSelectedPackWorkspace()

        dispatch.setGateway(gateway)
    }

    fun sessionMemory(): SessionMemory = sessionMemory

    fun workspaceMemory(): WorkspaceMemory = workspaceMemory

    fun packSelectionMemory() = workspaceMemory.packSelection()

    fun registryMemory(): RegistryMemory = workspaceMemory.registries()

    fun navigationMemory(): NavigationMemory = navigationMemory

    fun uiMemory(): UiMemory = uiMemory

    fun debugMemory(): DebugMemory = debugMemory

    fun assetCache(): StudioAssetCache = assetCache

    fun prefetcher(): StudioPrefetcher = prefetcher

    fun catalogMemory(): CatalogMemory = AssetEditorClient.catalogMemory()

    fun <T : Any> allTypedEntries(registryKey: ResourceKey<Registry<T>>): List<ElementEntry<T>> =
        workspaceMemory.registries().allTypedElements(registryKey)

    fun <T : Any> entryById(
        registryKey: ResourceKey<Registry<T>>,
        elementId: String?
    ): ElementEntry<T>? {
        if (elementId.isNullOrBlank()) {
            return null
        }
        val identifier = Identifier.tryParse(elementId) ?: return null
        return workspaceMemory.registries().get(registryKey, identifier)
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
        val nextKey = sessionMemory.worldSessionKey()
        if (workspaceMemory.worldSessionKey() == nextKey) {
            return
        }

        ClientDebugTelemetry.lifecycle(
            I18n.get("debug:telemetry.world_session_changed"),
            mapOf(
                "previousWorldSessionKey" to workspaceMemory.worldSessionKey(),
                "nextWorldSessionKey" to nextKey
            )
        )

        workspaceMemory.setWorldSessionKey(nextKey)
        workspaceMemory.resetForWorldSync()
        snapshotRegistries()
    }

    fun resetForWorldClose() {
        workspaceMemory.resetForWorldClose()
        uiMemory.reset()
        navigationMemory.reset()
        assetCache.invalidateAll()
        dispatch.clearGateway(gateway)
    }

    fun dispose() {
        subscriptions.forEach(Subscription::unsubscribe)
        assetCache.invalidateAll()
        dispatch.clearGateway(gateway)
        workspaceMemory.dispose()
    }

    @Suppress("UNCHECKED_CAST")
    private fun snapshotRegistries() {
        val connection = Minecraft.getInstance().connection ?: return
        for (binding in RegistryWorkspaceBindings.all()) {
            snapshotClientBinding(connection, binding as fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBinding<Any>)
        }
    }

    private fun refreshSelectedPackWorkspace() {
        workspaceMemory.clearPendingActions()
        workspaceMemory.issues().clear()
        val pack = workspaceMemory.packSelection().selectedPack() ?: return

        for (binding in RegistryWorkspaceBindings.all()) {
            requestWorkspaceRefresh(pack.packId(), binding.registryKey())
        }
    }

    private fun <T : Any> snapshotClientBinding(
        connection: net.minecraft.client.multiplayer.ClientPacketListener,
        binding: fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBinding<T>
    ) {
        connection.registryAccess().lookup(binding.registryKey()).ifPresent { registry ->
            workspaceMemory.registries().snapshotFromRegistry(
                binding.registryKey(),
                registry
            ) { entry: ElementEntry<T> -> binding.initializeEntry(entry).custom() }
        }
    }

    private fun requestWorkspaceRefresh(
        packId: String,
        registry: ResourceKey<out Registry<*>>
    ) {
        ClientDebugTelemetry.sync(
            I18n.get("debug:telemetry.requested_workspace_refresh"),
            mapOf(
                "packId" to packId,
                "registry" to registry.identifier().toString()
            )
        )

        gateway.requestPackWorkspace(registry)
    }
}
