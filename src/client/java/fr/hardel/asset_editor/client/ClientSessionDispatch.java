package fr.hardel.asset_editor.client;

import fr.hardel.asset_editor.client.javafx.window.VoxelStudioWindow;
import fr.hardel.asset_editor.client.javafx.lib.action.EditorActionGateway;
import fr.hardel.asset_editor.client.state.ClientSessionState;
import fr.hardel.asset_editor.permission.StudioPermissions;
import fr.hardel.asset_editor.network.pack.PackWorkspaceSyncPayload;
import fr.hardel.asset_editor.network.workspace.WorkspaceSyncPayload;
import fr.hardel.asset_editor.store.ServerPackManager.PackEntry;
import javafx.application.Platform;

import java.util.List;

public final class ClientSessionDispatch {

    private final ClientSessionState sessionState;
    private EditorActionGateway activeGateway;

    public ClientSessionDispatch(ClientSessionState sessionState) {
        this.sessionState = sessionState;
    }

    public void setGateway(EditorActionGateway gateway) {
        activeGateway = gateway;
    }

    public void clearGateway() {
        clearGateway(activeGateway);
    }

    public void clearGateway(EditorActionGateway gateway) {
        if (activeGateway == gateway)
            activeGateway = null;
    }

    public void handlePermissionSync(StudioPermissions permissions) {
        runSessionUpdate(() -> sessionState.updatePermissions(permissions));
    }

    public void handlePackListSync(List<PackEntry> packs) {
        runSessionUpdate(() -> sessionState.updatePacks(packs));
    }

    public void handleWorkspaceSync(WorkspaceSyncPayload payload) {
        var gateway = activeGateway;
        if (gateway != null)
            Platform.runLater(() -> gateway.handleWorkspaceSync(payload));
    }

    public void handlePackWorkspaceSync(PackWorkspaceSyncPayload payload) {
        var gateway = activeGateway;
        if (gateway != null)
            Platform.runLater(() -> gateway.handlePackWorkspaceSync(payload.packId(), payload.registryId(), payload.entries()));
    }

    private void runSessionUpdate(Runnable update) {
        if (!VoxelStudioWindow.isUiThreadAvailable()) {
            update.run();
            return;
        }
        if (Platform.isFxApplicationThread()) {
            update.run();
            return;
        }
        Platform.runLater(update);
    }
}
