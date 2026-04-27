package fr.hardel.asset_editor.network;

import fr.hardel.asset_editor.network.data.ServerDataKey;
import fr.hardel.asset_editor.network.data.ServerDataKeys;
import fr.hardel.asset_editor.network.data.ServerDataRequestPayload;
import fr.hardel.asset_editor.network.data.ServerDataSyncPayload;
import fr.hardel.asset_editor.network.data.StudioDataKeys;
import fr.hardel.asset_editor.network.pack.PackCreatePayload;
import fr.hardel.asset_editor.network.pack.PackListRequestPayload;
import fr.hardel.asset_editor.network.pack.PackListSyncPayload;
import fr.hardel.asset_editor.network.pack.PackWorkspaceRequestPayload;
import fr.hardel.asset_editor.network.pack.PackWorkspaceSyncPayload;
import fr.hardel.asset_editor.network.workspace.ElementSeedRequestPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.network.workspace.WorkspaceMutationRequestPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload;
import fr.hardel.asset_editor.network.structure.StructureAssemblyRequestPayload;
import fr.hardel.asset_editor.network.structure.StructureAssemblyResolver;
import fr.hardel.asset_editor.network.structure.StructureAssemblyResponsePayload;
import fr.hardel.asset_editor.network.structure.StructureReplaceBlocksPayload;
import fr.hardel.asset_editor.network.structure.StructureTemplateCatalog;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.WorkspaceDefinition;
import fr.hardel.asset_editor.permission.PermissionManager;
import fr.hardel.asset_editor.permission.StudioPermissions;
import fr.hardel.asset_editor.workspace.io.DataPackManager;
import fr.hardel.asset_editor.workspace.io.ServerPackService;
import fr.hardel.asset_editor.workspace.WorkspaceAccessResolver;
import fr.hardel.asset_editor.workspace.WorkspaceBroadcastService;
import fr.hardel.asset_editor.workspace.WorkspaceMutationService;
import fr.hardel.asset_editor.workspace.WorkspaceQueryService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class AssetEditorNetworking {

    private static final WorkspaceAccessResolver WORKSPACE_ACCESS = new WorkspaceAccessResolver();
    private static final WorkspaceQueryService WORKSPACE_QUERY = new WorkspaceQueryService(WORKSPACE_ACCESS);
    private static final WorkspaceMutationService WORKSPACE_MUTATION = new WorkspaceMutationService(WORKSPACE_ACCESS);
    private static final WorkspaceBroadcastService WORKSPACE_BROADCAST = new WorkspaceBroadcastService();
    private static final ServerPackService PACK_SERVICE = new ServerPackService();

    public static void register() {
        StudioDataKeys.init();

        PayloadTypeRegistry.playS2C().register(PermissionSyncPayload.TYPE, PermissionSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WorkspaceSyncPayload.TYPE, WorkspaceSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PackWorkspaceSyncPayload.TYPE, PackWorkspaceSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PackListSyncPayload.TYPE, PackListSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerDataSyncPayload.TYPE, ServerDataSyncPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(WorkspaceMutationRequestPayload.TYPE, WorkspaceMutationRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(WorkspaceMutationRequestPayload.TYPE, AssetEditorNetworking::handleWorkspaceMutation);

        PayloadTypeRegistry.playC2S().register(PackListRequestPayload.TYPE, PackListRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PackListRequestPayload.TYPE, AssetEditorNetworking::handlePackListRequest);

        PayloadTypeRegistry.playC2S().register(PackWorkspaceRequestPayload.TYPE, PackWorkspaceRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PackWorkspaceRequestPayload.TYPE, AssetEditorNetworking::handlePackWorkspaceRequest);

        PayloadTypeRegistry.playC2S().register(PackCreatePayload.TYPE, PackCreatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PackCreatePayload.TYPE, AssetEditorNetworking::handlePackCreate);

        PayloadTypeRegistry.playC2S().register(ElementSeedRequestPayload.TYPE, ElementSeedRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ElementSeedRequestPayload.TYPE, AssetEditorNetworking::handleElementSeedRequest);

        PayloadTypeRegistry.playC2S().register(ServerDataRequestPayload.TYPE, ServerDataRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ServerDataRequestPayload.TYPE, AssetEditorNetworking::handleServerDataRequest);

        PayloadTypeRegistry.playC2S().register(StructureReplaceBlocksPayload.TYPE, StructureReplaceBlocksPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(StructureReplaceBlocksPayload.TYPE, AssetEditorNetworking::handleStructureReplaceBlocks);

        PayloadTypeRegistry.playC2S().register(StructureAssemblyRequestPayload.TYPE, StructureAssemblyRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StructureAssemblyResponsePayload.TYPE, StructureAssemblyResponsePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(StructureAssemblyRequestPayload.TYPE, AssetEditorNetworking::handleStructureAssemblyRequest);

        PayloadTypeRegistry.playC2S().register(ReloadRequestPayload.TYPE, ReloadRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ReloadRequestPayload.TYPE, AssetEditorNetworking::handleReloadRequest);
    }

    public static void sendPermissions(ServerPlayer player, StudioPermissions permissions) {
        ServerPlayNetworking.send(player, new PermissionSyncPayload(permissions));
    }

    public static void sendPackList(ServerPlayer player, java.util.List<DataPackManager.PackEntry> packs) {
        ServerPlayNetworking.send(player, new PackListSyncPayload(packs));
    }

    public static void broadcastPackList(MinecraftServer server, java.util.List<DataPackManager.PackEntry> packs) {
        PackListSyncPayload payload = new PackListSyncPayload(packs);
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            ServerPlayNetworking.send(player, payload);
    }

    public static <T> void broadcastServerData(MinecraftServer server, ServerDataKey<T> key, java.util.List<T> data) {
        ServerDataSyncPayload payload = ServerDataSyncPayload.create(key, data);
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            ServerPlayNetworking.send(player, payload);
    }

    public static void broadcastAllServerData(MinecraftServer server) {
        for (ServerDataKey<?> key : ServerDataKeys.all()) {
            if (key.automaticBroadcast()) {
                broadcastResolved(server, key);
            } else {
                broadcastServerData(server, key, java.util.List.of());
            }
        }
    }

    private static <T> void broadcastResolved(MinecraftServer server, ServerDataKey<T> key) {
        broadcastServerData(server, key, key.provider().apply(server));
    }

    private static void sendMutationResult(ServerPlayer player, UUID actionId, String packId, boolean accepted, String errorCode, WorkspaceElementSnapshot snapshot, boolean modifiedVsReference) {
        ServerPlayNetworking.send(player, WorkspaceSyncPayload.mutationResult(actionId, packId, accepted, errorCode, snapshot, modifiedVsReference));
    }

    private static void handlePackWorkspaceRequest(PackWorkspaceRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> WORKSPACE_QUERY.loadPackWorkspace(context.player(), context.server(), payload.packId(), payload.registryId()).ifPresent(response -> ServerPlayNetworking.send(context.player(), response)));
    }

    private static void handlePackListRequest(PackListRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> PACK_SERVICE.listPacks().ifPresent(packs -> sendPackList(context.player(), packs)));
    }

    private static void handlePackCreate(PackCreatePayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> PACK_SERVICE.createPack(context.player(), payload.name(), payload.namespace(), payload.icon()).ifPresent(packs -> broadcastPackList(context.server(), packs)));
    }

    private static void handleWorkspaceMutation(WorkspaceMutationRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            MinecraftServer server = context.server();
            WorkspaceMutationService.MutationResult result = WORKSPACE_MUTATION.mutate(player, server, payload);
            if (result instanceof WorkspaceMutationService.MutationResult.Failure(String errorCode)) {
                sendMutationResult(player, payload.actionId(), payload.packId(), false, errorCode, null, false);
                return;
            }

            var success = (WorkspaceMutationService.MutationResult.Success) result;
            sendMutationResult(player, payload.actionId(), success.packId(), true, "", success.snapshot(), success.modifiedVsReference());
            WORKSPACE_BROADCAST.broadcastMutation(server, player, success.packId(), success.snapshot(), success.modifiedVsReference());
        });
    }

    private static void handleElementSeedRequest(ElementSeedRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            WorkspaceAccessResolver.Resolution resolution = WORKSPACE_ACCESS.resolveEditable(
                context.player(), context.server(), payload.packId(), payload.registryId());
            if (!(resolution instanceof WorkspaceAccessResolver.Resolution.Success access)) return;

            sendElementSeed(context.player(), access.definition(), access, payload.elementId());
        });
    }

    private static <T> void sendElementSeed(ServerPlayer player, WorkspaceDefinition<T> definition,
        WorkspaceAccessResolver.Resolution.Success access, net.minecraft.resources.Identifier elementId) {
        ElementEntry<T> entry = access.repository().get(access.packId(), definition, access.packRoot(), access.registries(), elementId);
        if (entry == null) return;

        WorkspaceElementSnapshot snapshot = definition.toSnapshot(entry, access.registries());
        boolean modifiedVsReference = access.repository().isModifiedVsReference(
            access.packId(), definition, access.packRoot(), access.registries(), elementId);
        ServerPlayNetworking.send(player, WorkspaceSyncPayload.remoteSync(access.packId(), snapshot, modifiedVsReference));
    }

    private static void handleServerDataRequest(ServerDataRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> resolveAndSendServerData(context.player(), context.server(), payload.key(), payload.ids()));
    }

    private static void handleStructureReplaceBlocks(StructureReplaceBlocksPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            PermissionManager permissionManager = PermissionManager.get();
            if (permissionManager == null || !permissionManager.getEffectivePermissions(context.player()).canEdit())
                return;

            DataPackManager packManager = DataPackManager.get();
            if (packManager == null)
                return;

            if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(payload.fromBlock())
                || !net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(payload.toBlock()))
                return;

            packManager.resolveWritablePack(payload.packId()).ifPresent(packRoot -> {
                StructureTemplateCatalog.readRaw(context.server(), payload.structureId()).ifPresent(tag -> {
                    if (StructureTemplateCatalog.replaceBlocks(tag, payload.fromBlock(), payload.toBlock()) <= 0)
                        return;
                    try {
                        StructureTemplateCatalog.writeRaw(packRoot, payload.structureId(), tag);
                        broadcastPartialResolved(context.server(), StudioDataKeys.STRUCTURE_TEMPLATES, java.util.List.of(payload.structureId()));
                    } catch (java.io.IOException ignored) {
                    }
                });
            });
        });
    }

    private static void handleStructureAssemblyRequest(StructureAssemblyRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            var snapshot = StructureAssemblyResolver.resolve(context.server(), payload.structureId());
            ServerPlayNetworking.send(context.player(), new StructureAssemblyResponsePayload(payload.structureId(), snapshot));
        });
    }

    private static void handleReloadRequest(ReloadRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            PermissionManager manager = PermissionManager.get();
            if (manager == null || !manager.isAdmin(player.getUUID())) return;
            MinecraftServer server = context.server();
            server.reloadResources(server.getPackRepository().getSelectedIds());
        });
    }

    private static <T> void broadcastPartialResolved(MinecraftServer server, ServerDataKey<T> key, java.util.List<net.minecraft.resources.Identifier> ids) {
        ServerDataSyncPayload payload = ServerDataSyncPayload.createPartial(key, key.partialProvider().apply(server, ids));
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            ServerPlayNetworking.send(player, payload);
    }

    private static void resolveAndSendServerData(ServerPlayer player, MinecraftServer server, net.minecraft.resources.Identifier key, java.util.List<net.minecraft.resources.Identifier> ids) {
        ServerPlayNetworking.send(player, ServerDataKeys.resolve(key, ids, server));
    }

    private AssetEditorNetworking() {}
}
