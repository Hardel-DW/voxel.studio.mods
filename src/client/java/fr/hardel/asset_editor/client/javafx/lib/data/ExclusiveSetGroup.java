package fr.hardel.asset_editor.client.javafx.lib.data;

import net.minecraft.resources.Identifier;

import java.util.List;

public record ExclusiveSetGroup(String id, Identifier image, String value) {

    public static final List<ExclusiveSetGroup> ALL = List.of(
        new ExclusiveSetGroup("armor",    featureImage("armor"),        "#minecraft:exclusive_set/armor"),
        new ExclusiveSetGroup("bow",      featureImage("bow"),          "#minecraft:exclusive_set/bow"),
        new ExclusiveSetGroup("crossbow", featureImage("crossbow"),     "#minecraft:exclusive_set/crossbow"),
        new ExclusiveSetGroup("damage",   featureImage("sword"),        "#minecraft:exclusive_set/damage"),
        new ExclusiveSetGroup("riptide",  featureImage("trident"),      "#minecraft:exclusive_set/riptide"),
        new ExclusiveSetGroup("mining",   featureImage("mining_loot"),  "#minecraft:exclusive_set/mining"),
        new ExclusiveSetGroup("boots",    featureImage("foot_armor"),   "#minecraft:exclusive_set/boots")
    );

    private static Identifier featureImage(String name) {
        return Identifier.fromNamespaceAndPath("asset_editor", "textures/features/item/" + name + ".png");
    }
}
