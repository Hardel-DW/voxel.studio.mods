package fr.hardel.asset_editor.client.javafx.components.page.recipe;

import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeNodeModel;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecipeTreeBuilder {

    private static final List<RecipeBlockConfig> RECIPE_BLOCKS = List.of(
            new RecipeBlockConfig("minecraft:barrier", List.of(), true),
            new RecipeBlockConfig("minecraft:campfire", List.of("minecraft:campfire_cooking"), false),
            new RecipeBlockConfig("minecraft:furnace", List.of("minecraft:smelting"), false),
            new RecipeBlockConfig("minecraft:blast_furnace", List.of("minecraft:blasting"), false),
            new RecipeBlockConfig("minecraft:smoker", List.of("minecraft:smoking"), false),
            new RecipeBlockConfig("minecraft:stonecutter", List.of("minecraft:stonecutting"), false),
            new RecipeBlockConfig("minecraft:crafting_table", List.of(
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
            new RecipeBlockConfig("minecraft:smithing_table",
                    List.of("minecraft:smithing_transform", "minecraft:smithing_trim"),
                    false)
    );

    public static TreeNodeModel build(List<RecipeEntry> elements) {
        TreeNodeModel root = new TreeNodeModel();
        root.setCount(elements.size());

        for (RecipeBlockConfig block : RECIPE_BLOCKS) {
            if (block.special()) continue;

            ArrayList<RecipeEntry> matching = new ArrayList<>();
            for (RecipeEntry element : elements) {
                if (block.recipeTypes().contains(element.type())) {
                    matching.add(element);
                }
            }

            boolean hasSubTypes = block.recipeTypes().size() > 1;
            TreeNodeModel blockNode = new TreeNodeModel();
            blockNode.setCount(matching.size());

            if (hasSubTypes) {
                for (String recipeType : block.recipeTypes()) {
                    ArrayList<RecipeEntry> subMatching = new ArrayList<>();
                    for (RecipeEntry element : matching) {
                        if (recipeType.equals(element.type())) {
                            subMatching.add(element);
                        }
                    }
                    if (subMatching.isEmpty()) continue;
                    TreeNodeModel subNode = new TreeNodeModel();
                    subNode.setCount(subMatching.size());
                    for (RecipeEntry element : subMatching) {
                        subNode.identifiers().add(element.uniqueId());
                    }
                    blockNode.children().put(recipeType, subNode);
                }
            } else {
                for (RecipeEntry element : matching) {
                    blockNode.identifiers().add(element.uniqueId());
                }
            }

            root.children().put(block.id(), blockNode);
        }

        return root;
    }

    public static Map<String, Identifier> folderIcons() {
        LinkedHashMap<String, Identifier> icons = new LinkedHashMap<>();
        for (RecipeBlockConfig block : RECIPE_BLOCKS) {
            if (block.special()) continue;
            String resource = block.id().contains(":") ? block.id().split(":", 2)[1] : block.id();
            Identifier icon = Identifier.fromNamespaceAndPath("asset_editor", "textures/features/block/%s.png".formatted(resource));
            icons.put(block.id(), icon);
            for (String type : block.recipeTypes()) {
                icons.put(type, icon);
            }
        }
        return icons;
    }

    public record RecipeEntry(String uniqueId, String type) {
    }

    private record RecipeBlockConfig(String id, List<String> recipeTypes, boolean special) {
    }

    private RecipeTreeBuilder() {
    }
}
