package fr.hardel.asset_editor.client.state;

import fr.hardel.asset_editor.client.selector.MutableSelectorStore;
import fr.hardel.asset_editor.client.selector.SelectorEquality;
import fr.hardel.asset_editor.client.selector.StoreSelection;
import fr.hardel.asset_editor.client.selector.Subscription;
import fr.hardel.asset_editor.network.PackCreatePayload;
import fr.hardel.asset_editor.network.PackListRequestPayload;
import fr.hardel.asset_editor.permission.StudioPermissions;
import fr.hardel.asset_editor.store.ServerPackManager.PackEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.function.Function;

public final class ClientSessionState {

    public record Snapshot(StudioPermissions permissions,
        List<ClientPackInfo> availablePacks,
        String worldSessionKey,
        boolean permissionsReceived,
        boolean packListReceived) {

        public Snapshot {
            permissions = permissions == null ? StudioPermissions.NONE : permissions;
            availablePacks = List.copyOf(availablePacks == null ? List.of() : availablePacks);
            worldSessionKey = worldSessionKey == null ? "" : worldSessionKey;
        }

        public static Snapshot empty() {
            return new Snapshot(StudioPermissions.NONE, List.of(), "", false, false);
        }
    }

    private final MutableSelectorStore<Snapshot> store = new MutableSelectorStore<>(Snapshot.empty());
    private final ObservableList<ClientPackInfo> availablePacks = FXCollections.observableArrayList();
    private final Subscription syncSubscription = store.subscribe(this::syncFromStore);

    public ClientSessionState() {
        syncFromStore();
    }

    public Snapshot snapshot() {
        return store.getState();
    }

    public <R> StoreSelection<Snapshot, R> select(Function<? super Snapshot, ? extends R> selector) {
        return store.select(selector);
    }

    public <R> StoreSelection<Snapshot, R> select(Function<? super Snapshot, ? extends R> selector,
        SelectorEquality<? super R> equality) {
        return store.select(selector, equality);
    }

    public StudioPermissions permissions() {
        return snapshot().permissions();
    }

    public ObservableList<ClientPackInfo> availablePacks() {
        return availablePacks;
    }

    public String worldSessionKey() {
        return snapshot().worldSessionKey();
    }

    public boolean hasReceivedPermissions() {
        return snapshot().permissionsReceived();
    }

    public boolean hasReceivedPackList() {
        return snapshot().packListReceived();
    }

    public void setWorldSessionKey(String key) {
        store.update(state -> new Snapshot(
            state.permissions(),
            state.availablePacks(),
            key,
            state.permissionsReceived(),
            state.packListReceived()));
    }

    public void updatePermissions(StudioPermissions nextPermissions) {
        store.update(state -> new Snapshot(
            nextPermissions,
            state.availablePacks(),
            state.worldSessionKey(),
            true,
            state.packListReceived()));
    }

    public void updatePacks(List<PackEntry> entries) {
        List<ClientPackInfo> packs = entries == null ? List.of() : entries.stream().map(ClientPackInfo::from).toList();
        store.update(state -> new Snapshot(
            state.permissions(),
            packs,
            state.worldSessionKey(),
            state.permissionsReceived(),
            true));
    }

    public void refreshPackList() {
        Minecraft.getInstance().execute(() -> ClientPlayNetworking.send(new PackListRequestPayload()));
    }

    public void createPack(String name, String namespace) {
        Minecraft.getInstance().execute(() -> ClientPlayNetworking.send(new PackCreatePayload(name, namespace)));
    }

    public void clear() {
        store.setState(Snapshot.empty());
    }

    public void dispose() {
        syncSubscription.unsubscribe();
    }

    private void syncFromStore() {
        Snapshot state = snapshot();
        if (!availablePacks.equals(state.availablePacks()))
            availablePacks.setAll(state.availablePacks());
    }
}
