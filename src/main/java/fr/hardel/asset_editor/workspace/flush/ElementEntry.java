package fr.hardel.asset_editor.workspace.flush;

import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

public record ElementEntry<T>(Identifier id, T data, Set<Identifier> tags, CustomFields custom) {

    public ElementEntry<T> withData(T newData) {
        return new ElementEntry<>(id, newData, tags, custom);
    }

    public ElementEntry<T> withTags(Set<Identifier> newTags) {
        return new ElementEntry<>(id, data, Set.copyOf(newTags), custom);
    }

    public ElementEntry<T> withCustom(CustomFields newCustom) {
        return new ElementEntry<>(id, data, tags, newCustom);
    }

    public ElementEntry<T> toggleTag(Identifier tagId) {
        var copy = new HashSet<>(tags);
        if (!copy.remove(tagId))
            copy.add(tagId);
        return new ElementEntry<>(id, data, Set.copyOf(copy), custom);
    }
}
