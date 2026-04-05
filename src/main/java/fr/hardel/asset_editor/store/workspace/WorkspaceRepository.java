package fr.hardel.asset_editor.store.workspace;

import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.workspace.definition.WorkspaceDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorkspaceRepository {

    private static WorkspaceRepository instance;

    public static WorkspaceRepository get() {
        return instance;
    }

    public static void init(MinecraftServer server) {
        instance = new WorkspaceRepository(server);
    }

    public static void shutdown() {
        instance = null;
    }

    private final RegistryReader registryReader = new RegistryReader();
    private final PackOverlayLoader packOverlayLoader;
    private final DiffPlanner diffPlanner = new DiffPlanner();
    private final DiskWriter diskWriter = new DiskWriter();

    private final Map<String, Map<Identifier, ElementEntry<?>>> baselines = new HashMap<>();
    private final Map<String, Map<String, RegistryWorkspace<?>>> workspaces = new HashMap<>();

    private WorkspaceRepository(MinecraftServer server) {
        this.packOverlayLoader = new PackOverlayLoader(server);
    }

    public <T> void snapshotBaseline(WorkspaceDefinition<T> definition,
        ResourceManager resourceManager,
        @org.jspecify.annotations.Nullable Registry<T> registry,
        HolderLookup.Provider registries) {
        var baseline = registryReader.readBaseline(definition, resourceManager, registry, registries);
        baselines.put(definition.registryName(), wildcardMap(baseline));
        workspaces.values().forEach(perRegistry -> perRegistry.remove(definition.registryName()));
    }

    public <T> ElementEntry<T> get(String packId, WorkspaceDefinition<T> definition,
        Path packRoot, HolderLookup.Provider registries, Identifier elementId) {
        return workspace(packId, definition, packRoot, registries).get(elementId);
    }

    public <T> void put(String packId, WorkspaceDefinition<T> definition,
        Path packRoot, HolderLookup.Provider registries,
        Identifier elementId, ElementEntry<T> entry) {
        workspace(packId, definition, packRoot, registries).put(elementId, entry);
    }

    public <T> List<ElementEntry<T>> snapshotWorkspace(String packId, WorkspaceDefinition<T> definition,
        Path packRoot, HolderLookup.Provider registries) {
        return List.copyOf(workspace(packId, definition, packRoot, registries).entries());
    }

    public <T> void flushDirty(Path packRoot, String packId, WorkspaceDefinition<T> definition,
        HolderLookup.Provider registries) {
        RegistryWorkspace<T> workspace = workspace(packId, definition, packRoot, registries);
        if (workspace.dirty().isEmpty())
            return;

        RegistryDiffPlan<T> plan = diffPlanner.plan(
            packRoot,
            definition,
            workspace,
            registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE)
        );
        if (!plan.isEmpty())
            diskWriter.write(plan, definition.codec(), registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE));
        workspace.clearDirty();
    }

    public void clearAll() {
        baselines.clear();
        workspaces.clear();
    }

    private <T> RegistryWorkspace<T> workspace(String packId, WorkspaceDefinition<T> definition,
        Path packRoot, HolderLookup.Provider registries) {
        Map<String, RegistryWorkspace<?>> registryWorkspaces = workspaces.computeIfAbsent(packId, ignored -> new HashMap<>());
        String name = definition.registryName();
        if (!registryWorkspaces.containsKey(name)) {
            registryWorkspaces.put(name, packOverlayLoader.loadWorkspace(packId, packRoot, definition, registries, baselineEntries(definition)));
        }
        return TypeSafeWorkspaceMap.narrow(registryWorkspaces.get(name));
    }

    private <T> Map<Identifier, ElementEntry<T>> baselineEntries(WorkspaceDefinition<T> definition) {
        return TypeSafeWorkspaceMap.narrowEntries(baselines.getOrDefault(definition.registryName(), Map.of()));
    }

    private static <T> Map<Identifier, ElementEntry<?>> wildcardMap(Map<Identifier, ElementEntry<T>> entries) {
        LinkedHashMap<Identifier, ElementEntry<?>> result = new LinkedHashMap<>();
        entries.forEach(result::put);
        return result;
    }

    /**
     * Type-safe heterogeneous container casts.
     * Safe because workspaces/baselines are keyed by registry name and populated
     * via typed methods that receive WorkspaceDefinition&lt;T&gt;.
     */
    private static final class TypeSafeWorkspaceMap {
        @SuppressWarnings("unchecked")
        static <T> RegistryWorkspace<T> narrow(RegistryWorkspace<?> workspace) {
            return (RegistryWorkspace<T>) workspace;
        }

        @SuppressWarnings("unchecked")
        static <T> Map<Identifier, ElementEntry<T>> narrowEntries(Map<Identifier, ElementEntry<?>> entries) {
            return (Map<Identifier, ElementEntry<T>>) (Map<?, ?>) entries;
        }
    }
}
