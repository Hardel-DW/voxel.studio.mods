package fr.hardel.asset_editor.workspace;

import fr.hardel.asset_editor.workspace.definition.WorkspaceDefinition;
import fr.hardel.asset_editor.workspace.io.*;
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
    private final OverlayManager overlayManager;
    private final DiffPlanner diffPlanner = new DiffPlanner();
    private final DiskWriter diskWriter = new DiskWriter();

    private final Map<String, Map<Identifier, ElementEntry<?>>> baselines = new HashMap<>();
    private final Map<String, Map<String, RegistryWorkspace<?>>> workspaces = new HashMap<>();

    private WorkspaceRepository(MinecraftServer server) {
        this.overlayManager = new OverlayManager(server);
    }

    public <T> void snapshotBaseline(WorkspaceDefinition<T> definition,
        ResourceManager resourceManager,
        @org.jspecify.annotations.Nullable Registry<T> registry,
        HolderLookup.Provider registries) {
        var baseline = registryReader.readBaseline(definition, resourceManager, registry, registries);
        baselines.put(definition.registryName(), new LinkedHashMap<>(baseline));
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

    public <T> List<ElementEntry<T>> snapshotWorkspace(String packId, WorkspaceDefinition<T> definition, Path packRoot, HolderLookup.Provider registries) {
        return List.copyOf(workspace(packId, definition, packRoot, registries).entries());
    }

    public <T> void flushDirty(Path packRoot, String packId, WorkspaceDefinition<T> definition,
        HolderLookup.Provider registries) {
        RegistryWorkspace<T> workspace = workspace(packId, definition, packRoot, registries);
        if (workspace.dirty().isEmpty())
            return;

        RegistryDiffPlan<T> plan = diffPlanner.plan(packRoot, definition, workspace, registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE));
        if (!plan.isEmpty())
            diskWriter.write(plan, definition.codec(), registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE));

        workspace.clearDirty();
    }

    private <T> RegistryWorkspace<T> workspace(String packId, WorkspaceDefinition<T> definition,
        Path packRoot, HolderLookup.Provider registries) {
        Map<String, RegistryWorkspace<?>> registryWorkspaces = workspaces.computeIfAbsent(packId, ignored -> new HashMap<>());
        String name = definition.registryName();
        if (!registryWorkspaces.containsKey(name)) {
            Map<Identifier, ElementEntry<T>> baselineEntries = TypeSafeWorkspaceMap.narrowEntries(baselines.getOrDefault(definition.registryName(), Map.of()));
            registryWorkspaces.put(name, overlayManager.loadWorkspace(packId, packRoot, definition, registries, baselineEntries));
        }
        return TypeSafeWorkspaceMap.narrow(registryWorkspaces.get(name));
    }

    /**
     * Type-safe heterogeneous container casts. Safe because workspaces/baselines are keyed by registry name and populated via typed methods that receive
     * WorkspaceDefinition&lt;T&gt;.
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
