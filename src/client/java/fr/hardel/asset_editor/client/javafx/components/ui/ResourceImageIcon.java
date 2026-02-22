package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.ResourceLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class ResourceImageIcon extends ImageView {

    public ResourceImageIcon(Identifier location, double size) {
        setFitWidth(size);
        setFitHeight(size);
        setPreserveRatio(true);
        try (var stream = ResourceLoader.open(location)) {
            setImage(new Image(stream));
        } catch (Exception ignored) {
            setVisible(false);
            setManaged(false);
        }
    }

    public ResourceImageIcon(String webPath, double size) {
        setFitWidth(size);
        setFitHeight(size);
        setPreserveRatio(true);
        for (Identifier id : imageCandidates(webPath)) {
            try (var stream = ResourceLoader.open(id)) {
                setImage(new Image(stream));
                return;
            } catch (Exception ignored) {
            }
        }
        setVisible(false);
        setManaged(false);
    }

    private static List<Identifier> imageCandidates(String webPath) {
        String normalized = normalize(webPath);
        ArrayList<Identifier> candidates = new ArrayList<>();
        candidates.add(Identifier.fromNamespaceAndPath("asset_editor", normalized));
        if (normalized.endsWith(".png")) {
            String webp = normalized.substring(0, normalized.length() - 4) + ".webp";
            candidates.add(Identifier.fromNamespaceAndPath("asset_editor", webp));
        }
        return candidates;
    }

    private static String normalize(String webPath) {
        if (webPath == null || webPath.isBlank()) {
            throw new IllegalArgumentException("webPath cannot be empty");
        }
        String path = webPath.trim().replace('\\', '/');
        if (path.startsWith("/")) path = path.substring(1);
        if (path.startsWith("images/")) return "textures/" + path.substring("images/".length());
        return path;
    }
}

