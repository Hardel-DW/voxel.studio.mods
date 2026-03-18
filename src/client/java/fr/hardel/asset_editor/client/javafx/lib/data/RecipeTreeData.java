package fr.hardel.asset_editor.client.javafx.lib.data;

import net.minecraft.resources.Identifier;

import java.util.List;

public final class RecipeTreeData {

    public static final List<RecipeBlockConfig> RECIPE_BLOCKS = List.of(
        new RecipeBlockConfig(Identifier.fromNamespaceAndPath("minecraft", "barrier"), List.of(), true),
        new RecipeBlockConfig(Identifier.fromNamespaceAndPath("minecraft", "campfire"),
            List.of(Identifier.fromNamespaceAndPath("minecraft", "campfire_cooking")), false),
        new RecipeBlockConfig(Identifier.fromNamespaceAndPath("minecraft", "furnace"),
            List.of(Identifier.fromNamespaceAndPath("minecraft", "smelting")), false),
        new RecipeBlockConfig(Identifier.fromNamespaceAndPath("minecraft", "blast_furnace"),
            List.of(Identifier.fromNamespaceAndPath("minecraft", "blasting")), false),
        new RecipeBlockConfig(Identifier.fromNamespaceAndPath("minecraft", "smoker"),
            List.of(Identifier.fromNamespaceAndPath("minecraft", "smoking")), false),
        new RecipeBlockConfig(Identifier.fromNamespaceAndPath("minecraft", "stonecutter"),
            List.of(Identifier.fromNamespaceAndPath("minecraft", "stonecutting")), false),
        new RecipeBlockConfig(Identifier.fromNamespaceAndPath("minecraft", "crafting_table"), List.of(
            Identifier.fromNamespaceAndPath("minecraft", "crafting_shapeless"),
            Identifier.fromNamespaceAndPath("minecraft", "crafting_shaped"),
            Identifier.fromNamespaceAndPath("minecraft", "crafting_decorated_pot"),
            Identifier.fromNamespaceAndPath("minecraft", "crafting_special_armordye"),
            Identifier.fromNamespaceAndPath("minecraft", "crafting_special_bannerduplicate"),
            Identifier.fromNamespaceAndPath("minecraft", "crafting_special_bookcloning"),
            Identifier.fromNamespaceAndPath("minecraft", "crafting_special_firework_rocket"),
            Identifier.fromNamespaceAndPath("minecraft", "crafting_special_firework_star"),
            Identifier.fromNamespaceAndPath("minecraft", "crafting_special_firework_star_fade"),
            Identifier.fromNamespaceAndPath("minecraft", "crafting_special_mapcloning"),
            Identifier.fromNamespaceAndPath("minecraft", "crafting_special_mapextending"),
            Identifier.fromNamespaceAndPath("minecraft", "crafting_special_repairitem"),
            Identifier.fromNamespaceAndPath("minecraft", "crafting_special_shielddecoration"),
            Identifier.fromNamespaceAndPath("minecraft", "crafting_special_tippedarrow"),
            Identifier.fromNamespaceAndPath("minecraft", "crafting_transmute")), false),
        new RecipeBlockConfig(Identifier.fromNamespaceAndPath("minecraft", "smithing_table"), List.of(
            Identifier.fromNamespaceAndPath("minecraft", "smithing_transform"),
            Identifier.fromNamespaceAndPath("minecraft", "smithing_trim")), false));

    public record RecipeBlockConfig(Identifier blockId, List<Identifier> recipeTypes, boolean special) {

        public Identifier icon() {
            return blockId.withPath("textures/studio/block/" + blockId.getPath() + ".png");
        }
    }

    private RecipeTreeData() {}
}
