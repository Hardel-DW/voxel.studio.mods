package fr.hardel.asset_editor.event;

import fr.hardel.asset_editor.permission.PermissionManager;
import fr.hardel.asset_editor.workspace.io.DataPackManager;
import fr.hardel.asset_editor.workspace.WorkspaceRepository;
import fr.hardel.asset_editor.workspace.WorkspaceBaselineSnapshots;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class SeverStartedEvent {

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PermissionManager.init(server);
            WorkspaceRepository.init(server);
            DataPackManager.init(server);
            WorkspaceBaselineSnapshots.snapshot(server);
        });
    }

    private SeverStartedEvent() {
    }
}
