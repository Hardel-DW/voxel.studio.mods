package fr.hardel.asset_editor.client.javafx.components.page.recipe;

import fr.hardel.asset_editor.client.javafx.components.ui.tree.TreeNodeModel;
import fr.hardel.asset_editor.client.javafx.lib.data.RecipeTreeData;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecipeTreeBuilder {

    public static TreeNodeModel build(List<RecipeEntry> elements) {
        TreeNodeModel root = new TreeNodeModel();
        root.setCount(elements.size());

        for (RecipeTreeData.RecipeBlockConfig block : RecipeTreeData.RECIPE_BLOCKS) {
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
        for (RecipeTreeData.RecipeBlockConfig block : RecipeTreeData.RECIPE_BLOCKS) {
            if (block.special()) continue;
            icons.put(block.id(), block.icon());
            for (String type : block.recipeTypes()) {
                icons.put(type, block.icon());
            }
        }
        return icons;
    }

    public record RecipeEntry(String uniqueId, String type) {
    }

    private RecipeTreeBuilder() {
    }
}
