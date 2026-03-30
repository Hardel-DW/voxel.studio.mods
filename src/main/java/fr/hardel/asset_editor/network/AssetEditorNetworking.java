package fr.hardel.asset_editor.network;

import fr.hardel.asset_editor.network.pack.PackCreatePayload;
import fr.hardel.asset_editor.network.pack.PackListRequestPayload;
import fr.hardel.asset_editor.network.pack.PackListSyncPayload;
import fr.hardel.asset_editor.network.pack.PackWorkspaceRequestPayload;
import fr.hardel.asset_editor.network.pack.PackWorkspaceSyncPayload;
import fr.hardel.asset_editor.network.recipe.RecipeCatalogBuilder;
import fr.hardel.asset_editor.network.recipe.RecipeCatalogSyncPayload;
import fr.hardel.asset_editor.network.session.PermissionSyncPayload;
import fr.hardel.asset_editor.network.workspace.ElementSeedRequestPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.network.workspace.WorkspaceMutationRequestPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBinding;
import fr.hardel.asset_editor.permission.StudioPermissions;
import fr.hardel.asset_editor.store.ServerPackManager;
import fr.hardel.asset_editor.store.ServerPackService;
import fr.hardel.asset_editor.workspace.service.ResolvedWorkspaceAccess;
import fr.hardel.asset_editor.workspace.service.WorkspaceAccessResolver;
import fr.hardel.asset_editor.workspace.service.WorkspaceBroadcastService;
import fr.hardel.asset_editor.workspace.service.WorkspaceMutationService;
import fr.hardel.asset_editor.workspace.service.WorkspaceQueryService;
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
        PayloadTypeRegistry.playS2C().register(PermissionSyncPayload.TYPE, PermissionSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RecipeCatalogSyncPayload.TYPE, RecipeCatalogSyncPayload.CODEC);
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

        PayloadTypeRegistry.playC2S().register(ElementSeedRequestPayload.TYPE, ElementSeedRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ElementSeedRequestPayload.TYPE, AssetEditorNetworking::handleElementSeedRequest);
    }

    public static void sendPermissions(ServerPlayer player, StudioPermissions permissions) {
        ServerPlayNetworking.send(player, new PermissionSyncPayload(permissions));
    }

    public static void sendPackList(ServerPlayer player, java.util.List<ServerPackManager.PackEntry> packs) {
        ServerPlayNetworking.send(player, new PackListSyncPayload(packs));
    }

    public static void sendRecipeCatalog(ServerPlayer player, MinecraftServer server) {
        ServerPlayNetworking.send(player, RecipeCatalogBuilder.build(server));
    }

    public static void broadcastPackList(MinecraftServer server, java.util.List<ServerPackManager.PackEntry> packs) {
        PackListSyncPayload payload = new PackListSyncPayload(packs);
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            ServerPlayNetworking.send(player, payload);
    }

    public static void broadcastRecipeCatalog(MinecraftServer server) {
        RecipeCatalogSyncPayload payload = RecipeCatalogBuilder.build(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            ServerPlayNetworking.send(player, payload);
    }

    private static void sendMutationResult(ServerPlayer player, UUID actionId, String packId, boolean accepted, String errorCode, WorkspaceElementSnapshot snapshot) {
        ServerPlayNetworking.send(player, WorkspaceSyncPayload.mutationResult(actionId, packId, accepted, errorCode, snapshot));
    }

    private static void handlePackWorkspaceRequest(PackWorkspaceRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> WORKSPACE_QUERY.loadPackWorkspace(context.player(), context.server(), payload.packId(), payload.registryId()).ifPresent(response -> ServerPlayNetworking.send(context.player(), response)));
    }

    private static void handlePackListRequest(PackListRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> PACK_SERVICE.listPacks().ifPresent(packs -> sendPackList(context.player(), packs)));
    }

    private static void handlePackCreate(PackCreatePayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> PACK_SERVICE.createPack(context.player(), payload.name(), payload.namespace()).ifPresent(packs -> broadcastPackList(context.server(), packs)));
    }

    private static void handleWorkspaceMutation(WorkspaceMutationRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            MinecraftServer server = context.server();
            WorkspaceMutationService.MutationResult result = WORKSPACE_MUTATION.mutate(player, server, payload);
            if (result instanceof WorkspaceMutationService.MutationResult.Failure failure) {
                sendMutationResult(player, payload.actionId(), payload.packId(), false, failure.errorCode(), null);
                return;
            }

            var success = (WorkspaceMutationService.MutationResult.Success) result;
            sendMutationResult(player, payload.actionId(), success.packId(), true, "", success.snapshot());
            WORKSPACE_BROADCAST.broadcastMutation(server, player, success.packId(), success.snapshot());
        });
    }

    private static void handleElementSeedRequest(ElementSeedRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            WorkspaceAccessResolver.Resolution resolution = WORKSPACE_ACCESS.resolveEditable(
                context.player(), context.server(), payload.packId(), payload.registryId());
            if (!(resolution instanceof WorkspaceAccessResolver.Resolution.Success success)) return;

            sendElementSeed(context.player(), success.access().binding(), success.access(), payload.elementId());
        });
    }

    private static <T> void sendElementSeed(ServerPlayer player, RegistryWorkspaceBinding<T> binding,
        ResolvedWorkspaceAccess access, net.minecraft.resources.Identifier elementId) {
        ElementEntry<T> entry = access.repository().get(access.packId(), binding, access.packRoot(), access.registries(), elementId);
        if (entry == null) return;

        WorkspaceElementSnapshot snapshot = binding.toSnapshot(entry, access.registries());
        ServerPlayNetworking.send(player, WorkspaceSyncPayload.remoteSync(access.packId(), snapshot));
    }

    private AssetEditorNetworking() {}
}
