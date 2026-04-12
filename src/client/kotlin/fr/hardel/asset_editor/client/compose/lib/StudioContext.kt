package fr.hardel.asset_editor.client.compose.lib

import fr.hardel.asset_editor.client.ClientSessionDispatch
import fr.hardel.asset_editor.client.compose.lib.action.EditorActionGateway
import fr.hardel.asset_editor.client.compose.lib.assets.DefaultStudioAssetCache
import fr.hardel.asset_editor.client.compose.lib.assets.DefaultStudioPrefetcher
import fr.hardel.asset_editor.client.compose.lib.assets.StudioAssetCache
import fr.hardel.asset_editor.client.compose.lib.assets.StudioPrefetcher
import fr.hardel.asset_editor.client.debug.ClientDebugTelemetry
import fr.hardel.asset_editor.client.memory.core.Subscription
import fr.hardel.asset_editor.client.memory.session.debug.DebugMemory
import fr.hardel.asset_editor.client.memory.session.ui.NavigationMemory

import fr.hardel.asset_editor.client.memory.session.SessionMemory
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistries
import fr.hardel.asset_editor.client.memory.core.ClientWorkspaceRegistry
import fr.hardel.asset_editor.client.memory.session.ui.UiMemory
import fr.hardel.asset_editor.client.ClientPreferences
import fr.hardel.asset_editor.client.memory.persistent.IssueMemory
import fr.hardel.asset_editor.client.memory.session.ui.PackSelectionMemory
import fr.hardel.asset_editor.client.memory.session.server.RegistryMemory
import fr.hardel.asset_editor.permission.StudioPermissions
import fr.hardel.asset_editor.data.concept.StudioRegistryResolver
import java.util.Optional
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.Registry
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagKey

