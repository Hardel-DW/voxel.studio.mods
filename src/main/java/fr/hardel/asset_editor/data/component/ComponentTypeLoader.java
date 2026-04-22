package fr.hardel.asset_editor.data.component;

import com.google.gson.JsonElement;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class ComponentTypeLoader implements PreparableReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentTypeLoader.class);
    private static final FileToIdConverter LISTER = FileToIdConverter.json("components_types");

    private static volatile Map<Identifier, StudioComponentTypeDef> definitions = Map.of();

    public static Map<Identifier, StudioComponentTypeDef> definitions() {
        return definitions;
    }

    public static StudioComponentTypeDef get(Identifier id) {
        return definitions.get(id);
    }

    @Override
    public @NonNull CompletableFuture<Void> reload(SharedState sharedState, @NonNull Executor prepExecutor, PreparationBarrier barrier, @NonNull Executor applyExecutor) {
        ResourceManager manager = sharedState.resourceManager();
        return CompletableFuture.supplyAsync(() -> prepare(manager), prepExecutor)
            .thenCompose(barrier::wait)
            .thenAcceptAsync(this::apply, applyExecutor);
    }

    private Map<Identifier, StudioComponentTypeDef> prepare(ResourceManager manager) {
        Map<Identifier, StudioComponentTypeDef> result = new LinkedHashMap<>();

        for (Map.Entry<Identifier, Resource> entry : LISTER.listMatchingResources(manager).entrySet()) {
            Identifier location = entry.getKey();
            Identifier componentId = LISTER.fileToId(location);

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement element = StrictJsonParser.parse(reader);
                ComponentWidget widget = ComponentWidget.CODEC
                    .parse(new Dynamic<>(JsonOps.INSTANCE, element))
                    .getOrThrow();

                result.put(componentId, new StudioComponentTypeDef(componentId, widget));
            } catch (Exception e) {
                LOGGER.error("Failed to load component type from {} in pack {}", location, entry.getValue().sourcePackId(), e);
            }
        }

        return Map.copyOf(result);
    }

    private void apply(Map<Identifier, StudioComponentTypeDef> data) {
        definitions = data;
        LOGGER.info("Loaded {} studio component type definitions", definitions.size());
    }
}
