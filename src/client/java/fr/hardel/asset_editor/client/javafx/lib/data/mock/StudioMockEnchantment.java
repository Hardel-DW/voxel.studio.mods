package fr.hardel.asset_editor.client.javafx.lib.data.mock;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import java.util.List;

public record StudioMockEnchantment(
        String namespace,
        String resource,
        int maxLevel,
        int weight,
        int anvilCost,
        List<String> slots,
        List<String> items,
        String exclusiveGroup) {

    public String uniqueKey() {
        return namespace + ":" + resource;
    }

    public Identifier identifier() {
        return Identifier.fromNamespaceAndPath(namespace, resource);
    }

    public String displayName() {
        return I18n.get(identifier().toLanguageKey("enchantment"));
    }

    public boolean isVanilla() {
        return "minecraft".equals(namespace);
    }

    public Identifier previewTexture() {
        return null;
    }
}


