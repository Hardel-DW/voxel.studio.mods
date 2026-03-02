package fr.hardel.asset_editor.client.javafx.lib.store;

import net.minecraft.resources.Identifier;

public record StudioElementId(Identifier identifier, String registry) {

    public static StudioElementId parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        int separator = value.indexOf('$');
        String identifierPart = separator >= 0 ? value.substring(0, separator) : value;
        String registryPart = separator >= 0 && separator + 1 < value.length() ? value.substring(separator + 1) : null;
        Identifier identifier = Identifier.tryParse(identifierPart);
        if (identifier == null) {
            return null;
        }
        return new StudioElementId(identifier, registryPart == null || registryPart.isBlank() ? null : registryPart);
    }

    public String namespace() {
        return identifier.getNamespace();
    }

    public String resourcePath() {
        return identifier.getPath();
    }

    public String resourceLeaf() {
        String path = resourcePath();
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    public String uniqueKey() {
        return registry == null ? identifier.toString() : identifier + "$" + registry;
    }
}
