package fr.hardel.asset_editor.event;

import fr.hardel.asset_editor.permission.PermissionManager;
import fr.hardel.asset_editor.store.ServerPackManager;
import fr.hardel.asset_editor.store.workspace.WorkspaceRepository;
import fr.hardel.asset_editor.workspace.WorkspaceBaselineSnapshots;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class SeverStartedEvent {

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PermissionManager.init(server);
            WorkspaceRepository.init(server);
            ServerPackManager.init(server);
            WorkspaceBaselineSnapshots.snapshot(server);
        });
    }

    private SeverStartedEvent() {
    }
}
