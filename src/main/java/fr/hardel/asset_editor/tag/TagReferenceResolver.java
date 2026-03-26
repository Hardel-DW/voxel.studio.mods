package fr.hardel.asset_editor.tag;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.PlaceholderLookupProvider;

import java.util.Optional;

public final class TagReferenceResolver {

    public <T> HolderSet<T> resolve(ResourceKey<Registry<T>> registryKey, Identifier tagId,
        HolderLookup.Provider registries) {
        if (registryKey == null || tagId == null || registries == null)
            return null;

        var lookup = registries.lookupOrThrow(registryKey);
        var tag = TagKey.create(registryKey, tagId);
        try {
            Optional<HolderSet.Named<T>> resolved = lookup.get(tag);
            return resolved.<HolderSet<T>>map(named -> named).orElse(null);
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    public <T> HolderSet<T> resolveOrPlaceholder(ResourceKey<Registry<T>> registryKey, Identifier tagId,
        HolderLookup.Provider registries) {
        if (registryKey == null || tagId == null || registries == null)
            return null;

        HolderSet<T> resolved = resolve(registryKey, tagId, registries);
        if (resolved != null)
            return resolved;

        PlaceholderLookupProvider placeholders = new PlaceholderLookupProvider(registries);
        return placeholders.lookup(registryKey)
            .flatMap(lookup -> lookup.get(TagKey.create(registryKey, tagId)))
            .orElse(null);
    }
}
