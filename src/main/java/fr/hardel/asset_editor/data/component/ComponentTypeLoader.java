package fr.hardel.asset_editor.data.component;

import com.google.gson.JsonElement;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.data.codec.CodecTypeLoader;
import fr.hardel.asset_editor.data.codec.CodecWidget;
import fr.hardel.asset_editor.data.codec.CodecWidgetResolver;
import fr.hardel.asset_editor.data.codec.StudioCodecTypeDef;
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
    private static final FileToIdConverter LISTER = FileToIdConverter.json("codec/components");

    private static volatile Map<Identifier, StudioCodecTypeDef> definitions = Map.of();

    public static Map<Identifier, StudioCodecTypeDef> definitions() {
        return definitions;
    }

    public static StudioCodecTypeDef get(Identifier id) {
        return definitions.get(id);
    }

    @Override
    public @NonNull CompletableFuture<Void> reload(SharedState sharedState, @NonNull Executor prepExecutor, PreparationBarrier barrier, @NonNull Executor applyExecutor) {
        ResourceManager manager = sharedState.resourceManager();
        return CompletableFuture.supplyAsync(() -> prepare(manager), prepExecutor)
            .thenCompose(barrier::wait)
            .thenAcceptAsync(this::apply, applyExecutor);
    }

    private Map<Identifier, StudioCodecTypeDef> prepare(ResourceManager manager) {
        Map<Identifier, StudioCodecTypeDef> result = new LinkedHashMap<>();
        Map<Identifier, CodecWidget> codecTypes = CodecTypeLoader.loadWidgets(manager);

        for (Map.Entry<Identifier, Resource> entry : LISTER.listMatchingResources(manager).entrySet()) {
            Identifier location = entry.getKey();
            Identifier componentId = LISTER.fileToId(location);

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement element = StrictJsonParser.parse(reader);
                CodecWidget widget = CodecWidget.CODEC
                    .parse(new Dynamic<>(JsonOps.INSTANCE, element))
                    .getOrThrow();
                widget = CodecWidgetResolver.resolve(widget, codecTypes);

                result.put(componentId, new StudioCodecTypeDef(componentId, widget));
            } catch (Exception e) {
                LOGGER.error("Failed to load component type from {} in pack {}", location, entry.getValue().sourcePackId(), e);
            }
        }

        return Map.copyOf(result);
    }

    private void apply(Map<Identifier, StudioCodecTypeDef> data) {
        definitions = data;
        LOGGER.info("Loaded {} studio component type definitions", definitions.size());
    }
}
