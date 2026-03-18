package fr.hardel.asset_editor.client.javafx.lib;

import fr.hardel.asset_editor.client.ClientSessionDispatch;
import fr.hardel.asset_editor.client.javafx.lib.action.EditorActionGateway;
import fr.hardel.asset_editor.client.javafx.lib.action.EnchantmentActions;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.routes.StudioRouter;
import fr.hardel.asset_editor.client.state.ClientSessionState;
import fr.hardel.asset_editor.client.state.ClientWorkspaceState;
import fr.hardel.asset_editor.client.state.WorkspacePackSelectionState;
import fr.hardel.asset_editor.client.state.WorkspaceTabsState;
import fr.hardel.asset_editor.client.state.WorkspaceUiState;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.permission.StudioPermissions;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class StudioContext {

    private final ClientSessionState sessionState;
    private final ClientWorkspaceState workspaceState;
    private final ClientSessionDispatch dispatch;
    private final StudioRouter router = new StudioRouter();
    private final EditorActionGateway gateway;
    private final ChangeListener<StudioPermissions> permissionListener;

    public StudioContext(ClientSessionState sessionState, ClientSessionDispatch dispatch) {
        this.sessionState = sessionState;
        this.workspaceState = new ClientWorkspaceState(sessionState);
        this.dispatch = dispatch;
        this.gateway = new EditorActionGateway(sessionState, workspaceState);
        this.permissionListener = (obs, oldValue, newValue) -> enforcePermissionRoute();

        router.setPermissionSupplier(sessionState::permissions);
        sessionState.permissionsProperty().addListener(permissionListener);
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

    public ReadOnlyObjectProperty<StudioPermissions> permissionsProperty() {
        return sessionState.permissionsProperty();
    }

    private void enforcePermissionRoute() {
        if (router.currentRoute() == StudioRoute.NO_PERMISSION) {
            StudioConcept.firstAccessible(permissions()).ifPresent(concept -> router.navigate(concept.overviewRoute()));
            return;
        }
        router.revalidate();
    }

    public <T> Collection<ElementEntry<?>> allEntries(ResourceKey<Registry<T>> registryKey) {
        return workspaceState.elementStore().allElements(registryKey);
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

    public <T> Holder.Reference<T> findElement(ResourceKey<Registry<T>> registryKey) {
        String id = workspaceState.tabsState().currentElementId();
        if (id == null || id.isBlank())
            return null;
        for (Holder.Reference<T> holder : registryElements(registryKey)) {
            if (holder.key().identifier().toString().equals(id))
                return holder;
        }
        return null;
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
                entry -> EnchantmentActions.initializeCustom(entry.data(), entry.tags())));
    }

    public void dispose() {
        sessionState.permissionsProperty().removeListener(permissionListener);
        dispatch.clearGateway(gateway);
        workspaceState.dispose();
    }
}
