package fr.hardel.asset_editor.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientPreferences {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientPreferences.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("asset_editor.json");

    private static JsonObject lastPackByWorld = new JsonObject();

    public static String lastPackId() {
        String worldId = worldId();
        if (worldId == null || !lastPackByWorld.has(worldId)) return null;
        return lastPackByWorld.get(worldId).getAsString();
    }

    public static void setLastPackId(String packId) {
        String worldId = worldId();
        if (worldId == null) return;
        if (packId != null)
            lastPackByWorld.addProperty(worldId, packId);
        else
            lastPackByWorld.remove(worldId);
        save();
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            if (json.has("lastPackByWorld"))
                lastPackByWorld = json.getAsJsonObject("lastPackByWorld");
        } catch (Exception e) {
            LOGGER.warn("Failed to load client preferences: {}", e.getMessage());
        }
    }

    private static void save() {
        JsonObject json = new JsonObject();
        json.add("lastPackByWorld", lastPackByWorld);
        try {
            Files.writeString(FILE, GSON.toJson(json));
        } catch (IOException e) {
            LOGGER.warn("Failed to save client preferences: {}", e.getMessage());
        }
    }

    private static String worldId() {
        var client = Minecraft.getInstance();
        var serverInfo = client.getCurrentServer();
        if (serverInfo != null) return serverInfo.ip;
        var sp = client.getSingleplayerServer();
        if (sp != null) return sp.getWorldData().getLevelName();
        return null;
    }

    private ClientPreferences() {}
}
