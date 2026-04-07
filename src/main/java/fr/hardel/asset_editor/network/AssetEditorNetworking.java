package fr.hardel.asset_editor.network;

import fr.hardel.asset_editor.network.data.ServerDataKey;
import fr.hardel.asset_editor.network.data.ServerDataRequestPayload;
import fr.hardel.asset_editor.network.data.ServerDataSyncPayload;
import fr.hardel.asset_editor.network.data.StudioDataKeys;
import fr.hardel.asset_editor.network.pack.PackCreatePayload;
import fr.hardel.asset_editor.network.pack.PackListRequestPayload;
import fr.hardel.asset_editor.network.pack.PackListSyncPayload;
import fr.hardel.asset_editor.network.pack.PackWorkspaceRequestPayload;
import fr.hardel.asset_editor.network.pack.PackWorkspaceSyncPayload;
import fr.hardel.asset_editor.network.recipe.RecipeCatalogBuilder;
import fr.hardel.asset_editor.network.workspace.ElementSeedRequestPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceElementSnapshot;
import fr.hardel.asset_editor.network.workspace.WorkspaceMutationRequestPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload;
import fr.hardel.asset_editor.data.compendium.CompendiumTagLoader;
import fr.hardel.asset_editor.data.recipe.RecipeEntryLoader;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.WorkspaceDefinition;
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

    public static <T> void sendServerData(ServerPlayer player, ServerDataKey<T> key, java.util.List<T> data) {
        ServerPlayNetworking.send(player, ServerDataSyncPayload.create(key, data));
    }

    public static <T> void broadcastServerData(MinecraftServer server, ServerDataKey<T> key, java.util.List<T> data) {
        ServerDataSyncPayload payload = ServerDataSyncPayload.create(key, data);
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            ServerPlayNetworking.send(player, payload);
    }

    public static void broadcastAllServerData(MinecraftServer server) {
        broadcastServerData(server, StudioDataKeys.RECIPE_CATALOG, RecipeCatalogBuilder.build(server));
        broadcastServerData(server, StudioDataKeys.COMPENDIUM_ITEMS, CompendiumTagLoader.itemGroups());
        broadcastServerData(server, StudioDataKeys.COMPENDIUM_ENCHANTMENTS, CompendiumTagLoader.enchantmentGroups());
        broadcastServerData(server, StudioDataKeys.RECIPE_ENTRIES, RecipeEntryLoader.entries());
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
            if (result instanceof WorkspaceMutationService.MutationResult.Failure(String errorCode)) {
                sendMutationResult(player, payload.actionId(), payload.packId(), false, errorCode, null);
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
            if (!(resolution instanceof WorkspaceAccessResolver.Resolution.Success access)) return;

            sendElementSeed(context.player(), access.definition(), access, payload.elementId());
        });
    }

    private static <T> void sendElementSeed(ServerPlayer player, WorkspaceDefinition<T> definition,
        WorkspaceAccessResolver.Resolution.Success access, net.minecraft.resources.Identifier elementId) {
        ElementEntry<T> entry = access.repository().get(access.packId(), definition, access.packRoot(), access.registries(), elementId);
        if (entry == null) return;

        WorkspaceElementSnapshot snapshot = definition.toSnapshot(entry, access.registries());
        ServerPlayNetworking.send(player, WorkspaceSyncPayload.remoteSync(access.packId(), snapshot));
    }

    private static void handleServerDataRequest(ServerDataRequestPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> resolveAndSendServerData(context.player(), context.server(), payload.key()));
    }

    private static void resolveAndSendServerData(ServerPlayer player, MinecraftServer server, net.minecraft.resources.Identifier key) {
        if (key.equals(StudioDataKeys.RECIPE_CATALOG.id()))
            sendServerData(player, StudioDataKeys.RECIPE_CATALOG, RecipeCatalogBuilder.build(server));
        else if (key.equals(StudioDataKeys.COMPENDIUM_ITEMS.id()))
            sendServerData(player, StudioDataKeys.COMPENDIUM_ITEMS, CompendiumTagLoader.itemGroups());
        else if (key.equals(StudioDataKeys.COMPENDIUM_ENCHANTMENTS.id()))
            sendServerData(player, StudioDataKeys.COMPENDIUM_ENCHANTMENTS, CompendiumTagLoader.enchantmentGroups());
        else if (key.equals(StudioDataKeys.RECIPE_ENTRIES.id()))
            sendServerData(player, StudioDataKeys.RECIPE_ENTRIES, RecipeEntryLoader.entries());
    }

    private AssetEditorNetworking() {}
}
