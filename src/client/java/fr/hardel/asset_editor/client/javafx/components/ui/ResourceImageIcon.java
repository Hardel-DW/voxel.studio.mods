package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelResourceLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourceImageIcon extends ImageView {

    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> MISSING = ConcurrentHashMap.newKeySet();

    public ResourceImageIcon(Identifier location, double size) {
        setFitWidth(size);
        setFitHeight(size);
        setPreserveRatio(true);
        setSmooth(false);
        String resourceKey = location.toString();
        String cacheKey = resourceKey + "@" + Math.round(size);
        if (MISSING.contains(resourceKey)) {
            setVisible(false);
            setManaged(false);
            return;
        }
        Image cached = CACHE.get(cacheKey);
        if (cached != null) {
            setImage(cached);
            return;
        }
        try (var stream = VoxelResourceLoader.open(location)) {
            Image image = new Image(stream, size, size, true, false);
            CACHE.put(cacheKey, image);
            setImage(image);
        } catch (Exception ignored) {
            MISSING.add(resourceKey);
            setVisible(false);
            setManaged(false);
        }
    }
}
