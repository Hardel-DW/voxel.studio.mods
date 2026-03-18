package fr.hardel.asset_editor.store;

import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class PackWorkspaceStore<T> {

    private final Map<Identifier, ElementEntry<T>> entries;
    private final java.util.Set<Identifier> dirty = new java.util.LinkedHashSet<>();

    public PackWorkspaceStore(Map<Identifier, ElementEntry<T>> entries) {
        this.entries = new LinkedHashMap<>(entries);
    }

    public ElementEntry<T> get(Identifier id) {
        return entries.get(id);
    }

    public void put(Identifier id, ElementEntry<T> entry) {
        entries.put(id, entry);
        dirty.add(id);
    }

    public Collection<ElementEntry<T>> entries() {
        return entries.values();
    }

    public Map<Identifier, ElementEntry<T>> entryMap() {
        return entries;
    }

    public Set<Identifier> dirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty.clear();
    }
}
