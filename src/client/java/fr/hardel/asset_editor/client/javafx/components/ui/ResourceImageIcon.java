package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.ResourceLoader;
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
        String key = location.toString();
        if (MISSING.contains(key)) {
            setVisible(false);
            setManaged(false);
            return;
        }
        Image cached = CACHE.get(key);
        if (cached != null) {
            setImage(cached);
            return;
        }
        try (var stream = ResourceLoader.open(location)) {
            Image image = new Image(stream);
            CACHE.put(key, image);
            setImage(image);
        } catch (Exception ignored) {
            MISSING.add(key);
            setVisible(false);
            setManaged(false);
        }
    }
}

