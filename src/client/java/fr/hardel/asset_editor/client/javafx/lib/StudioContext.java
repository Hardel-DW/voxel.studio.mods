package fr.hardel.asset_editor.client.javafx.lib;

import fr.hardel.asset_editor.client.javafx.lib.action.EditorActionGateway;
import fr.hardel.asset_editor.client.javafx.lib.action.EnchantmentActions;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.lib.store.RegistryElementStore;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.routes.StudioRouter;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioPackState;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioTabsState;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioUiState;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.network.PermissionRequestPayload;
import fr.hardel.asset_editor.permission.StudioPermissions;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.LevelResource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class StudioContext {

    private final StudioRouter router = new StudioRouter();
    private final StudioUiState uiState = new StudioUiState();
    private final StudioTabsState tabsState = new StudioTabsState();
    private final StudioPackState packState = new StudioPackState();
    private final RegistryElementStore elementStore = new RegistryElementStore();
    private final ObjectProperty<StudioPermissions> permissionsProperty =
            new SimpleObjectProperty<>(StudioPermissions.NONE);
    private final EditorActionGateway gateway = new EditorActionGateway(packState, elementStore, this::permissions);
    private String worldSessionKey = "";

    public StudioContext() {
        router.setPermissionSupplier(this::permissions);
    }

    public StudioRouter router() {
        return router;
    }

    public StudioUiState uiState() {
        return uiState;
    }

    public StudioTabsState tabsState() {
        return tabsState;
    }

    public StudioPackState packState() {
        return packState;
    }

    public RegistryElementStore elementStore() {
        return elementStore;
    }

    public EditorActionGateway gateway() {
        return gateway;
    }

    public StudioPermissions permissions() {
        return permissionsProperty.get();
    }

    public ObjectProperty<StudioPermissions> permissionsProperty() {
        return permissionsProperty;
    }

    public void setPermissions(StudioPermissions permissions) {
        permissionsProperty.set(permissions);
        enforcePermissionRoute();
    }

    private void enforcePermissionRoute() {
        if (router.currentRoute() == StudioRoute.NO_PERMISSION) {
            StudioConcept.firstAccessible(permissions()).ifPresent(
                    concept -> router.navigate(concept.overviewRoute()));
            return;
        }
        router.revalidate();
    }

    public <T> Collection<ElementEntry<?>> allEntries(ResourceKey<Registry<T>> registryKey) {
        return elementStore.allElements(registryKey);
    }

    public <T> List<ElementEntry<T>> allTypedEntries(ResourceKey<Registry<T>> registryKey) {
        return elementStore.allTypedElements(registryKey);
    }

    public <T> ElementEntry<T> currentEntry(ResourceKey<Registry<T>> registryKey) {
        String id = tabsState.currentElementId();
        if (id == null || id.isBlank()) return null;
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) return null;
        return elementStore.get(registryKey, identifier);
    }

    public <T> List<Holder.Reference<T>> registryElements(ResourceKey<Registry<T>> registryKey) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return List.of();
        return conn.registryAccess()
                .lookup(registryKey)
                .map(reg -> reg.listElements().toList())
                .orElse(List.of());
    }

    public <T> Optional<HolderSet<T>> resolveTag(ResourceKey<Registry<T>> registryKey, TagKey<T> tagKey) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return Optional.empty();
        return conn.registryAccess().lookup(registryKey)
                .flatMap(reg -> reg.get(tagKey))
                .<HolderSet<T>>map(named -> named);
    }

    public <T> Optional<Holder.Reference<T>> resolveHolder(ResourceKey<Registry<T>> registryKey, Identifier id) {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return Optional.empty();
        return conn.registryAccess().lookup(registryKey).flatMap(reg -> reg.get(ResourceKey.create(registryKey, id)));
    }

    public <T> Holder.Reference<T> findElement(ResourceKey<Registry<T>> registryKey) {
        String id = tabsState.currentElementId();
        if (id == null || id.isBlank()) return null;
        for (var h : registryElements(registryKey)) {
            if (h.key().identifier().toString().equals(id)) return h;
        }
        return null;
    }

    public void resyncWorldSession(boolean force) {
        String nextKey = computeWorldSessionKey();
        if (worldSessionKey.equals(nextKey)) {
            if (force) {
                packState.refreshFromServer();
            }
            return;
        }

        worldSessionKey = nextKey;
        elementStore.clearAll();
        tabsState.reset();
        packState.refreshFromServer();
        snapshotRegistries();
        requestPermissions();
    }

    private void requestPermissions() {
        Minecraft.getInstance().execute(() ->
                ClientPlayNetworking.send(new PermissionRequestPayload()));
    }

    public void resetForWorldClose() {
        worldSessionKey = "";
        permissionsProperty.set(StudioPermissions.NONE);
        elementStore.clearAll();
        tabsState.reset();
        packState.clearSelection();
        packState.availablePacks().clear();
        router.navigate(StudioRoute.NO_PERMISSION);
    }

    private void snapshotRegistries() {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return;
        conn.registryAccess().lookup(Registries.ENCHANTMENT).ifPresent(reg ->
                elementStore.snapshotFromRegistry(Registries.ENCHANTMENT, reg,
                        entry -> EnchantmentActions.initializeCustom(entry.data(), entry.tags())));
    }

    private static String computeWorldSessionKey() {
        var mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (server != null) {
            return "sp:" + server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        }

        var conn = mc.getConnection();
        if (conn == null) {
            return "";
        }

        ServerData data = mc.getCurrentServer();
        if (data != null && !data.ip.isBlank()) {
            return "mp:" + data.ip;
        }

        return "mp:" + conn.getConnection().getRemoteAddress();
    }
}
