package fr.hardel.asset_editor.tag;

import net.minecraft.core.HolderSet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TagHelper {

    public static <T> Identifier firstIdentifier(@org.jspecify.annotations.Nullable HolderSet<T> holderSet,
        Function<TagKey<T>, Optional<HolderSet<T>>> tagResolver) {
        if (holderSet == null)
            return null;

        Identifier direct = holderSet.stream()
            .map(holder -> holder.unwrapKey().map(ResourceKey::identifier).orElse(null))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

        if (direct != null)
            return direct;

        return holderSet.unwrapKey()
            .flatMap(tagResolver)
            .flatMap(resolved -> resolved.stream()
                .map(holder -> holder.unwrapKey().map(ResourceKey::identifier).orElse(null))
                .filter(Objects::nonNull)
                .findFirst())
            .orElse(null);
    }

    public static Set<Identifier> filterByPathPrefix(Set<Identifier> tags, String prefix) {
        return tags.stream()
            .filter(tagId -> tagId.getPath().startsWith(prefix))
            .collect(Collectors.toUnmodifiableSet());
    }

    private TagHelper() {}
}
