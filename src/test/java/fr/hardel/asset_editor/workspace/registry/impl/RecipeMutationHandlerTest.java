package fr.hardel.asset_editor.workspace.registry.impl;

import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.store.CustomFields;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.workspace.action.recipe.RecipeEditorActions;
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistries;
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistry;
import fr.hardel.asset_editor.workspace.registry.RegistryMutationContext;
import fr.hardel.asset_editor.workspace.registry.RegistryMutationContexts;
import fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBinding;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.TransmuteResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeMutationHandlerTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        try {
            RecipeAdapterRegistries.register();
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void shapedIngredientMutationRemainsEncodable() {
        HolderLookup.Provider registries = registries();
        RegistryMutationContext context = RegistryMutationContexts.client(registries);
        RecipeMutationHandler handler = new RecipeMutationHandler();
        ElementEntry<Recipe<?>> entry = entry(shapedRecipe());

        ElementEntry<Recipe<?>> updated = handler.apply(
            entry,
            new RecipeEditorActions.AddIngredient(8, List.of(Identifier.withDefaultNamespace("stick")), true),
            context
        );

        String encoded = Recipe.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), updated.data())
            .result()
            .map(Object::toString)
            .orElse(null);
        assertNotNull(encoded);
        assertTrue(encoded.contains("\"type\":\"minecraft:crafting_shaped\""));
        assertTrue(encoded.contains("minecraft:stick"));
    }

    @Test
    void transmuteConversionProducesRecipe() {
        Recipe<?> converted = RecipeAdapterRegistry.convert(
            shapedRecipe(),
            Identifier.withDefaultNamespace("crafting_transmute"),
            true
        );

        TransmuteRecipe transmute = assertInstanceOf(TransmuteRecipe.class, converted);
        assertEquals(Items.CHEST, transmute.result.item().value());
        assertEquals(1, transmute.result.count());
    }

    @Test
    void resultCountIsClampedToNonStackableMaximum() {
        HolderLookup.Provider registries = registries();
        RegistryMutationContext context = RegistryMutationContexts.client(registries);
        RecipeMutationHandler handler = new RecipeMutationHandler();
        ElementEntry<Recipe<?>> entry = entry(transmuteBoatRecipe(1));

        ElementEntry<Recipe<?>> updated = handler.apply(
            entry,
            new RecipeEditorActions.SetResultCount(4),
            context
        );

        TransmuteRecipe recipe = assertInstanceOf(TransmuteRecipe.class, updated.data());
        assertEquals(1, recipe.result.count());
    }

    @Test
    void invalidRecipeSnapshotThrowsInsteadOfReturningEmptyJson() {
        RegistryWorkspaceBinding<Recipe<?>> binding = new RegistryWorkspaceBinding<>(Registries.RECIPE, Recipe.CODEC, null, null);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> binding.toSnapshot(entry(transmuteBoatRecipe(4)), registries())
        );

        assertTrue(exception.getMessage().contains("Failed to encode workspace snapshot"));
    }

    private static HolderLookup.Provider registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    private static ElementEntry<Recipe<?>> entry(Recipe<?> recipe) {
        return new ElementEntry<>(
            Identifier.fromNamespaceAndPath("test", "entry"),
            recipe,
            Set.of(),
            CustomFields.EMPTY
        );
    }

    private static ShapedRecipe shapedRecipe() {
        return new ShapedRecipe(
            "",
            CraftingBookCategory.MISC,
            ShapedRecipePattern.of(
                Map.of('P', Ingredient.of(Items.OAK_PLANKS)),
                List.of("PP", "PP")
            ),
            new ItemStack(Items.CHEST),
            true
        );
    }

    private static TransmuteRecipe transmuteBoatRecipe(int count) {
        return new TransmuteRecipe(
            "",
            CraftingBookCategory.MISC,
            Ingredient.of(Items.OAK_PLANKS),
            Ingredient.of(Items.STICK),
            new TransmuteResult(Items.ACACIA_BOAT.builtInRegistryHolder(), count, DataComponentPatch.EMPTY)
        );
    }
}
