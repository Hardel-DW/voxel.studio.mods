package fr.hardel.asset_editor.client.javafx.lib.data;

import net.minecraft.resources.Identifier;

public record StudioElementId(Identifier identifier, String registry) {

    public static StudioElementId parse(String raw) {
        if (raw == null || raw.isBlank())
            return null;
        String[] parts = raw.split("\\|", 2);
        String idPart = parts[0];
        String registryPart = parts.length > 1 ? parts[1] : null;
        Identifier identifier = Identifier.tryParse(idPart);
        if (identifier == null)
            return null;
        return new StudioElementId(identifier, registryPart == null || registryPart.isBlank() ? null : registryPart);
    }

    public String namespace() {
        return identifier.getNamespace();
    }

    public String resourcePath() {
        return identifier.getPath();
    }
}
