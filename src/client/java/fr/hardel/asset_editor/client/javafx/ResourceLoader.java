package fr.hardel.asset_editor.client.javafx;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;

/**
 * Thread-safe bridge that provides access to Minecraft's ResourceManager
 * from the JavaFX thread. Updated by the asset reload listener.
 */
public final class ResourceLoader {

    private static volatile ResourceManager current = ResourceManager.Empty.INSTANCE;

    public static void update(ResourceManager manager) {
        current = manager;
    }

    public static InputStream open(Identifier location) throws IOException {
        return current.open(location);
    }

    private ResourceLoader() {}
}
