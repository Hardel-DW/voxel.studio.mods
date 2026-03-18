package fr.hardel.asset_editor.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.hardel.asset_editor.client.javafx.VoxelResourceLoader;
import fr.hardel.asset_editor.client.javafx.VoxelStudioWindow;
import fr.hardel.asset_editor.client.network.ClientNetworkHandler;
import fr.hardel.asset_editor.client.rendering.ItemAtlasRenderer;
import fr.hardel.asset_editor.client.state.ClientSessionState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

public class AssetEditorClient implements ClientModInitializer {

    public static final String BUILD_VERSION = "26.0.1-RC";
    private static final ClientSessionState SESSION_STATE = new ClientSessionState();
    private static final ClientSessionDispatch SESSION_DISPATCH = new ClientSessionDispatch(SESSION_STATE);

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("asset_editor", "main"));
    private static final KeyMapping OPEN_STUDIO = KeyBindingHelper.registerKeyBinding(
        new KeyMapping("key.asset_editor.open_studio", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, CATEGORY));
    private boolean hadWorld;

    public static ClientSessionState sessionState() {
        return SESSION_STATE;
    }

    public static ClientSessionDispatch sessionDispatch() {
        return SESSION_DISPATCH;
    }

    @Override
    public void onInitializeClient() {
        ClientPreferences.load();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean hasWorld = client.level != null && client.getConnection() != null;
            if (this.hadWorld && !hasWorld) {
                SESSION_STATE.clear();
                VoxelStudioWindow.onWorldClosed();
            }

            this.hadWorld = hasWorld;
            if (hasWorld)
                SESSION_STATE.setWorldSessionKey(computeWorldSessionKey());
            if (!OPEN_STUDIO.consumeClick())
                return;

            if (client.player != null && !SESSION_STATE.hasReceivedPermissions()) {
                client.player.displayClientMessage(Component.translatable("studio:permission.waiting"), false);
                return;
            }

            if (client.player != null && SESSION_STATE.permissions().isNone()) {
                client.player.displayClientMessage(Component.translatable("studio:permission.blocked"), false);
                return;
            }

            VoxelStudioWindow.open();
        });

        ClientNetworkHandler.register();
        ResourceLoader.get(PackType.CLIENT_RESOURCES)
            .registerReloader(
                Identifier.fromNamespaceAndPath("asset_editor", "studio_reload"),
                new StudioReloadListener());
    }

    private static final class StudioReloadListener implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(@NonNull ResourceManager manager) {
            VoxelResourceLoader.update(manager);
            ItemAtlasRenderer.requestGeneration();
            VoxelStudioWindow.onResourceReload();
        }
    }

    private static String computeWorldSessionKey() {
        Minecraft client = Minecraft.getInstance();
        var server = client.getSingleplayerServer();
        if (server != null)
            return "sp:" + server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();

        var conn = client.getConnection();
        if (conn == null)
            return "";

        ServerData data = client.getCurrentServer();
        if (data != null && !data.ip.isBlank())
            return "mp:" + data.ip;
        return "mp:" + conn.getConnection().getRemoteAddress();
    }
}
