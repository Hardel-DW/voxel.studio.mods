package fr.hardel.asset_editor.client;

import fr.hardel.asset_editor.client.javafx.VoxelStudioWindow;
import fr.hardel.asset_editor.permission.StudioPermissions;
import javafx.application.Platform;

public final class ClientPermissionState {

    private static StudioPermissions permissions = StudioPermissions.NONE;
    private static boolean received;

    public static StudioPermissions get() {
        return permissions;
    }

    public static boolean hasReceived() {
        return received;
    }

    public static void update(StudioPermissions newPermissions) {
        permissions = newPermissions;
        received = true;
        Platform.runLater(() -> VoxelStudioWindow.onPermissionsUpdated(newPermissions));
    }

    public static void reset() {
        permissions = StudioPermissions.NONE;
        received = false;
    }

    private ClientPermissionState() {
    }
}
