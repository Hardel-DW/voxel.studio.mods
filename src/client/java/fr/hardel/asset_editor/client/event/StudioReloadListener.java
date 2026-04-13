package fr.hardel.asset_editor.client.event;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.bootstrap.StudioWindowFacade;
import fr.hardel.asset_editor.client.rendering.ItemAtlasRenderer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jspecify.annotations.NonNull;

public final class StudioReloadListener implements ResourceManagerReloadListener {

    public static void register() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES)
            .registerReloader(
                Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "studio_reload"),
                new StudioReloadListener());
    }

    @Override
    public void onResourceManagerReload(@NonNull ResourceManager manager) {
        ItemAtlasRenderer.requestGeneration();
        StudioWindowFacade.notifyResourceReload();
    }
}
