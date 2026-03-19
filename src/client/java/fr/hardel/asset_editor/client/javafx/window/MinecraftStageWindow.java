package fr.hardel.asset_editor.client.javafx.window;

import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.VoxelResourceLoader;
import javafx.application.Platform;
import javafx.scene.text.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class MinecraftStageWindow extends UndecoratedStageWindow {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftStageWindow.class);

    private boolean platformStarted;

    protected MinecraftStageWindow(double minWidth, double minHeight, List<String> stylesheets) {
        super(minWidth, minHeight, stylesheets);
    }

    protected abstract void onCreated();

    protected void onWorldClosed() {}

    protected void onResourceReload() {}

    public boolean isPlatformStarted() {
        return platformStarted;
    }

    public void open() {
        if (!platformStarted) {
            platformStarted = true;
            Thread.ofVirtual().name("javafx-init").start(() -> {
                Platform.startup(() -> {
                    VoxelResourceLoader.update(Minecraft.getInstance().getResourceManager());
                    loadFonts();
                    onCreated();
                    show();
                });
                Platform.setImplicitExit(false);
            });
            return;
        }

        Platform.runLater(() -> {
            show();
        });
    }

    public void fireWorldClosed() {
        if (platformStarted)
            Platform.runLater(this::onWorldClosed);
    }

    public void fireResourceReload() {
        if (platformStarted)
            Platform.runLater(this::onResourceReload);
    }

    private void loadFonts() {
        for (var variant : VoxelFonts.Variant.values()) {
            Identifier id = Identifier.fromNamespaceAndPath("asset_editor", "fonts/" + variant.fileName + ".ttf");
            try (var is = VoxelResourceLoader.open(id)) {
                VoxelFonts.register(variant, Font.loadFont(is, 12));
            } catch (Exception exception) {
                LOGGER.warn("Failed to load font {}: {}", variant.fileName, exception.getMessage());
            }
        }
    }
}
