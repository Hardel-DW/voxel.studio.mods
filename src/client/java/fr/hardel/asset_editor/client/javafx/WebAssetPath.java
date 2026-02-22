package fr.hardel.asset_editor.client.javafx;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class WebAssetPath {

    public static Identifier toIdentifier(String webPath) {
        String normalized = normalize(webPath);
        return Identifier.fromNamespaceAndPath("asset_editor", normalized);
    }

    public static List<Identifier> imageCandidates(String webPath) {
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

    private WebAssetPath() {
    }
}
