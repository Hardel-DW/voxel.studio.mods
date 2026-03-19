package fr.hardel.asset_editor.client.javafx.lib;

import fr.hardel.asset_editor.client.ClientSessionDispatch;
import fr.hardel.asset_editor.client.javafx.lib.action.EditorActionGateway;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.store.EnchantmentFlushAdapter;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioViewMode;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.routes.StudioRouter;
import fr.hardel.asset_editor.client.selector.SelectorEquality;
import fr.hardel.asset_editor.client.selector.StoreSelection;
import fr.hardel.asset_editor.client.selector.Subscription;
import fr.hardel.asset_editor.client.state.ClientSessionState;
import fr.hardel.asset_editor.client.state.ClientWorkspaceState;
import fr.hardel.asset_editor.client.state.ClientPackInfo;
import fr.hardel.asset_editor.client.state.RegistryElementStore;
import fr.hardel.asset_editor.client.state.StudioOpenTab;
import fr.hardel.asset_editor.client.state.WorkspacePackSelectionState;
import fr.hardel.asset_editor.client.state.WorkspaceTabsState;
import fr.hardel.asset_editor.client.state.WorkspaceUiState;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.permission.StudioPermissions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.List;
import java.util.Optional;

public final class StudioContext {

    private final ClientSessionState sessionState;
    private final ClientWorkspaceState workspaceState;
    private final ClientSessionDispatch dispatch;
    private final StudioRouter router = new StudioRouter();
    private final EditorActionGateway gateway;
    private final Subscription permissionSubscription;
    private final Subscription packSelectionSubscription;

    public StudioContext(ClientSessionState sessionState, ClientSessionDispatch dispatch) {
        this.sessionState = sessionState;
        this.workspaceState = new ClientWorkspaceState(sessionState);
        this.dispatch = dispatch;
        this.gateway = new EditorActionGateway(sessionState, workspaceState);
        this.permissionSubscription = sessionState.select(ClientSessionState.Snapshot::permissions)
            .subscribe(permission -> enforcePermissionRoute(), true);
        this.packSelectionSubscription = workspaceState.packSelectionState()
            .select(WorkspacePackSelectionState.Snapshot::selectedPack)
            .subscribe(pack -> refreshSelectedPackWorkspace(), true);

        router.setPermissionSupplier(sessionState::permissions);
        dispatch.setGateway(gateway);
    }

    public ClientSessionState sessionState() {
        return sessionState;
    }

    public ClientWorkspaceState workspaceState() {
        return workspaceState;
    }

    public StudioRouter router() {
        return router;
    }

    public WorkspaceUiState uiState() {
        return workspaceState.uiState();
    }

    public WorkspaceTabsState tabsState() {
        return workspaceState.tabsState();
    }

    public WorkspacePackSelectionState packState() {
        return workspaceState.packSelectionState();
    }

    public RegistryElementStore elementStore() {
        return workspaceState.elementStore();
    }

    public EditorActionGateway gateway() {
        return gateway;
    }

    public StudioPermissions permissions() {
        return sessionState.permissions();
    }

    public StoreSelection<ClientSessionState.Snapshot, StudioPermissions> selectPermissions() {
        return sessionState.select(ClientSessionState.Snapshot::permissions);
    }

    public StoreSelection<ClientSessionState.Snapshot, List<ClientPackInfo>> selectAvailablePacks() {
        return sessionState.select(ClientSessionState.Snapshot::availablePacks, SelectorEquality.listEquality());
    }

    public StoreSelection<WorkspacePackSelectionState.Snapshot, ClientPackInfo> selectSelectedPack() {
        return workspaceState.packSelectionState().select(WorkspacePackSelectionState.Snapshot::selectedPack);
    }

    public StoreSelection<WorkspaceTabsState.Snapshot, List<StudioOpenTab>> selectOpenTabs() {
        return workspaceState.tabsState().select(WorkspaceTabsState.Snapshot::openTabs, SelectorEquality.listEquality());
    }

