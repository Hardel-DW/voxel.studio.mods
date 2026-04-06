package fr.hardel.asset_editor.workspace;

import fr.hardel.asset_editor.workspace.definition.WorkspaceDefinition;
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
            for (var definition : WorkspaceDefinition.all())
                definition.snapshotBaseline(repository, vanillaResources, server.registryAccess());
        }
    }

    private WorkspaceBaselineSnapshots() {}
}
