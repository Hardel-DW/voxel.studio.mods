package fr.hardel.asset_editor.client;

import fr.hardel.asset_editor.client.javafx.VoxelStudioWindow;
import fr.hardel.asset_editor.client.javafx.lib.action.EditorActionGateway;
import fr.hardel.asset_editor.client.state.ClientSessionState;
import fr.hardel.asset_editor.network.EditorAction;
import fr.hardel.asset_editor.permission.StudioPermissions;
import fr.hardel.asset_editor.store.ServerPackManager.PackEntry;
import javafx.application.Platform;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

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

    public void handleActionResponse(UUID actionId, boolean accepted, String message) {
        var gateway = activeGateway;
        if (gateway != null)
            Platform.runLater(() -> gateway.handleResponse(actionId, accepted, message));
    }

    public void handleElementUpdate(Identifier registryId, Identifier targetId, EditorAction action) {
        var gateway = activeGateway;
        if (gateway != null)
            Platform.runLater(() -> gateway.handleRemoteUpdate(registryId, targetId, action));
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