    public StoreSelection<WorkspaceTabsState.Snapshot, Integer> selectActiveTabIndex() {
        return workspaceState.tabsState().select(WorkspaceTabsState.Snapshot::activeTabIndex);
    }

    public StoreSelection<WorkspaceTabsState.Snapshot, String> selectCurrentElementId() {
        return workspaceState.tabsState().select(WorkspaceTabsState.Snapshot::currentElementId);
    }

    public StoreSelection<WorkspaceUiState.Snapshot, String> selectFilterPath() {
        return workspaceState.uiState().select(WorkspaceUiState.Snapshot::filterPath);
    }

    public StoreSelection<WorkspaceUiState.Snapshot, StudioViewMode> selectViewMode() {
        return workspaceState.uiState().select(WorkspaceUiState.Snapshot::viewMode);
    }

    private void enforcePermissionRoute() {
        if (router.currentRoute() == StudioRoute.NO_PERMISSION) {
            StudioConcept.firstAccessible(permissions()).ifPresent(concept -> router.navigate(concept.overviewRoute()));
            return;
        }
        router.revalidate();
    }

    public <T> List<ElementEntry<T>> allTypedEntries(ResourceKey<Registry<T>> registryKey) {
        return workspaceState.elementStore().allTypedElements(registryKey);
    }

    public <T> ElementEntry<T> currentEntry(ResourceKey<Registry<T>> registryKey) {
        String id = workspaceState.tabsState().currentElementId();
        if (id == null || id.isBlank())
            return null;
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null)
            return null;
        return workspaceState.elementStore().get(registryKey, identifier);
    }

    public <T> List<Holder.Reference<T>> registryElements(ResourceKey<Registry<T>> registryKey) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null)
            return List.of();
        return conn.registryAccess()
            .lookup(registryKey)
            .map(registry -> registry.listElements().toList())
            .orElse(List.of());
    }

    public <T> Optional<HolderSet<T>> resolveTag(ResourceKey<Registry<T>> registryKey, TagKey<T> tagKey) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null)
            return Optional.empty();
        return conn.registryAccess().lookup(registryKey)
            .flatMap(registry -> registry.get(tagKey))
            .map(named -> (HolderSet<T>) named);
    }

    public <T> Optional<Holder.Reference<T>> resolveHolder(ResourceKey<Registry<T>> registryKey, Identifier id) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null)
            return Optional.empty();
        return conn.registryAccess().lookup(registryKey).flatMap(registry -> registry.get(ResourceKey.create(registryKey, id)));
    }

    public void resyncWorldSession(boolean force) {
        String nextKey = sessionState.worldSessionKey();
        if (workspaceState.worldSessionKey().equals(nextKey)) {
            if (force)
                sessionState.refreshPackList();
            return;
        }

        workspaceState.setWorldSessionKey(nextKey);
        workspaceState.resetForWorldSync();
        sessionState.refreshPackList();
        snapshotRegistries();
    }

    public void resetForWorldClose() {
        workspaceState.resetForWorldClose();
        dispatch.clearGateway(gateway);
        router.navigate(StudioRoute.NO_PERMISSION);
    }

    private void snapshotRegistries() {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null)
            return;
        conn.registryAccess().lookup(Registries.ENCHANTMENT).ifPresent(registry ->
            workspaceState.elementStore().snapshotFromRegistry(
                Registries.ENCHANTMENT,
                registry,
                entry -> EnchantmentFlushAdapter.initializeCustom(entry.data(), entry.tags())));
    }

    public void dispose() {
        permissionSubscription.unsubscribe();
        packSelectionSubscription.unsubscribe();
        dispatch.clearGateway(gateway);
        workspaceState.dispose();
    }

    private void refreshSelectedPackWorkspace() {
        workspaceState.clearPendingActions();
        workspaceState.issueState().clear();
        if (workspaceState.packSelectionState().selectedPack() == null)
            return;
        gateway.requestPackWorkspace(Registries.ENCHANTMENT);
    }
}
