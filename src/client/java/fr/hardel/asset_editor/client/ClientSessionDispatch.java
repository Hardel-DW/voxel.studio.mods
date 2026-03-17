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
        if (activeGateway != null)
            Platform.runLater(() -> activeGateway.handleResponse(actionId, accepted, message));
    }

    public static void handleElementUpdate(Identifier registryId, Identifier targetId, EditorAction action) {
        if (activeGateway != null)
            Platform.runLater(() -> activeGateway.handleRemoteUpdate(registryId, targetId, action));
    }

    private ClientSessionDispatch() {
    }
}
