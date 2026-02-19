package fr.hardel.asset_editor.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.hardel.asset_editor.client.javafx.ResourceLoader;
import fr.hardel.asset_editor.client.javafx.VoxelStudioWindow;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.lwjgl.glfw.GLFW;

public class AssetEditorClient implements ClientModInitializer {

    public static final String BUILD_VERSION = "24.0.1-RC";

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("asset_editor", "main"));
    private static final KeyMapping OPEN_STUDIO = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.asset_editor.open_studio", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY));

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (OPEN_STUDIO.consumeClick())
                VoxelStudioWindow.open();
        });

        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(PackType.CLIENT_RESOURCES)
                .registerReloader(
                        Identifier.fromNamespaceAndPath("asset_editor", "studio_reload"),
                        new StudioReloadListener());
    }

    private static final class StudioReloadListener implements ResourceManagerReloadListener {

        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            ResourceLoader.update(manager);
            VoxelStudioWindow.onResourceReload();
        }
    }
}
