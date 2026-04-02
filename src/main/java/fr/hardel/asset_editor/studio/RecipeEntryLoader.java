package fr.hardel.asset_editor.studio;

import com.google.gson.JsonElement;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class RecipeEntryLoader implements PreparableReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeEntryLoader.class);
    private static final FileToIdConverter LISTER = FileToIdConverter.json("studio/recipe");

    private static volatile List<RecipeEntryDefinition> entries = List.of();

    public static List<RecipeEntryDefinition> entries() {
        return entries;
    }

    @Override
    public CompletableFuture<Void> reload(SharedState sharedState, Executor prepExecutor, PreparationBarrier barrier, Executor applyExecutor) {
        ResourceManager manager = sharedState.resourceManager();
        return CompletableFuture.supplyAsync(() -> prepare(manager), prepExecutor)
            .thenCompose(barrier::wait)
            .thenAcceptAsync(this::apply, applyExecutor);
    }

    private List<RecipeEntryDefinition> prepare(ResourceManager manager) {
        List<RecipeEntryDefinition> result = new ArrayList<>();

        for (Map.Entry<Identifier, Resource> entry : LISTER.listMatchingResources(manager).entrySet()) {
            Identifier location = entry.getKey();
            Identifier entryId = LISTER.fileToId(location);

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement element = StrictJsonParser.parse(reader);
                RecipeEntryDefinition definition = RecipeEntryDefinition.CODEC
                    .parse(new Dynamic<>(JsonOps.INSTANCE, element))
                    .getOrThrow();

                result.add(new RecipeEntryDefinition(
                    entryId,
                    definition.recipeTypes(),
                    definition.special(),
                    definition.templateKind(),
                    definition.showRecipeTypesInAdvanced()
                ));
            } catch (Exception e) {
                LOGGER.error("Failed to load recipe entry from {} in pack {}", location, entry.getValue().sourcePackId(), e);
            }
        }

        return List.copyOf(result);
    }

    private void apply(List<RecipeEntryDefinition> data) {
        entries = data;
        LOGGER.info("Loaded {} studio recipe entries", entries.size());
    }
}
