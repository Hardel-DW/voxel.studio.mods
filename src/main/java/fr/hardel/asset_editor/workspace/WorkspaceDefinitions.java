package fr.hardel.asset_editor.workspace;

import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WorkspaceDefinitions {

    private static final Map<String, WorkspaceDefinition<?>> DEFINITIONS = new ConcurrentHashMap<>();

    public static void register(WorkspaceDefinition<?> definition) {
        WorkspaceDefinition<?> previous = DEFINITIONS.putIfAbsent(definition.registryId().toString(), definition);
        if (previous != null)
            throw new IllegalStateException("Duplicate workspace definition: " + definition.registryId());
    }

    public static WorkspaceDefinition<?> get(Identifier registryId) {
        if (registryId == null)
            return null;
        return DEFINITIONS.get(registryId.toString());
    }

    public static boolean contains(WorkspaceDefinition<?> definition) {
        return definition != null && DEFINITIONS.get(definition.registryId().toString()) == definition;
    }

    public static Collection<WorkspaceDefinition<?>> all() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    public static void clearAllState() {
        DEFINITIONS.values().forEach(WorkspaceDefinition::clearState);
    }

    private WorkspaceDefinitions() {}
}
