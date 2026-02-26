package fr.hardel.asset_editor.client.javafx.lib.data;

import net.minecraft.resources.Identifier;

import java.util.List;

public record SlotConfig(String id, String nameKey, Identifier image, List<String> slots) {

    public static Identifier featureImage(String name) {
        return Identifier.fromNamespaceAndPath("asset_editor", "textures/features/slots/" + name + ".png");
    }
}
