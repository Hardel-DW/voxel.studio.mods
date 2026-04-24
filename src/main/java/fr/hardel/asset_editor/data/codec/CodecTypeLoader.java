package fr.hardel.asset_editor.data.codec;

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

public final class CodecTypeLoader implements PreparableReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodecTypeLoader.class);
    private static final FileToIdConverter LISTER = FileToIdConverter.json("codec_types");

    private static volatile Map<Identifier, StudioCodecTypeDef> definitions = Map.of();

    public static Map<Identifier, StudioCodecTypeDef> definitions() {
        return definitions;
    }

    public static StudioCodecTypeDef get(Identifier id) {
        return definitions.get(id);
    }

    public static Map<Identifier, CodecWidget> loadWidgets(ResourceManager manager) {
        Map<Identifier, CodecWidget> raw = loadRawWidgets(manager);
        Map<Identifier, CodecWidget> resolved = new LinkedHashMap<>();
        raw.forEach((id, widget) -> {
            try {
                resolved.put(id, CodecWidgetResolver.resolve(widget, raw));
            } catch (Exception e) {
                LOGGER.error("Failed to resolve codec type {}", id, e);
            }
        });
        return Map.copyOf(resolved);
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
        loadWidgets(manager).forEach((id, widget) -> result.put(id, new StudioCodecTypeDef(id, widget)));
        return Map.copyOf(result);
    }

    private static Map<Identifier, CodecWidget> loadRawWidgets(ResourceManager manager) {
        Map<Identifier, CodecWidget> result = new LinkedHashMap<>();

        for (Map.Entry<Identifier, Resource> entry : LISTER.listMatchingResources(manager).entrySet()) {
            Identifier location = entry.getKey();
            Identifier codecId = LISTER.fileToId(location);

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement element = StrictJsonParser.parse(reader);
                CodecWidget widget = CodecWidget.CODEC
                    .parse(new Dynamic<>(JsonOps.INSTANCE, element))
                    .getOrThrow();

                result.put(codecId, widget);
            } catch (Exception e) {
                LOGGER.error("Failed to load codec type from {} in pack {}", location, entry.getValue().sourcePackId(), e);
            }
        }

        return Map.copyOf(result);
    }

    private void apply(Map<Identifier, StudioCodecTypeDef> data) {
        definitions = data;
        LOGGER.info("Loaded {} studio codec type definitions", definitions.size());
    }
}
