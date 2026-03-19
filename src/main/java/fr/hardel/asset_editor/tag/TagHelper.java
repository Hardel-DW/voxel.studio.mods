package fr.hardel.asset_editor.tag;

import net.minecraft.core.HolderSet;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TagHelper {

    public static <T> Identifier firstIdentifier(Optional<HolderSet<T>> holderSet,
        Function<TagKey<T>, Optional<HolderSet<T>>> tagResolver) {
        return holderSet.flatMap(set -> set.stream()
                .map(holder -> holder.unwrapKey().map(key -> key.identifier()).orElse(null))
                .filter(Objects::nonNull)
                .findFirst())
            .orElseGet(() -> holderSet.flatMap(HolderSet::unwrapKey)
                .flatMap(tagResolver)
                .flatMap(resolved -> resolved.stream()
                    .map(holder -> holder.unwrapKey().map(key -> key.identifier()).orElse(null))
                    .filter(Objects::nonNull)
                    .findFirst())
                .orElse(null));
    }

    public static Set<Identifier> filterByPathPrefix(Set<Identifier> tags, String prefix) {
        return tags.stream()
            .filter(tagId -> tagId.getPath().startsWith(prefix))
            .collect(Collectors.toUnmodifiableSet());
    }

    private TagHelper() {}
}
