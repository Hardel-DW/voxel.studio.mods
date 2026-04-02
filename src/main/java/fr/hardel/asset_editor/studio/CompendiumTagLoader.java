package fr.hardel.asset_editor.studio;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

public final class CompendiumTagLoader implements PreparableReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompendiumTagLoader.class);

    private static final FileToIdConverter ITEM_LISTER = FileToIdConverter.json("studio/compendium/item");
    private static final FileToIdConverter ENCHANTMENT_LISTER = FileToIdConverter.json("studio/compendium/enchantment");

    private static volatile List<CompendiumTagGroup> itemGroups = List.of();
    private static volatile List<CompendiumTagGroup> enchantmentGroups = List.of();

    public static List<CompendiumTagGroup> itemGroups() {
        return itemGroups;
    }

    public static List<CompendiumTagGroup> enchantmentGroups() {
        return enchantmentGroups;
    }

    @Override
    public CompletableFuture<Void> reload(SharedState sharedState, Executor prepExecutor, PreparationBarrier barrier, Executor applyExecutor) {
        ResourceManager manager = sharedState.resourceManager();
        return CompletableFuture.supplyAsync(() -> prepare(manager), prepExecutor)
            .thenCompose(barrier::wait)
            .thenAcceptAsync(this::apply, applyExecutor);
    }

    private PreparedData prepare(ResourceManager manager) {
        List<CompendiumTagGroup> items = loadMerged(manager, ITEM_LISTER);
        List<CompendiumTagGroup> enchantments = loadMerged(manager, ENCHANTMENT_LISTER);
        return new PreparedData(items, enchantments);
    }

    private void apply(PreparedData data) {
        itemGroups = data.itemGroups();
        enchantmentGroups = data.enchantmentGroups();
        LOGGER.info("Loaded {} compendium item groups, {} compendium enchantment groups", itemGroups.size(), enchantmentGroups.size());
    }

    private List<CompendiumTagGroup> loadMerged(ResourceManager manager, FileToIdConverter lister) {
        List<CompendiumTagGroup> result = new ArrayList<>();

        for (Map.Entry<Identifier, List<Resource>> entry : lister.listMatchingResourceStacks(manager).entrySet()) {
            Identifier location = entry.getKey();
            Identifier groupId = lister.fileToId(location);
            List<CompendiumTagEntry> accumulated = new ArrayList<>();

            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonElement element = StrictJsonParser.parse(reader);
                    CompendiumTagFile file = CompendiumTagFile.CODEC.parse(new Dynamic<>(JsonOps.INSTANCE, element)).getOrThrow();
                    if (file.replace()) {
                        accumulated.clear();
                    }
                    accumulated.addAll(file.values());
                } catch (Exception e) {
                    LOGGER.error("Failed to load compendium tags from {} in pack {}", location, resource.sourcePackId(), e);
                }
            }

            result.add(new CompendiumTagGroup(groupId, List.copyOf(accumulated)));
        }

        return List.copyOf(result);
    }

    private record PreparedData(List<CompendiumTagGroup> itemGroups, List<CompendiumTagGroup> enchantmentGroups) {}

    private record CompendiumTagFile(List<CompendiumTagEntry> values, boolean replace) {
        static final Codec<CompendiumTagFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CompendiumTagEntry.CODEC.listOf().fieldOf("values").forGetter(CompendiumTagFile::values),
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(CompendiumTagFile::replace)
        ).apply(instance, CompendiumTagFile::new));
    }
}
