package fr.hardel.asset_editor.store.workspace;

import fr.hardel.asset_editor.network.workspace.RegistryWorkspaceBinding;
import fr.hardel.asset_editor.store.ElementEntry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;

import java.nio.file.Path;
import java.util.HashMap;
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

    public <T> void snapshotBaseline(RegistryWorkspaceBinding<T> binding,
        ResourceManager resourceManager,
        Registry<T> registry,
        HolderLookup.Provider registries) {
        baselines.put(binding.registryName(), baselineMap(registryReader.readBaseline(binding, resourceManager, registry, registries)));
        workspaces.values().forEach(perRegistry -> perRegistry.remove(binding.registryName()));
    }

    public <T> ElementEntry<T> get(String packId, RegistryWorkspaceBinding<T> binding,
        Path packRoot, HolderLookup.Provider registries, Identifier elementId) {
        return workspace(packId, binding, packRoot, registries).get(elementId);
    }

    public <T> void put(String packId, RegistryWorkspaceBinding<T> binding,
        Path packRoot, HolderLookup.Provider registries,
        Identifier elementId, ElementEntry<T> entry) {
        workspace(packId, binding, packRoot, registries).put(elementId, entry);
    }

    public <T> List<ElementEntry<T>> snapshotWorkspace(String packId, RegistryWorkspaceBinding<T> binding,
        Path packRoot, HolderLookup.Provider registries) {
        return List.copyOf(workspace(packId, binding, packRoot, registries).entries());
    }

    public <T> void flushDirty(Path packRoot, String packId, RegistryWorkspaceBinding<T> binding,
        HolderLookup.Provider registries) {
        RegistryWorkspace<T> workspace = workspace(packId, binding, packRoot, registries);
        if (workspace.dirty().isEmpty())
            return;

        RegistryDiffPlan<T> plan = diffPlanner.plan(packRoot, binding, workspace);
        if (!plan.isEmpty())
            diskWriter.write(plan, binding.codec(), registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE));
        workspace.clearDirty();
    }

    public void clearAll() {
        baselines.clear();
        workspaces.clear();
    }

    @SuppressWarnings("unchecked")
    private <T> RegistryWorkspace<T> workspace(String packId, RegistryWorkspaceBinding<T> binding,
        Path packRoot, HolderLookup.Provider registries) {
        Map<String, RegistryWorkspace<?>> registryWorkspaces = workspaces.computeIfAbsent(packId, ignored -> new HashMap<>());
        return (RegistryWorkspace<T>) registryWorkspaces.computeIfAbsent(binding.registryName(),
            ignored -> packOverlayLoader.loadWorkspace(packId, packRoot, binding, registries, baselineEntries(binding)));
    }

    @SuppressWarnings("unchecked")
    private <T> Map<Identifier, ElementEntry<T>> baselineEntries(RegistryWorkspaceBinding<T> binding) {
        return (Map<Identifier, ElementEntry<T>>) (Map<?, ?>) baselines.getOrDefault(binding.registryName(), Map.of());
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<Identifier, ElementEntry<?>> baselineMap(Map<Identifier, ElementEntry<T>> entries) {
        return (Map<Identifier, ElementEntry<?>>) (Map<?, ?>) entries;
    }
}
