package fr.hardel.asset_editor.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.hardel.asset_editor.client.javafx.VoxelResourceLoader;
import fr.hardel.asset_editor.client.javafx.VoxelStudioWindow;
import fr.hardel.asset_editor.client.network.ClientNetworkHandler;
import fr.hardel.asset_editor.client.rendering.ItemAtlasRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

public class AssetEditorClient implements ClientModInitializer {

    public static final String BUILD_VERSION = "26.0.1-RC";

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("asset_editor", "main"));
    private static final KeyMapping OPEN_STUDIO = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.asset_editor.open_studio", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, CATEGORY));
    private boolean hadWorld;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean hasWorld = client.level != null && client.getConnection() != null;
            if (this.hadWorld && !hasWorld) {
                ClientPermissionState.reset();
                ClientPackCache.reset();
                VoxelStudioWindow.onWorldClosed();
            }

            this.hadWorld = hasWorld;
            if (!OPEN_STUDIO.consumeClick()) return;
            if (client.player != null && !ClientPermissionState.hasReceived()) {
                client.player.displayClientMessage(Component.translatable("studio:permission.waiting"), false);
                return;
            }
            if (client.player != null && ClientPermissionState.get().isNone()) {
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
}
