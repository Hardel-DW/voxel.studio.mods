package fr.hardel.asset_editor.workspace;

import fr.hardel.asset_editor.store.workspace.WorkspaceRepository;
import fr.hardel.asset_editor.workspace.definition.WorkspaceDefinition;
import fr.hardel.asset_editor.workspace.definition.WorkspaceDefinitions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

import java.util.ArrayList;

public final class WorkspaceBaselineSnapshots {

    public static void snapshot(MinecraftServer server) {
        var repository = WorkspaceRepository.get();
        if (repository == null)
            return;

        var selectedPacks = server.getPackRepository().getSelectedPacks();
        var vanillaPacks = new ArrayList<>(selectedPacks.stream()
            .filter(pack -> pack.getPackSource() != PackSource.WORLD)
            .map(Pack::open)
            .toList());

        try (var vanillaResources = new MultiPackResourceManager(PackType.SERVER_DATA, vanillaPacks)) {
            for (var definition : WorkspaceDefinitions.all())
                snapshotDefinition(server, repository, vanillaResources, definition);
        }
    }

    private static <T> void snapshotDefinition(
        MinecraftServer server,
        WorkspaceRepository repository,
        MultiPackResourceManager resources,
        WorkspaceDefinition<T> definition
    ) {
        var registry = server.registryAccess().lookup(definition.registryKey()).orElse(null);
        repository.snapshotBaseline(definition, resources, registry, server.registryAccess());
    }

    private WorkspaceBaselineSnapshots() {
    }
}
