package fr.hardel.asset_editor.workspace;

import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class WorkspaceRegistry<T> {

    private final Map<Identifier, ElementEntry<T>> referenceEntries;
    private final Map<Identifier, ElementEntry<T>> currentEntries;
    private final Set<Identifier> dirty = new java.util.LinkedHashSet<>();
    private final Set<Identifier> dirtyTags = new java.util.LinkedHashSet<>();

    public WorkspaceRegistry(Map<Identifier, ElementEntry<T>> referenceEntries,
        Map<Identifier, ElementEntry<T>> currentEntries) {
        this.referenceEntries = new LinkedHashMap<>(referenceEntries);
        this.currentEntries = new LinkedHashMap<>(currentEntries);
    }

    public ElementEntry<T> get(Identifier id) {
        return currentEntries.get(id);
    }

    public void put(Identifier id, ElementEntry<T> entry) {
        ElementEntry<T> previous = currentEntries.get(id);
        currentEntries.put(id, entry);
        dirty.add(id);
        if (previous != null)
            dirtyTags.addAll(previous.tags());

        dirtyTags.addAll(entry.tags());
    }

    public Collection<ElementEntry<T>> entries() {
        return currentEntries.values();
    }

    public Map<Identifier, ElementEntry<T>> referenceEntries() {
        return referenceEntries;
    }

    public Map<Identifier, ElementEntry<T>> currentEntries() {
        return currentEntries;
    }

    public Set<Identifier> modifiedIds() {
        LinkedHashSet<Identifier> result = new LinkedHashSet<>();
        for (Map.Entry<Identifier, ElementEntry<T>> entry : currentEntries.entrySet()) {
            if (!Objects.equals(referenceEntries.get(entry.getKey()), entry.getValue()))
                result.add(entry.getKey());
        }
        return result;
    }

    public boolean isModifiedVsReference(Identifier id) {
        ElementEntry<T> current = currentEntries.get(id);
        if (current == null)
            return false;

        return !Objects.equals(referenceEntries.get(id), current);
    }

    public Set<Identifier> dirty() {
        return dirty;
    }

    public Set<Identifier> dirtyTags() {
        return dirtyTags;
    }

    public void clearDirty() {
        dirty.clear();
        dirtyTags.clear();
    }
}
