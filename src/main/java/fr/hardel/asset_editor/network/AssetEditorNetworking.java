package fr.hardel.asset_editor.network;

import fr.hardel.asset_editor.network.pack.PackCreatePayload;
import fr.hardel.asset_editor.network.pack.PackListRequestPayload;
import fr.hardel.asset_editor.network.pack.PackListSyncPayload;
import fr.hardel.asset_editor.network.pack.PackWorkspaceRequestPayload;
import fr.hardel.asset_editor.network.pack.PackWorkspaceSyncPayload;
import fr.hardel.asset_editor.network.session.PermissionSyncPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.network.workspace.WorkspaceMutationRequestPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload;
import fr.hardel.asset_editor.permission.PermissionManager;
import fr.hardel.asset_editor.permission.StudioPermissions;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.store.ServerPackManager;
import fr.hardel.asset_editor.store.workspace.WorkspaceRepository;
import fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBinding;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AssetEditorNetworking {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssetEditorNetworking.class);

    private static final Map<String, RegistryWorkspaceBinding<?>> BINDINGS = new HashMap<>();

    public static <T> void registerBinding(RegistryWorkspaceBinding<T> binding) {
        BINDINGS.put(binding.registryId().toString(), binding);
    }

    @SuppressWarnings("unchecked")
    public static <T> RegistryWorkspaceBinding<T> binding(Identifier registryId) {
        return (RegistryWorkspaceBinding<T>) BINDINGS.get(registryId.toString());
    }

    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(PermissionSyncPayload.TYPE, PermissionSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WorkspaceSyncPayload.TYPE, WorkspaceSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PackWorkspaceSyncPayload.TYPE, PackWorkspaceSyncPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(WorkspaceMutationRequestPayload.TYPE, WorkspaceMutationRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(WorkspaceMutationRequestPayload.TYPE, AssetEditorNetworking::handleWorkspaceMutation);

        PayloadTypeRegistry.playS2C().register(PackListSyncPayload.TYPE, PackListSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PackListRequestPayload.TYPE, PackListRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PackListRequestPayload.TYPE, AssetEditorNetworking::handlePackListRequest);
        PayloadTypeRegistry.playC2S().register(PackWorkspaceRequestPayload.TYPE, PackWorkspaceRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PackWorkspaceRequestPayload.TYPE, AssetEditorNetworking::handlePackWorkspaceRequest);

        PayloadTypeRegistry.playC2S().register(PackCreatePayload.TYPE, PackCreatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PackCreatePayload.TYPE, AssetEditorNetworking::handlePackCreate);
    }

    public static void sendPermissions(ServerPlayer player, StudioPermissions permissions) {
        ServerPlayNetworking.send(player, new PermissionSyncPayload(permissions));
    }

    public static void sendPackList(ServerPlayer player, java.util.List<ServerPackManager.PackEntry> packs) {
        ServerPlayNetworking.send(player, new PackListSyncPayload(packs));
    }

    private static void handleWorkspaceMutation(WorkspaceMutationRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            MinecraftServer server = context.server();
            var permManager = PermissionManager.get();
            if (permManager == null) {
                sendMutationResult(player, payload.actionId(), payload.packId(), false,
                    "error:server_unavailable", null);
                return;
            }

            if (!permManager.getEffectivePermissions(player).canEdit()) {
                sendMutationResult(player, payload.actionId(), payload.packId(), false,
                    "error:permission_denied", null);
                return;
            }

            var repository = WorkspaceRepository.get();
            var packManager = ServerPackManager.get();
            if (repository == null || packManager == null) {
                sendMutationResult(player, payload.actionId(), payload.packId(), false,
                    "error:server_unavailable", null);
                return;
            }

            var binding = binding(payload.registryId());
            if (binding == null) {
                sendMutationResult(player, payload.actionId(), payload.packId(), false,
                    "error:invalid_registry", null);
                return;
            }

            var packRoot = packManager.resolveWritablePack(payload.packId());
            if (packRoot.isEmpty()) {
                sendMutationResult(player, payload.actionId(), payload.packId(), false,
                    "error:invalid_pack", null);
                return;
            }

            handleWorkspaceMutationTyped(payload, player, server, repository, binding, packRoot.get());
        });
    }

    private static void handlePackWorkspaceRequest(PackWorkspaceRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            var permManager = PermissionManager.get();
            var repository = WorkspaceRepository.get();
            var packManager = ServerPackManager.get();
            if (permManager == null || repository == null || packManager == null)
                return;
            if (!permManager.getEffectivePermissions(context.player()).canEdit())
                return;

            var binding = binding(payload.registryId());
            if (binding == null)
                return;

            var packRoot = packManager.resolveWritablePack(payload.packId());
            if (packRoot.isEmpty())
                return;

            sendPackWorkspace(context.player(), payload.packId(), binding,
                repository.snapshotWorkspace(payload.packId(), binding, packRoot.get(), context.server().registryAccess()),
                context.server().registryAccess());
        });
    }

    private static void handlePackListRequest(PackListRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            var packManager = ServerPackManager.get();
            if (packManager == null)
                return;
            sendPackList(context.player(), packManager.listPacks());
        });
    }

    private static void handlePackCreate(PackCreatePayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            var permManager = PermissionManager.get();
            if (permManager == null)
                return;
            if (!permManager.getEffectivePermissions(context.player()).canEdit())
                return;

            var packManager = ServerPackManager.get();
            if (packManager == null)
                return;
            packManager.createPack(payload.name(), payload.namespace());
            sendPackList(context.player(), packManager.listPacks());
        });
    }

    private static void broadcastUpdate(MinecraftServer server, ServerPlayer sender,
        String packId, WorkspaceElementSnapshot snapshot) {
        var permManager = PermissionManager.get();
        if (permManager == null)
            return;

        var payload = WorkspaceSyncPayload.remoteSync(packId, snapshot);
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == sender)
                continue;
            if (!permManager.getEffectivePermissions(other).canEdit())
                continue;
            ServerPlayNetworking.send(other, payload);
        }
    }

    private static <T> void sendPackWorkspace(ServerPlayer player, String packId, RegistryWorkspaceBinding<T> binding,
        List<ElementEntry<T>> entries, net.minecraft.core.HolderLookup.Provider registries) {
        List<WorkspaceElementSnapshot> snapshots = entries.stream()
            .map(entry -> binding.toSnapshot(entry, registries))
            .toList();
        ServerPlayNetworking.send(player, new PackWorkspaceSyncPayload(packId, binding.registryId(), snapshots));
    }

    private static void sendMutationResult(ServerPlayer player, UUID actionId, String packId,
        boolean accepted, String errorCode, WorkspaceElementSnapshot snapshot) {
        ServerPlayNetworking.send(player, WorkspaceSyncPayload.mutationResult(actionId, packId, accepted, errorCode, snapshot));
    }

    private static <T> void handleWorkspaceMutationTyped(WorkspaceMutationRequestPayload payload,
        ServerPlayer player, MinecraftServer server, WorkspaceRepository repository,
        RegistryWorkspaceBinding<T> binding, Path packRoot) {
        ElementEntry<T> entry = repository.get(payload.packId(), binding, packRoot, server.registryAccess(), payload.targetId());
        if (entry == null) {
            sendMutationResult(player, payload.actionId(), payload.packId(), false,
                "error:element_not_found", null);
            return;
        }

        ElementEntry<T> updated;
        try {
            updated = binding.interpreter().apply(entry, payload.action(), server.registryAccess());
        } catch (Exception e) {
            LOGGER.warn("Action rejected for {}: {}", payload.targetId(), e.getMessage());
            sendMutationResult(player, payload.actionId(), payload.packId(), false,
                "error:invalid_action", null);
            return;
        }

        repository.put(payload.packId(), binding, packRoot, server.registryAccess(), payload.targetId(), updated);
        repository.flushDirty(packRoot, payload.packId(), binding, server.registryAccess());
        WorkspaceElementSnapshot snapshot = binding.toSnapshot(updated, server.registryAccess());

        sendMutationResult(player, payload.actionId(), payload.packId(), true, "", snapshot);
        broadcastUpdate(server, player, payload.packId(), snapshot);
    }

    private AssetEditorNetworking() {}
}
