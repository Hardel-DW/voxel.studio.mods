package fr.hardel.asset_editor.workspace.definition;

import fr.hardel.asset_editor.workspace.action.EditorActionRegistry;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WorkspaceDefinitions {

    private static final Map<String, WorkspaceDefinition<?>> DEFINITIONS = new ConcurrentHashMap<>();

    public static <T> void register(WorkspaceDefinition<T> definition) {
        if (definition == null)
            throw new IllegalArgumentException("Workspace definition cannot be null");

        WorkspaceDefinition<?> previous = DEFINITIONS.putIfAbsent(definition.registryId().toString(), definition);
        if (previous != null)
            throw new IllegalStateException("Duplicate workspace definition: " + definition.registryId());

        for (var actionType : definition.actionTypes()) {
            EditorActionRegistry.register(actionType);
        }
    }

    public static WorkspaceDefinition<?> get(Identifier registryId) {
        if (registryId == null)
            return null;
        return DEFINITIONS.get(registryId.toString());
    }

    public static Collection<WorkspaceDefinition<?>> all() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    private WorkspaceDefinitions() {}
}
