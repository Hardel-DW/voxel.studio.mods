package fr.hardel.asset_editor.workspace.action.recipe;

import fr.hardel.asset_editor.workspace.ElementEntry;
import fr.hardel.asset_editor.workspace.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.action.EditorAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Arrays;
import java.util.Optional;

public record SetCategoryAction(String category) implements EditorAction<Recipe<?>> {

    public static final StreamCodec<ByteBuf, SetCategoryAction> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetCategoryAction::category, SetCategoryAction::new);

    @Override
    public ElementEntry<Recipe<?>> apply(ElementEntry<Recipe<?>> entry, RegistryMutationContext ctx) {
        RecipeIngredientHelper helper = new RecipeIngredientHelper(ctx.registries());
        Recipe<?> updated = null;
        Optional<CraftingBookCategory> craftingCategory = parseCraftingCategory(category);

        if (craftingCategory.isPresent())
            updated = helper.setCraftingCategory(entry.data(), craftingCategory.get());

        if (updated == null) {
            Optional<CookingBookCategory> cookingCategory = parseCookingCategory(category);
            if (cookingCategory.isPresent())
                updated = helper.setCookingCategory(entry.data(), cookingCategory.get());
        }

        return updated == null ? entry : entry.withData(updated);
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
