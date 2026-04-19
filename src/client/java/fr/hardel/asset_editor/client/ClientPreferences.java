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

    public record Data(
        Map<String, String> lastSelectedPack,
        boolean showFpsCounter,
        boolean disableVsync,
        boolean stayOnSplash,
        boolean showHoverTriangle
    ) {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                .optionalFieldOf("last_selected_pack", Map.of())
                .forGetter(Data::lastSelectedPack),
            Codec.BOOL.optionalFieldOf("show_fps_counter", false).forGetter(Data::showFpsCounter),
            Codec.BOOL.optionalFieldOf("disable_vsync", false).forGetter(Data::disableVsync),
            Codec.BOOL.optionalFieldOf("stay_on_splash", false).forGetter(Data::stayOnSplash),
            Codec.BOOL.optionalFieldOf("show_hover_triangle", false).forGetter(Data::showHoverTriangle))
            .apply(i, (packs, fps, vsync, splash, triangle) ->
                new Data(new HashMap<>(packs), fps, vsync, splash, triangle)));

        public Data() {
            this(new HashMap<>(), false, false, false, false);
        }
    }

    private static final ModConfig<Data> CONFIG = new ModConfig<>(FabricLoader.getInstance().getConfigDir().resolve("asset_editor.json"), Data.CODEC, new Data());

    public static void register() {
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

    public static boolean showFpsCounter() {
        return CONFIG.get().showFpsCounter();
    }

    public static void setShowFpsCounter(boolean value) {
        Data current = CONFIG.get();
        CONFIG.set(new Data(current.lastSelectedPack(), value, current.disableVsync(), current.stayOnSplash(), current.showHoverTriangle()));
        CONFIG.save();
    }

    public static boolean disableVsync() {
        return CONFIG.get().disableVsync();
    }

    public static void setDisableVsync(boolean value) {
        Data current = CONFIG.get();
        CONFIG.set(new Data(current.lastSelectedPack(), current.showFpsCounter(), value, current.stayOnSplash(), current.showHoverTriangle()));
        CONFIG.save();
    }

    public static boolean stayOnSplash() {
        return CONFIG.get().stayOnSplash();
    }

    public static void setStayOnSplash(boolean value) {
        Data current = CONFIG.get();
        CONFIG.set(new Data(current.lastSelectedPack(), current.showFpsCounter(), current.disableVsync(), value, current.showHoverTriangle()));
        CONFIG.save();
    }

    public static boolean showHoverTriangle() {
        return CONFIG.get().showHoverTriangle();
    }

    public static void setShowHoverTriangle(boolean value) {
        Data current = CONFIG.get();
        CONFIG.set(new Data(current.lastSelectedPack(), current.showFpsCounter(), current.disableVsync(), current.stayOnSplash(), value));
        CONFIG.save();
    }

    private static String worldId() {
        Minecraft client = Minecraft.getInstance();
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
