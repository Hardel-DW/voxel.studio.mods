package fr.hardel.asset_editor.network;

import com.mojang.serialization.Codec;
import fr.hardel.asset_editor.permission.PermissionManager;
import fr.hardel.asset_editor.permission.StudioPermissions;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.store.FlushAdapter;
import fr.hardel.asset_editor.store.ServerElementStore;
import fr.hardel.asset_editor.store.ServerPackManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AssetEditorNetworking {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssetEditorNetworking.class);

    public record RegistryBinding<T>(ResourceKey<Registry<T>> registryKey, Codec<T> codec, FlushAdapter<T> adapter) {}

    private static final Map<String, RegistryBinding<?>> BINDINGS = new HashMap<>();

    public static <T> void registerBinding(ResourceKey<Registry<T>> key, Codec<T> codec, FlushAdapter<T> adapter) {
        BINDINGS.put(key.identifier().getPath(), new RegistryBinding<>(key, codec, adapter));
    }

    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(PermissionSyncPayload.TYPE, PermissionSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EditorActionResponsePayload.TYPE, EditorActionResponsePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ElementUpdatePayload.TYPE, ElementUpdatePayload.CODEC);

        PayloadTypeRegistry.playC2S().register(EditorActionPayload.TYPE, EditorActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(EditorActionPayload.TYPE, AssetEditorNetworking::handleEditorAction);

        PayloadTypeRegistry.playS2C().register(PackListSyncPayload.TYPE, PackListSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PackListRequestPayload.TYPE, PackListRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PackListRequestPayload.TYPE, AssetEditorNetworking::handlePackListRequest);

        PayloadTypeRegistry.playC2S().register(PackCreatePayload.TYPE, PackCreatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PackCreatePayload.TYPE, AssetEditorNetworking::handlePackCreate);
    }

    public static void sendPermissions(ServerPlayer player, StudioPermissions permissions) {
        ServerPlayNetworking.send(player, new PermissionSyncPayload(permissions));
    }

    public static void sendPackList(ServerPlayer player, java.util.List<ServerPackManager.PackEntry> packs) {
        ServerPlayNetworking.send(player, new PackListSyncPayload(packs));
    }

    private static void handleEditorAction(EditorActionPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            MinecraftServer server = context.server();
            var permManager = PermissionManager.get();
            if (permManager == null) {
                sendResponse(player, payload.actionId(), false, "error:server_unavailable");
                return;
            }

            var perms = permManager.getEffectivePermissions(player);
            if (!perms.canEdit()) {
                sendResponse(player, payload.actionId(), false, "error:permission_denied");
                return;
            }

            var store = ServerElementStore.get();
            if (store == null) {
                sendResponse(player, payload.actionId(), false, "error:server_unavailable");
                return;
            }

            ElementEntry<?> entry = store.get(payload.registryId(), payload.targetId());
            if (entry == null) {
                sendResponse(player, payload.actionId(), false, "error:element_not_found");
                return;
            }

            ElementEntry<?> updated = ActionInterpreter.apply(payload.registryId(), entry, payload.action(), server.registryAccess());
            store.put(payload.registryId(), payload.targetId(), updated);

            flushElement(server, payload.packId(), payload.registryId());

            sendResponse(player, payload.actionId(), true, "");
            broadcastUpdate(server, player, payload.registryId(), payload.targetId(), payload.action());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> void flushElement(MinecraftServer server, String packId, Identifier registryId) {
        var binding = (RegistryBinding<T>) BINDINGS.get(registryId.getPath());
        if (binding == null)
            return;

        var store = ServerElementStore.get();
        if (store == null)
            return;

        Path packRoot = resolvePackRoot(server, packId);
        if (packRoot == null) {
            LOGGER.warn("Cannot resolve pack root for packId: {}", packId);
            return;
        }

        store.flushDirty(packRoot, binding.registryKey(), binding.codec(), server.registryAccess(), binding.adapter());
    }

    private static Path resolvePackRoot(MinecraftServer server, String packId) {
        if (packId == null || packId.isBlank())
            return null;
        String name = packId.startsWith("file/") ? packId.substring(5) : packId;
        Path datapackDir = server.getWorldPath(LevelResource.DATAPACK_DIR);
        Path resolved = datapackDir.resolve(name).normalize();
        if (!resolved.startsWith(datapackDir.normalize()))
            return null;
        return resolved;
    }

    private static void broadcastUpdate(MinecraftServer server, ServerPlayer sender,
        Identifier registryId, Identifier targetId, EditorAction action) {
        var permManager = PermissionManager.get();
        if (permManager == null)
            return;

        var payload = new ElementUpdatePayload(registryId, targetId, action);
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == sender)
                continue;
            if (!permManager.getEffectivePermissions(other).canEdit())
                continue;
            ServerPlayNetworking.send(other, payload);
        }
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

    private static void sendResponse(ServerPlayer player, UUID actionId, boolean accepted, String message) {
        ServerPlayNetworking.send(player, new EditorActionResponsePayload(actionId, accepted, message));
    }

    private AssetEditorNetworking() {}
}
