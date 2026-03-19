package fr.hardel.asset_editor.store.workspace;

import fr.hardel.asset_editor.store.ElementEntry;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class RegistryWorkspace<T> {

    private final Map<Identifier, ElementEntry<T>> referenceEntries;
    private final Map<Identifier, ElementEntry<T>> currentEntries;
    private final Set<Identifier> dirty = new java.util.LinkedHashSet<>();

    public RegistryWorkspace(Map<Identifier, ElementEntry<T>> referenceEntries,
        Map<Identifier, ElementEntry<T>> currentEntries) {
        this.referenceEntries = new LinkedHashMap<>(referenceEntries);
        this.currentEntries = new LinkedHashMap<>(currentEntries);
    }

    public ElementEntry<T> get(Identifier id) {
        return currentEntries.get(id);
    }

    public void put(Identifier id, ElementEntry<T> entry) {
        currentEntries.put(id, entry);
        dirty.add(id);
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

    public Set<Identifier> dirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty.clear();
    }
}
