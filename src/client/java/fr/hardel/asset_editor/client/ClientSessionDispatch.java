package fr.hardel.asset_editor.client;

import fr.hardel.asset_editor.client.javafx.lib.action.EditorActionGateway;
import fr.hardel.asset_editor.network.EditorAction;
import javafx.application.Platform;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public final class ClientSessionDispatch {

    private static EditorActionGateway activeGateway;

    public static void setGateway(EditorActionGateway gateway) {
        activeGateway = gateway;
    }

    public static void clearGateway() {
        activeGateway = null;
    }

    public static void handleActionResponse(UUID actionId, boolean accepted, String message) {
        var gateway = activeGateway;
        if (gateway != null)
            Platform.runLater(() -> gateway.handleResponse(actionId, accepted, message));
    }

    public static void handleElementUpdate(Identifier registryId, Identifier targetId, EditorAction action) {
        var gateway = activeGateway;
        if (gateway != null)
            Platform.runLater(() -> gateway.handleRemoteUpdate(registryId, targetId, action));
    }

    private ClientSessionDispatch() {
    }
}
