package fr.hardel.asset_editor.client.javafx.lib.data;

import net.minecraft.resources.Identifier;

import java.util.List;

public record ExclusiveSetGroup(Identifier tagId) {

    public static final List<ExclusiveSetGroup> ALL = List.of(
            new ExclusiveSetGroup(Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/armor")),
            new ExclusiveSetGroup(Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/bow")),
            new ExclusiveSetGroup(Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/crossbow")),
            new ExclusiveSetGroup(Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/damage")),
            new ExclusiveSetGroup(Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/riptide")),
            new ExclusiveSetGroup(Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/mining")),
            new ExclusiveSetGroup(Identifier.fromNamespaceAndPath("minecraft", "exclusive_set/boots"))
    );

    public String id() {
        return tagId.getPath();
    }

    public Identifier image() {
        return tagId.withPath("textures/studio/enchantment/" + tagId.getPath() + ".png");
    }

    public String value() {
        return "#" + tagId;
    }
}
