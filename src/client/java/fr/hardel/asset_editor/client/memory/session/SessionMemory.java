package fr.hardel.asset_editor.client.memory.session;

import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.client.memory.core.Subscription;
import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.ClientPackInfo;
import fr.hardel.asset_editor.client.network.ClientPayloadSender;
import fr.hardel.asset_editor.network.recipe.RecipeCatalogSyncPayload;
import fr.hardel.asset_editor.network.pack.PackCreatePayload;
import fr.hardel.asset_editor.permission.StudioPermissions;
import fr.hardel.asset_editor.store.ServerPackManager.PackEntry;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class SessionMemory implements ReadableMemory<SessionMemory.Snapshot> {

    public record Snapshot(StudioPermissions permissions, List<ClientPackInfo> availablePacks, List<RecipeCatalogSyncPayload.Entry> recipeCatalog, String worldSessionKey,
        boolean permissionsReceived, boolean packListReceived) {

        public Snapshot {
            permissions = permissions == null ? StudioPermissions.NONE : permissions;
            availablePacks = List.copyOf(availablePacks == null ? List.of() : availablePacks);
            recipeCatalog = List.copyOf(recipeCatalog == null ? List.of() : recipeCatalog);
            worldSessionKey = worldSessionKey == null ? "" : worldSessionKey;
        }

        public static Snapshot empty() {
            return new Snapshot(StudioPermissions.NONE, List.of(), List.of(), "", false, false);
        }
    }

    private final SimpleMemory<Snapshot> memory = new SimpleMemory<>(Snapshot.empty());

    @Override
    public Snapshot snapshot() {
        return memory.snapshot();
    }

    @Override
    public Subscription subscribe(Runnable listener) {
        return memory.subscribe(listener);
    }

    public StudioPermissions permissions() {
        return snapshot().permissions();
    }

    public List<ClientPackInfo> availablePacks() {
        return snapshot().availablePacks();
    }

    public List<RecipeCatalogSyncPayload.Entry> recipeCatalog() {
        return snapshot().recipeCatalog();
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
        memory.update(state -> new Snapshot(state.permissions(), state.availablePacks(), state.recipeCatalog(), key, state.permissionsReceived(), state.packListReceived()));
    }

    public void updatePermissions(StudioPermissions nextPermissions) {
        memory.update(state -> new Snapshot(nextPermissions, state.availablePacks(), state.recipeCatalog(), state.worldSessionKey(), true, state.packListReceived()));
    }

    public void updatePacks(List<PackEntry> entries) {
        List<ClientPackInfo> packs = entries == null ? List.of() : entries.stream().map(ClientPackInfo::from).toList();
        memory.update(state -> new Snapshot(state.permissions(), packs, state.recipeCatalog(), state.worldSessionKey(), state.permissionsReceived(), true));
    }

    public void updateRecipeCatalog(List<RecipeCatalogSyncPayload.Entry> entries) {
        List<RecipeCatalogSyncPayload.Entry> recipeCatalog = entries == null ? List.of() : List.copyOf(entries);
        memory.update(state -> new Snapshot(state.permissions(), state.availablePacks(), recipeCatalog, state.worldSessionKey(), state.permissionsReceived(), state.packListReceived()));
    }

    public void createPack(String name, String namespace) {
        Minecraft.getInstance().execute(() -> ClientPayloadSender.send(new PackCreatePayload(name, namespace)));
    }

    public void clear() {
        memory.setSnapshot(Snapshot.empty());
    }
}
