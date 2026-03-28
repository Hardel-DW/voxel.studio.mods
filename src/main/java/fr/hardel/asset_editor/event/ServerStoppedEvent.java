package fr.hardel.asset_editor.event;

import fr.hardel.asset_editor.permission.PermissionManager;
import fr.hardel.asset_editor.store.ServerPackManager;
import fr.hardel.asset_editor.store.workspace.WorkspaceRepository;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class ServerStoppedEvent {

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            PermissionManager.shutdown();
            WorkspaceRepository.shutdown();
            ServerPackManager.shutdown();
        });
    }

    private ServerStoppedEvent() {
    }
}
