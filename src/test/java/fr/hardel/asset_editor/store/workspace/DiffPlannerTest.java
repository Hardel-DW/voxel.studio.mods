package fr.hardel.asset_editor.store.workspace;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.workspace.flush.CustomFields;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.flush.FlushAdapter;
import fr.hardel.asset_editor.workspace.flush.adapter.RecipeFlushAdapter;
import fr.hardel.asset_editor.workspace.action.recipe.adapter.RecipeAdapterRegistries;
import fr.hardel.asset_editor.workspace.WorkspaceDefinition;
import fr.hardel.asset_editor.workspace.io.DiffPlanner;
import fr.hardel.asset_editor.workspace.io.RegistryDiffPlan;
import fr.hardel.asset_editor.workspace.WorkspaceRegistry;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiffPlannerTest {

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
    void revertingAnAddedTagDeletesTheExistingTagFile() {
        Identifier entryId = Identifier.fromNamespaceAndPath(Identifier.DEFAULT_NAMESPACE, "aqua_affinity");
        Identifier curseTagId = Identifier.fromNamespaceAndPath(Identifier.DEFAULT_NAMESPACE, "curse");

        ElementEntry<String> referenceEntry = new ElementEntry<>(
            entryId,
            "aqua_affinity",
            Set.of(),
            CustomFields.EMPTY);
        ElementEntry<String> addedTagEntry = referenceEntry.toggleTag(curseTagId);

        WorkspaceRegistry<String> workspace = new WorkspaceRegistry<>(
            Map.of(entryId, referenceEntry),
            Map.of(entryId, referenceEntry));

        WorkspaceDefinition<String> definition = WorkspaceDefinition.of(
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Identifier.DEFAULT_NAMESPACE, "enchantment")),
            Codec.STRING, FlushAdapter.identity());

        DiffPlanner planner = new DiffPlanner();
        Path packRoot = Path.of("test-pack");
        Path cursePath = packRoot.resolve("data").resolve(Identifier.DEFAULT_NAMESPACE)
            .resolve("tags").resolve("enchantment").resolve("curse.json");

        workspace.put(entryId, addedTagEntry);
        RegistryDiffPlan<String> addPlan = planner.plan(packRoot, definition, workspace, JsonOps.INSTANCE);
        assertEquals(1, addPlan.tagWrites().size());
        assertEquals(cursePath, addPlan.tagWrites().getFirst().path());

        workspace.clearDirty();
        workspace.put(entryId, referenceEntry);

        RegistryDiffPlan<String> revertPlan = planner.plan(packRoot, definition, workspace, JsonOps.INSTANCE);
        assertTrue(revertPlan.tagWrites().isEmpty());
        assertEquals(Set.of(cursePath), Set.copyOf(revertPlan.tagDeletes()));
    }

    @Test
    void canonicalRecipeComparisonAvoidsWritingEquivalentVanillaRecipe() {
        Identifier entryId = Identifier.withDefaultNamespace("acacia_boat");
        Recipe<?> reference = new ShapedRecipe(
            "boat",
            CraftingBookCategory.MISC,
            ShapedRecipePattern.of(Map.of('#', Ingredient.of(Items.ACACIA_PLANKS)), List.of("# #", "###")),
            new ItemStack(Items.ACACIA_BOAT),
            true);
        Recipe<?> equivalent = new ShapedRecipe(
            "boat",
            CraftingBookCategory.MISC,
            ShapedRecipePattern.of(Map.of('A', Ingredient.of(Items.ACACIA_PLANKS)), List.of("A A", "AAA")),
            new ItemStack(Items.ACACIA_BOAT),
            true);

        ElementEntry<Recipe<?>> referenceEntry = new ElementEntry<>(entryId, reference, Set.of(), CustomFields.EMPTY);
        ElementEntry<Recipe<?>> currentEntry = new ElementEntry<>(entryId, equivalent, Set.of(), CustomFields.EMPTY);
        WorkspaceRegistry<Recipe<?>> workspace = new WorkspaceRegistry<>(
            Map.of(entryId, referenceEntry),
            Map.of(entryId, referenceEntry));
        WorkspaceDefinition<Recipe<?>> definition = WorkspaceDefinition.of(
            Registries.RECIPE, Recipe.CODEC, RecipeFlushAdapter.INSTANCE);
        Path packRoot = Path.of("test-pack");

        workspace.put(entryId, currentEntry);
        RegistryDiffPlan<Recipe<?>> plan = new DiffPlanner().plan(
            packRoot,
            definition,
            workspace,
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).createSerializationContext(JsonOps.INSTANCE));

        assertTrue(plan.elementWrites().isEmpty());
        assertEquals(
            Set.of(packRoot.resolve("data").resolve(Identifier.DEFAULT_NAMESPACE).resolve("recipe").resolve("acacia_boat.json")),
            Set.copyOf(plan.elementDeletes()));
    }
}
