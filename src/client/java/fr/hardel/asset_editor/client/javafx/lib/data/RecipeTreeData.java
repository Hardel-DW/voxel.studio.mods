package fr.hardel.asset_editor.client.javafx.lib.data;

import net.minecraft.resources.Identifier;

import java.util.List;

public final class RecipeTreeData {

    public static final List<RecipeBlockConfig> RECIPE_BLOCKS = List.of(
            block("minecraft:barrier", List.of(), true),
            block("minecraft:campfire", List.of("minecraft:campfire_cooking"), false),
            block("minecraft:furnace", List.of("minecraft:smelting"), false),
            block("minecraft:blast_furnace", List.of("minecraft:blasting"), false),
            block("minecraft:smoker", List.of("minecraft:smoking"), false),
            block("minecraft:stonecutter", List.of("minecraft:stonecutting"), false),
            block("minecraft:crafting_table", List.of(
                    "minecraft:crafting_shapeless",
                    "minecraft:crafting_shaped",
                    "minecraft:crafting_decorated_pot",
                    "minecraft:crafting_special_armordye",
                    "minecraft:crafting_special_bannerduplicate",
                    "minecraft:crafting_special_bookcloning",
                    "minecraft:crafting_special_firework_rocket",
                    "minecraft:crafting_special_firework_star",
                    "minecraft:crafting_special_firework_star_fade",
                    "minecraft:crafting_special_mapcloning",
                    "minecraft:crafting_special_mapextending",
                    "minecraft:crafting_special_repairitem",
                    "minecraft:crafting_special_shielddecoration",
                    "minecraft:crafting_special_tippedarrow",
                    "minecraft:crafting_transmute"
            ), false),
            block("minecraft:smithing_table",
                    List.of("minecraft:smithing_transform", "minecraft:smithing_trim"),
                    false)
    );

    public record RecipeBlockConfig(String id, List<String> recipeTypes, boolean special, Identifier icon) {
    }

    private static RecipeBlockConfig block(String id, List<String> recipeTypes, boolean special) {
        String resource = id.contains(":") ? id.split(":", 2)[1] : id;
        return new RecipeBlockConfig(
                id,
                recipeTypes,
                special,
                Identifier.fromNamespaceAndPath("asset_editor", "textures/features/block/%s.png".formatted(resource)));
    }

    private RecipeTreeData() {
    }
}
