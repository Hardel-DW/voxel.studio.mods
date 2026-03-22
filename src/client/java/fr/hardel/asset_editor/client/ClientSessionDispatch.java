package fr.hardel.asset_editor.client;

import fr.hardel.asset_editor.client.action.WorkspaceSyncGateway;
import fr.hardel.asset_editor.client.compose.window.VoxelStudioWindow;
import fr.hardel.asset_editor.client.state.ClientSessionState;
import fr.hardel.asset_editor.network.pack.PackListSyncPayload;
import fr.hardel.asset_editor.network.pack.PackWorkspaceSyncPayload;
import fr.hardel.asset_editor.network.session.PermissionSyncPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload;

import javax.swing.SwingUtilities;

public final class ClientSessionDispatch {

    private final ClientSessionState sessionState;
    private WorkspaceSyncGateway activeGateway;

    public ClientSessionDispatch(ClientSessionState sessionState) {
        this.sessionState = sessionState;
    }

    public void setGateway(WorkspaceSyncGateway gateway) {
        activeGateway = gateway;
    }

    public void clearGateway() {
        clearGateway(activeGateway);
    }

    public void clearGateway(WorkspaceSyncGateway gateway) {
        if (activeGateway == gateway)
            activeGateway = null;
    }

    public void handlePermissionSync(PermissionSyncPayload payload) {
        runSessionUpdate(() -> sessionState.updatePermissions(payload.permissions()));
    }

    public void handlePackListSync(PackListSyncPayload payload) {
        runSessionUpdate(() -> sessionState.updatePacks(payload.packs()));
    }

    public void handleWorkspaceSync(WorkspaceSyncPayload payload) {
        var gateway = activeGateway;
        if (gateway != null)
            runOnUiThread(() -> gateway.handleWorkspaceSync(payload));
    }

    public void handlePackWorkspaceSync(PackWorkspaceSyncPayload payload) {
        var gateway = activeGateway;
        if (gateway != null)
            runOnUiThread(() -> gateway.handlePackWorkspaceSync(payload.packId(), payload.registryId(), payload.entries()));
    }

    private void runSessionUpdate(Runnable update) {
        if (!VoxelStudioWindow.isUiThreadAvailable()) {
            update.run();
            return;
        }

        runOnUiThread(update);
    }

    private void runOnUiThread(Runnable update) {
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
            return;
        }

        SwingUtilities.invokeLater(update);
    }
}
