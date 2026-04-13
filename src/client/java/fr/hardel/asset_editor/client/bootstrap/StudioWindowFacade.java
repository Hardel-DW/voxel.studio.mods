package fr.hardel.asset_editor.client.bootstrap;

import fr.hardel.asset_editor.client.compose.StudioRoutesKt;
import fr.hardel.asset_editor.client.compose.window.VoxelStudioWindow;

public final class StudioWindowFacade {

    private static boolean started = false;

    public static synchronized void start() {
        if (started) return;
        StudioRoutesKt.registerStudioRoutes();
        VoxelStudioWindow.initialize();
        started = true;
    }

    public static void requestOpen() {
        start();
        VoxelStudioWindow.requestOpen();
    }

    public static void notifyWorldClosed() {
        if (!started) return;
        VoxelStudioWindow.notifyWorldClosed();
    }

    public static void notifyResourceReload() {
        if (!started) return;
        VoxelStudioWindow.notifyResourceReload();
    }

    public static boolean isUiThreadAvailable() {
        if (!started) return false;
        return VoxelStudioWindow.isUiThreadAvailable();
    }

    private StudioWindowFacade() {}
}
