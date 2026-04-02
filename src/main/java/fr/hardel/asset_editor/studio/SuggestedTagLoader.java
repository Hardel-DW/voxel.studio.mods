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

public final class SuggestedTagLoader implements PreparableReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuggestedTagLoader.class);

    private static final FileToIdConverter ITEM_LISTER = FileToIdConverter.json("studio/suggested/item");
    private static final FileToIdConverter ENCHANTMENT_LISTER = FileToIdConverter.json("studio/suggested/enchantment");

    private static volatile List<SuggestedTagGroup> itemGroups = List.of();
    private static volatile List<SuggestedTagGroup> enchantmentGroups = List.of();

    public static List<SuggestedTagGroup> itemGroups() {
        return itemGroups;
    }

    public static List<SuggestedTagGroup> enchantmentGroups() {
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
        List<SuggestedTagGroup> items = loadMerged(manager, ITEM_LISTER);
        List<SuggestedTagGroup> enchantments = loadMerged(manager, ENCHANTMENT_LISTER);
        return new PreparedData(items, enchantments);
    }

    private void apply(PreparedData data) {
        itemGroups = data.itemGroups();
        enchantmentGroups = data.enchantmentGroups();
        LOGGER.info("Loaded {} suggested item groups, {} suggested enchantment groups", itemGroups.size(), enchantmentGroups.size());
    }

    private List<SuggestedTagGroup> loadMerged(ResourceManager manager, FileToIdConverter lister) {
        List<SuggestedTagGroup> result = new ArrayList<>();

        for (Map.Entry<Identifier, List<Resource>> entry : lister.listMatchingResourceStacks(manager).entrySet()) {
            Identifier location = entry.getKey();
            Identifier groupId = lister.fileToId(location);
            List<SuggestedTagEntry> accumulated = new ArrayList<>();

            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonElement element = StrictJsonParser.parse(reader);
                    SuggestedTagFile file = SuggestedTagFile.CODEC.parse(new Dynamic<>(JsonOps.INSTANCE, element)).getOrThrow();
                    if (file.replace()) {
                        accumulated.clear();
                    }
                    accumulated.addAll(file.values());
                } catch (Exception e) {
                    LOGGER.error("Failed to load suggested tags from {} in pack {}", location, resource.sourcePackId(), e);
                }
            }

            result.add(new SuggestedTagGroup(groupId, List.copyOf(accumulated)));
        }

        return List.copyOf(result);
    }

    private record PreparedData(List<SuggestedTagGroup> itemGroups, List<SuggestedTagGroup> enchantmentGroups) {}

    private record SuggestedTagFile(List<SuggestedTagEntry> values, boolean replace) {
        static final Codec<SuggestedTagFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SuggestedTagEntry.CODEC.listOf().fieldOf("values").forGetter(SuggestedTagFile::values),
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(SuggestedTagFile::replace)
        ).apply(instance, SuggestedTagFile::new));
    }
}
