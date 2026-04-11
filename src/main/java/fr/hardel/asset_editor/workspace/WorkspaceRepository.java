package fr.hardel.asset_editor.workspace;

import com.mojang.serialization.JsonOps;
import fr.hardel.asset_editor.workspace.flush.ElementEntry;
import fr.hardel.asset_editor.workspace.io.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;

import java.nio.file.Path;
import java.util.List;

public final class WorkspaceRepository {

    private static WorkspaceRepository instance;

    public static WorkspaceRepository get() {
        return instance;
    }

    public static void init(MinecraftServer server) {
        instance = new WorkspaceRepository(server);
    }

    public static void shutdown() {
        WorkspaceDefinitions.clearAllState();
        instance = null;
    }

    private final RegistryReader registryReader = new RegistryReader();
    private final OverlayManager overlayManager;
    private final DiffPlanner diffPlanner = new DiffPlanner();
    private final DiskWriter diskWriter = new DiskWriter();

    private WorkspaceRepository(MinecraftServer server) {
        this.overlayManager = new OverlayManager(server);
    }

    public void snapshotAllBaselines(ResourceManager resourceManager, RegistryAccess registryAccess) {
        for (var definition : WorkspaceDefinitions.all())
            snapshotBaseline(definition, resourceManager, registryAccess);
    }

    private <T> void snapshotBaseline(WorkspaceDefinition<T> definition,
        ResourceManager resourceManager,
        RegistryAccess registryAccess) {
        Registry<T> registry = registryAccess.lookup(definition.registryKey()).orElse(null);
        var baseline = registryReader.readBaseline(definition, resourceManager, registry, registryAccess);
        definition.setBaseline(baseline);
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
        WorkspaceRegistry<T> workspace = workspace(packId, definition, packRoot, registries);
        if (workspace.dirty().isEmpty())
            return;

        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        RegistryDiffPlan<T> plan = diffPlanner.plan(packRoot, definition, workspace, ops);
        if (!plan.isEmpty())
            diskWriter.write(plan, definition, ops);

        workspace.clearDirty();
    }

    private <T> WorkspaceRegistry<T> workspace(String packId, WorkspaceDefinition<T> definition,
        Path packRoot, HolderLookup.Provider registries) {
        return definition.workspaceOrLoad(packId,
            () -> overlayManager.loadWorkspace(packId, packRoot, definition, registries, definition.baseline()));
    }
}
