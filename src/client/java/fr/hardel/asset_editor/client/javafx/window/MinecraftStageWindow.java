package fr.hardel.asset_editor.client.javafx.window;

import javafx.application.Platform;

import java.util.List;

public abstract class MinecraftStageWindow extends UndecoratedStageWindow {

    private boolean startupRequested;
    private volatile boolean platformReady;
    private volatile boolean pendingWorldClose;

    protected MinecraftStageWindow(double minWidth, double minHeight, List<String> stylesheets) {
        super(minWidth, minHeight, stylesheets);
    }

    protected abstract void onCreated();

    protected void onWorldClosed() {}

    protected void onResourceReload() {}

    public boolean isPlatformReady() {
        return platformReady;
    }

    public void open() {
        if (!startupRequested) {
            startupRequested = true;
            Thread.ofVirtual().name("javafx-init").start(() -> {
                Platform.startup(() -> {
                    initializeWindow();
                    platformReady = true;
                    onCreated();
                    if (pendingWorldClose) {
                        pendingWorldClose = false;
                        onWorldClosed();
                        return;
                    }
                    show();
                    onWindowFocused();
                });
                Platform.setImplicitExit(false);
            });
            return;
        }

        if (!platformReady)
            return;

        Platform.runLater(() -> {
            onWindowFocused();
            show();
        });
    }

    public void fireWorldClosed() {
        if (platformReady)
            Platform.runLater(this::onWorldClosed);
        else if (startupRequested)
            pendingWorldClose = true;
    }

    public void fireResourceReload() {
        if (platformReady)
            Platform.runLater(this::onResourceReload);
    }
}
