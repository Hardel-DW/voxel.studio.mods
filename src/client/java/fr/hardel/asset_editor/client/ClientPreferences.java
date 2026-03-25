package fr.hardel.asset_editor.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.hardel.asset_editor.client.config.ModConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

import java.util.HashMap;
import java.util.Map;

public final class ClientPreferences {

    public record Data(Map<String, String> lastSelectedPack) {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                .optionalFieldOf("last_selected_pack", Map.of())
                .forGetter(Data::lastSelectedPack))
            .apply(i, data -> new Data(new HashMap<>(data))));

        public Data() {
            this(new HashMap<>());
        }
    }

    private static final ModConfig<Data> CONFIG = new ModConfig<>(FabricLoader.getInstance().getConfigDir().resolve("asset_editor.json"), Data.CODEC, new Data());

    public static void load() {
        CONFIG.load();
    }

    public static String lastPackId() {
        String worldId = worldId();
        if (worldId == null)
            return null;

        return CONFIG.get().lastSelectedPack().get(worldId);
    }

    public static void setLastPackId(String packId) {
        String worldId = worldId();
        if (worldId == null) {
            return;
        }

        if (packId != null)
            CONFIG.get().lastSelectedPack().put(worldId, packId);
        else
            CONFIG.get().lastSelectedPack().remove(worldId);

        CONFIG.save();
    }

    private static String worldId() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return null;
        }

        ServerData serverInfo = client.getCurrentServer();
        if (serverInfo != null) {
            return serverInfo.ip;
        }

        IntegratedServer sp = client.getSingleplayerServer();
        if (sp != null) {
            return sp.getWorldData().getLevelName();
        }

        return null;
    }

    private ClientPreferences() {}
}
