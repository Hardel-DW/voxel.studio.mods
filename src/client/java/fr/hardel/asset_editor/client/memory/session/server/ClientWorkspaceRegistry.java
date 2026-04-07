package fr.hardel.asset_editor.client.memory.session.server;

import fr.hardel.asset_editor.client.memory.core.ReadableMemory;
import fr.hardel.asset_editor.client.memory.core.SimpleMemory;
import fr.hardel.asset_editor.workspace.WorkspaceDefinition;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientWorkspaceRegistry<T> {

    private final WorkspaceDefinition<T> definition;
    private final SimpleMemory<Map<Identifier, ElementEntry<T>>> memory = new SimpleMemory<>(Map.of());

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

    Map<Identifier, ElementEntry<?>> wildcardSnapshot() {
        LinkedHashMap<Identifier, ElementEntry<?>> result = new LinkedHashMap<>(memory.snapshot());
        return Collections.unmodifiableMap(result);
    }

    void publish(Map<Identifier, ElementEntry<T>> entries) {
        memory.setSnapshot(entries);
    }

    void clear() {
        memory.setSnapshot(Map.of());
    }
}
