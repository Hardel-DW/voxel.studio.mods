package fr.hardel.asset_editor.client.memory.core;

import fr.hardel.asset_editor.workspace.WorkspaceDefinition;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class ClientWorkspaceRegistry<T> {

    private final WorkspaceDefinition<T> definition;
    private final SimpleMemory<Map<Identifier, ElementEntry<T>>> memory = new SimpleMemory<>(Map.of());
    private final SimpleMemory<Set<Identifier>> modifiedMemory = new SimpleMemory<>(Set.of());

    private ClientWorkspaceRegistry(WorkspaceDefinition<T> definition) {
        this.definition = definition;
    }

    public static <T> ClientWorkspaceRegistry<T> of(WorkspaceDefinition<T> definition) {
        return new ClientWorkspaceRegistry<>(definition);
    }

    public WorkspaceDefinition<T> definition() {
        return definition;
    }

    public ResourceKey<Registry<T>> registryKey() {
        return definition.registryKey();
    }

    public Identifier registryId() {
        return definition.registryId();
    }

    public ReadableMemory<Map<Identifier, ElementEntry<T>>> memory() {
        return memory;
    }

    public Map<Identifier, ElementEntry<T>> snapshot() {
        return memory.snapshot();
    }

    public Map<Identifier, ElementEntry<?>> wildcardSnapshot() {
        LinkedHashMap<Identifier, ElementEntry<?>> result = new LinkedHashMap<>(memory.snapshot());
        return Collections.unmodifiableMap(result);
    }

    public void publish(Map<Identifier, ElementEntry<T>> entries) {
        memory.setSnapshot(entries);
    }

    public ReadableMemory<Set<Identifier>> modifiedIdsMemory() {
        return modifiedMemory;
    }

    public Set<Identifier> modifiedIdsSnapshot() {
        return modifiedMemory.snapshot();
    }

    public void publishModifiedIds(Set<Identifier> ids) {
        modifiedMemory.setSnapshot(Set.copyOf(ids == null ? Set.of() : ids));
    }

    public void markModified(Identifier id, boolean modified) {
        LinkedHashSet<Identifier> next = new LinkedHashSet<>(modifiedMemory.snapshot());
        if (modified ? next.add(id) : next.remove(id))
            modifiedMemory.setSnapshot(Set.copyOf(next));
    }

    public void clear() {
        memory.setSnapshot(Map.of());
        modifiedMemory.setSnapshot(Set.of());
    }
}
