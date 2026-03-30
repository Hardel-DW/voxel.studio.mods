package fr.hardel.asset_editor.workspace;

import fr.hardel.asset_editor.store.workspace.WorkspaceRepository;
import fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBinding;
import fr.hardel.asset_editor.workspace.registry.RegistryWorkspaceBindings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
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
            .map(pack -> pack.open())
            .toList());

        try (var vanillaResources = new MultiPackResourceManager(PackType.SERVER_DATA, vanillaPacks)) {
            for (var binding : RegistryWorkspaceBindings.all())
                snapshotBinding(server, repository, vanillaResources, binding);
        }
    }

    private static <T> void snapshotBinding(
        MinecraftServer server,
        WorkspaceRepository repository,
        MultiPackResourceManager resources,
        RegistryWorkspaceBinding<T> binding
    ) {
        var registry = server.registryAccess().lookup(binding.registryKey()).orElse(null);
        repository.snapshotBaseline(binding, resources, registry, server.registryAccess());
    }

    private WorkspaceBaselineSnapshots() {
    }
}
