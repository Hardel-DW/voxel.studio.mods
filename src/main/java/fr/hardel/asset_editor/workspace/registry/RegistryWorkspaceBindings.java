package fr.hardel.asset_editor.workspace.registry;

import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RegistryWorkspaceBindings {

    private static final Map<String, RegistryWorkspaceBinding<?>> BINDINGS = new ConcurrentHashMap<>();

    public static <T> void register(RegistryWorkspaceBinding<T> binding) {
        if (binding == null)
            return;

        BINDINGS.put(binding.registryId().toString(), binding);
    }

    @SuppressWarnings("unchecked")
    public static <T> RegistryWorkspaceBinding<T> get(Identifier registryId) {
        if (registryId == null)
            return null;
        return (RegistryWorkspaceBinding<T>) BINDINGS.get(registryId.toString());
    }

    public static Collection<RegistryWorkspaceBinding<?>> all() {
        return Collections.unmodifiableCollection(BINDINGS.values());
    }

    private RegistryWorkspaceBindings() {}
}
