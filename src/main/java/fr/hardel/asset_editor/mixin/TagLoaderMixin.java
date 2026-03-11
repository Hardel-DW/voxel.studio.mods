package fr.hardel.asset_editor.mixin;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.tag.ExtendedTagFile;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.StrictJsonParser;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SequencedSet;
import java.util.WeakHashMap;

@Mixin(TagLoader.class)
public abstract class TagLoaderMixin<T> {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private String directory;

    @Unique
    private static final Map<TagLoader.EntryWithSource, Boolean> ASSET_EDITOR_REMOVAL_MARKERS = Collections
            .synchronizedMap(new WeakHashMap<>());

    @Unique
    private static void assetEditor$markAsRemoved(TagLoader.EntryWithSource entry) {
        ASSET_EDITOR_REMOVAL_MARKERS.put(entry, Boolean.TRUE);
    }

    @Unique
    private static boolean assetEditor$isRemoved(TagLoader.EntryWithSource entry) {
        return ASSET_EDITOR_REMOVAL_MARKERS.containsKey(entry);
    }

    /**
     * @author Hardel
     * @reason Extend vanilla tags with an optional "exclude" array.
     */
    @Overwrite
    public Map<Identifier, List<TagLoader.EntryWithSource>> load(ResourceManager resourceManager) {
        Map<Identifier, List<TagLoader.EntryWithSource>> builders = new HashMap<>();
        FileToIdConverter lister = FileToIdConverter.json(this.directory);

        for (Entry<Identifier, List<Resource>> entry : lister.listMatchingResourceStacks(resourceManager).entrySet()) {
            Identifier location = entry.getKey();
            Identifier id = lister.fileToId(location);

            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonElement element = StrictJsonParser.parse(reader);
                    List<TagLoader.EntryWithSource> tagContents = builders.computeIfAbsent(id,
                            key -> new ArrayList<>());
                    ExtendedTagFile parsedContents = ExtendedTagFile.CODEC
                            .parse(new Dynamic<>(JsonOps.INSTANCE, element)).getOrThrow();
                    if (parsedContents.replace()) {
                        tagContents.clear();
                    }

                    String sourceId = resource.sourcePackId();
                    for (TagEntry excludedEntry : parsedContents.exclude()) {
                        TagLoader.EntryWithSource withSource = new TagLoader.EntryWithSource(excludedEntry, sourceId);
                        assetEditor$markAsRemoved(withSource);
                        tagContents.add(withSource);
                    }

                    parsedContents.entries().forEach(
                            addedEntry -> tagContents.add(new TagLoader.EntryWithSource(addedEntry, sourceId)));
                } catch (Exception exception) {
                    LOGGER.error("Couldn't read tag list {} from {} in data pack {}", id, location,
                            resource.sourcePackId(), exception);
                }
            }
        }

        return builders;
    }

    /**
     * @author Hardel
     * @reason Apply entries from "exclude" by removing resolved values from the
     *         accumulator.
     */
    @Overwrite
    private Either<List<TagLoader.EntryWithSource>, List<T>> tryBuildTag(
            TagEntry.Lookup<T> lookup,
            List<TagLoader.EntryWithSource> entries) {
        SequencedSet<T> values = new LinkedHashSet<>();
        List<TagLoader.EntryWithSource> missingElements = new ArrayList<>();

        for (TagLoader.EntryWithSource entry : entries) {
            if (assetEditor$isRemoved(entry)) {
                entry.entry().build(lookup, values::remove);
            } else if (!entry.entry().build(lookup, values::add)) {
                missingElements.add(entry);
            }
        }

        return missingElements.isEmpty() ? Either.right(List.copyOf(values)) : Either.left(missingElements);
    }
}
