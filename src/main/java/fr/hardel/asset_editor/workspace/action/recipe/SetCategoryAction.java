package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import fr.hardel.asset_editor.workspace.action.EditorActionType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Arrays;
import java.util.Optional;

public record SetCategoryAction(String category) implements EditorAction {

    public static final EditorActionType<Recipe<?>, SetCategoryAction> TYPE = new EditorActionType<>(
        Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "recipe/set_category"),
        SetCategoryAction.class,
        StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetCategoryAction::category, SetCategoryAction::new),
        (entry, action, ctx) -> {
            RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
            Recipe<?> updated = null;
            Optional<CraftingBookCategory> craftingCategory = parseCraftingCategory(action.category());

            if (craftingCategory.isPresent())
                updated = helper.setCraftingCategory(entry.data(), craftingCategory.get());

            if (updated == null) {
                Optional<CookingBookCategory> cookingCategory = parseCookingCategory(action.category());
                if (cookingCategory.isPresent())
                    updated = helper.setCookingCategory(entry.data(), cookingCategory.get());
            }

            return updated == null ? entry : entry.withData(updated);
        });

    @Override
    public EditorActionType<Recipe<?>, SetCategoryAction> type() {
        return TYPE;
    }

    private static Optional<CraftingBookCategory> parseCraftingCategory(String category) {
        return Arrays.stream(CraftingBookCategory.values())
            .filter(value -> value.getSerializedName().equals(category))
            .findFirst();
    }

    private static Optional<CookingBookCategory> parseCookingCategory(String category) {
        return Arrays.stream(CookingBookCategory.values())
            .filter(value -> value.getSerializedName().equals(category))
            .findFirst();
    }
}
