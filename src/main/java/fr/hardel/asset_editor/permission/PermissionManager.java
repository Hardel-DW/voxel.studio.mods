package fr.hardel.asset_editor.permission;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.network.AssetEditorNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PermissionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final LevelResource STORAGE_PATH = new LevelResource("asset_editor_permissions.json");
    private static final Codec<Map<UUID, StudioPermissions>> STORAGE_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, StudioPermissions.CODEC);

    private static PermissionManager instance;

    private final MinecraftServer server;
    private final Path filePath;
    private final ConcurrentHashMap<UUID, StudioPermissions> permissions = new ConcurrentHashMap<>();

    private PermissionManager(MinecraftServer server) {
        this.server = server;
        this.filePath = server.getWorldPath(STORAGE_PATH);
    }

    public static void init(MinecraftServer server) {
        instance = new PermissionManager(server);
        instance.load();
    }

    public static void shutdown() {
        if (instance != null) {
            instance.save();
            instance = null;
        }
    }

    public static PermissionManager get() {
        return instance;
    }

    public boolean isHostAdmin(UUID playerId) {
        if (server.isDedicatedServer())
            return false;
        var hostProfile = server.getSingleplayerProfile();
        return hostProfile != null && playerId.equals(hostProfile.id());
    }

    public StudioPermissions getEffectivePermissions(ServerPlayer player) {
        return getEffectivePermissions(player.getUUID());
    }

    public void setPermissions(UUID playerId, StudioPermissions perms) {
        permissions.put(playerId, perms);
        save();
        syncToPlayer(playerId);
    }

    public boolean isAdmin(UUID playerId) {
        return getEffectivePermissions(playerId).isAdmin();
    }

    public StudioPermissions getEffectivePermissions(UUID playerId) {
        if (isHostAdmin(playerId))
            return StudioPermissions.ADMIN;
        return permissions.getOrDefault(playerId, StudioPermissions.NONE);
    }

    public void syncToPlayer(UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null)
            return;
        syncToPlayer(player);
    }

    public void syncToPlayer(ServerPlayer player) {
        AssetEditorNetworking.sendPermissions(player, getEffectivePermissions(player));
    }

    private void load() {
        if (!Files.exists(filePath))
            return;
        try {
            JsonElement element = JsonParser.parseString(Files.readString(filePath));
            STORAGE_CODEC.parse(JsonOps.INSTANCE, element)
                .ifSuccess(permissions::putAll)
                .ifError(error -> LOGGER.warn("Failed to load permissions: {}", error.message()));
        } catch (IOException e) {
            LOGGER.warn("Failed to read permissions file", e);
        }
    }

    private void save() {
        STORAGE_CODEC.encodeStart(JsonOps.INSTANCE, Map.copyOf(permissions))
            .ifSuccess(json -> writeFile(GSON.toJson(json)))
            .ifError(error -> LOGGER.warn("Failed to encode permissions: {}", error.message()));
    }

    private void writeFile(String content) {
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content);
        } catch (IOException e) {
            LOGGER.warn("Failed to write permissions file", e);
        }
    }
}
