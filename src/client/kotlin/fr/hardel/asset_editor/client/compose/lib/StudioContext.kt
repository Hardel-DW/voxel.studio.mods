package fr.hardel.asset_editor.client.compose.lib

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import fr.hardel.asset_editor.client.ClientSessionDispatch
import fr.hardel.asset_editor.client.compose.lib.action.EditorActionGateway
import fr.hardel.asset_editor.client.compose.lib.data.StudioConcept
import fr.hardel.asset_editor.client.compose.lib.data.StudioSidebarView
import fr.hardel.asset_editor.client.compose.lib.data.StudioViewMode
import fr.hardel.asset_editor.client.compose.routes.StudioRoute
import fr.hardel.asset_editor.client.compose.routes.StudioRouter
import fr.hardel.asset_editor.client.debug.ClientDebugTelemetry
import fr.hardel.asset_editor.client.selector.Subscription
import fr.hardel.asset_editor.client.state.ClientPackInfo
import fr.hardel.asset_editor.client.state.ClientSessionState
import fr.hardel.asset_editor.client.state.ClientWorkspaceState
import fr.hardel.asset_editor.client.state.RegistryElementStore
import fr.hardel.asset_editor.permission.StudioPermissions
import fr.hardel.asset_editor.store.ElementEntry
import fr.hardel.asset_editor.store.EnchantmentFlushAdapter
import java.util.Optional
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
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

    data class OpenTab(
        val elementId: String,
        val route: StudioRoute
    )

    private val workspaceState = ClientWorkspaceState(sessionState)
    private val subscriptions = mutableListOf<Subscription>()

    val router = StudioRouter()
    val gateway = EditorActionGateway(sessionState, workspaceState)

    var permissions: StudioPermissions by mutableStateOf(sessionState.permissions())
        private set

    var availablePacks: ImmutableList<ClientPackInfo> by mutableStateOf(sessionState.snapshot().availablePacks().toImmutableList())
        private set

    var selectedPack: ClientPackInfo? by mutableStateOf(workspaceState.packSelectionState().selectedPack())
        private set

    var openTabs: ImmutableList<OpenTab> by mutableStateOf(mapOpenTabs(workspaceState.tabsState().openTabsView()))
        private set

    var activeTabIndex: Int by mutableIntStateOf(workspaceState.tabsState().activeTabIndex())
        private set

    var currentElementId: String by mutableStateOf(workspaceState.tabsState().currentElementId())
        private set

    var search: String by mutableStateOf(workspaceState.uiState().search())
        private set

    var filterPath: String by mutableStateOf(workspaceState.uiState().filterPath())
        private set

    var viewMode: StudioViewMode by mutableStateOf(workspaceState.uiState().viewMode())
        private set

    var sidebarView: StudioSidebarView by mutableStateOf(workspaceState.uiState().sidebarView())
        private set

    init {
        subscriptions += sessionState.select({ snapshot -> snapshot.permissions() })
            .subscribe({ nextPermissions ->
                permissions = nextPermissions
                enforcePermissionRoute()
            }, true)

        subscriptions += sessionState.select({ snapshot -> snapshot.availablePacks() })
            .subscribe({ packs ->
                availablePacks = packs.toImmutableList()
            }, true)

        subscriptions += workspaceState.packSelectionState()
            .select({ snapshot -> snapshot.selectedPack() })
            .subscribe({ pack ->
                selectedPack = pack
                refreshSelectedPackWorkspace()
            }, true)

        subscriptions += workspaceState.tabsState().subscribe {
            val snapshot = workspaceState.tabsState().snapshot()
            Snapshot.withMutableSnapshot {
                openTabs = mapOpenTabs(snapshot.openTabs())
                activeTabIndex = snapshot.activeTabIndex()
                currentElementId = snapshot.currentElementId()
            }
        }

        subscriptions += workspaceState.uiState().subscribe {
            val snapshot = workspaceState.uiState().snapshot()
            Snapshot.withMutableSnapshot {
                search = snapshot.search()
                filterPath = snapshot.filterPath()
                viewMode = snapshot.viewMode()
                sidebarView = snapshot.sidebarView()
            }
        }

        router.permissionSupplier = sessionState::permissions
        dispatch.setGateway(gateway)
    }

    fun sessionState(): ClientSessionState = sessionState

    fun workspaceState(): ClientWorkspaceState = workspaceState

    fun uiState() = workspaceState.uiState()

    fun tabsState() = workspaceState.tabsState()

    fun packState() = workspaceState.packSelectionState()

    fun elementStore(): RegistryElementStore = workspaceState.elementStore()

    fun openTab(elementId: String, route: StudioRoute) {
        workspaceState.tabsState().openElement(elementId, route)
    }

    fun updateSidebarView(view: StudioSidebarView) {
        workspaceState.uiState().setSidebarView(view)
    }

    fun updateViewMode(mode: StudioViewMode) {
        workspaceState.uiState().setViewMode(mode)
    }

    fun <T : Any> allTypedEntries(registryKey: ResourceKey<Registry<T>>): List<ElementEntry<T>> =
        workspaceState.elementStore().allTypedElements(registryKey)

    fun <T : Any> currentEntry(registryKey: ResourceKey<Registry<T>>): ElementEntry<T>? {
        val id = workspaceState.tabsState().currentElementId()
        if (id.isBlank()) {
            return null
        }
        val identifier = Identifier.tryParse(id) ?: return null
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
        dispatch.clearGateway(gateway)
        router.navigate(StudioRoute.NoPermission)
    }

    fun dispose() {
        subscriptions.forEach(Subscription::unsubscribe)
        dispatch.clearGateway(gateway)
        workspaceState.dispose()
    }

    private fun enforcePermissionRoute() {
        if (router.currentRoute == StudioRoute.NoPermission) {
            StudioConcept.firstAccessible(permissions)?.let { concept ->
                router.navigate(concept.overviewRoute)
            }
            return
        }
        router.revalidate()
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

    private fun mapOpenTabs(tabs: List<fr.hardel.asset_editor.client.state.StudioOpenTab>): ImmutableList<OpenTab> =
        tabs.map { tab -> OpenTab(tab.elementId(), tab.route()) }.toImmutableList()
}