class StudioContext(
    private val sessionMemory: SessionMemory,
    private val debugMemory: DebugMemory,
    private val dispatch: ClientSessionDispatch
) {

    private val packSelection =
        PackSelectionMemory(sessionMemory, ClientPreferences::lastPackId, ClientPreferences::setLastPackId)
    private val issues = IssueMemory()
    private val registries = RegistryMemory()
    private val subscriptions = mutableListOf<Subscription>()
    private val navigationMemory = NavigationMemory(sessionMemory::permissions)
    private val uiMemory = UiMemory()
    private val assetCache = DefaultStudioAssetCache()
    private val prefetcher = DefaultStudioPrefetcher(assetCache)
    private var lastWorldSessionKey = sessionMemory.worldSessionKey()

    val gateway = EditorActionGateway(sessionMemory, packSelection, registries, issues)

    init {
        snapshotRegistries()

        var lastPermissions = sessionMemory.permissions()
        fun handlePermissions(permissions: StudioPermissions) {
            navigationMemory.revalidate(permissions)
            if (navigationMemory.snapshot().current is NoPermissionDestination) {
                if (!permissions.isNone) {
                    StudioUiRegistry.firstSupportedConceptId()?.let { conceptId ->
                        navigationMemory.navigate(ConceptOverviewDestination(conceptId))
                    }
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

        var lastSelectedPackId = packSelection.selectedPack()?.packId()
        subscriptions += packSelection.subscribe {
            val selectedPackId = packSelection.selectedPack()?.packId()
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

    fun packSelectionMemory(): PackSelectionMemory = packSelection

    fun issueMemory(): IssueMemory = issues

    fun registryMemory(): RegistryMemory = registries

    fun navigationMemory(): NavigationMemory = navigationMemory

    fun uiMemory(): UiMemory = uiMemory

    fun debugMemory(): DebugMemory = debugMemory

    fun assetCache(): StudioAssetCache = assetCache

    fun prefetcher(): StudioPrefetcher = prefetcher

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

    fun registryAccess() = Minecraft.getInstance().connection?.registryAccess()

    fun requireRegistryAccess() = registryAccess() ?: error("Missing client registry access")

    fun studioConceptId(registryKey: ResourceKey<out Registry<*>>) =
        registryAccess()?.let { StudioRegistryResolver.conceptId(it, registryKey) }

    fun studioDefaultEditorTab(conceptId: Identifier): Identifier =
        StudioRegistryResolver.defaultEditorTab(requireRegistryAccess(), conceptId)

    fun studioEditorTabs(conceptId: Identifier): List<Identifier> =
        StudioRegistryResolver.editorTabs(requireRegistryAccess(), conceptId)

    fun studioRegistryKey(conceptId: Identifier): ResourceKey<out Registry<*>> =
        StudioRegistryResolver.registryKey(requireRegistryAccess(), conceptId)

    fun studioRegistryPath(conceptId: Identifier): String =
        StudioRegistryResolver.registryPath(requireRegistryAccess(), conceptId)

    fun studioTitleKey(conceptId: Identifier): String =
        StudioRegistryResolver.titleKey(requireRegistryAccess(), conceptId)

    fun studioTabTitleKey(conceptId: Identifier, tabId: Identifier): String =
        StudioRegistryResolver.tabTitleKey(requireRegistryAccess(), conceptId, tabId)

    fun studioIcon(conceptId: Identifier): Identifier =
        StudioRegistryResolver.icon(requireRegistryAccess(), conceptId)

    fun resyncWorldSession() {
        val nextKey = sessionMemory.worldSessionKey()
        if (lastWorldSessionKey == nextKey) {
            return
        }

        ClientDebugTelemetry.lifecycle(
            I18n.get("debug:telemetry.world_session_changed"),
            mapOf(
                "previousWorldSessionKey" to lastWorldSessionKey,
                "nextWorldSessionKey" to nextKey
            )
        )

        lastWorldSessionKey = nextKey
        registries.clearAll()
        packSelection.clearSelection()
        issues.clear()
        gateway.clearPendingActions()
        snapshotRegistries()
    }

    fun resetForWorldClose() {
        lastWorldSessionKey = ""
        registries.clearAll()
        packSelection.clearSelection()
        issues.clear()
        gateway.clearPendingActions()
        uiMemory.reset()
        navigationMemory.reset()
        assetCache.invalidateAll()
        dispatch.clearGateway(gateway)
    }

    fun dispose() {
        subscriptions.forEach(Subscription::unsubscribe)
        assetCache.invalidateAll()
        dispatch.clearGateway(gateway)
        packSelection.dispose()
    }

    private fun snapshotRegistries() {
        val connection = Minecraft.getInstance().connection ?: return
        for (workspace in ClientWorkspaceRegistries.all()) {
            snapshotWorkspace(connection.registryAccess(), workspace)
        }
    }

    private fun refreshSelectedPackWorkspace() {
        gateway.clearPendingActions()
        issues.clear()
        val pack = packSelection.selectedPack() ?: return

        for (workspace in ClientWorkspaceRegistries.all()) {
            requestWorkspaceRefresh(pack.packId(), workspace)
        }
    }

    private fun requestWorkspaceRefresh(
        packId: String,
        workspace: ClientWorkspaceRegistry<*>
    ) {
        ClientDebugTelemetry.sync(
            I18n.get("debug:telemetry.requested_workspace_refresh"),
            mapOf(
                "packId" to packId,
                "registry" to workspace.registryId().toString()
            )
        )

        gateway.requestPackWorkspace(workspace)
    }

    private fun snapshotWorkspace(
        registryAccess: net.minecraft.core.RegistryAccess,
        workspace: ClientWorkspaceRegistry<*>
    ) {
        snapshotWorkspaceTyped(registryAccess, workspace)
    }

    private fun <T : Any> snapshotWorkspaceTyped(
        registryAccess: net.minecraft.core.RegistryAccess,
        workspace: ClientWorkspaceRegistry<T>
    ) {
        registryAccess.lookup(workspace.registryKey()).ifPresent { registry ->
            registries.snapshotFromRegistry(workspace, registry)
        }
    }
}
