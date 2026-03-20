package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.javafx.VoxelColors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Cursor;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public final class CopyButton extends StackPane {

    private static final Identifier COPY_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/copy.svg");
    private static final Identifier CHECK_ICON = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/check.svg");
    private static final double ICON_SIZE = 14;

    private final Supplier<String> textSupplier;
    private final SvgIcon copyIcon;
    private final SvgIcon checkIcon;

    public CopyButton(Supplier<String> textSupplier) {
        this.textSupplier = textSupplier;

        copyIcon = new SvgIcon(COPY_ICON, ICON_SIZE, VoxelColors.ZINC_500);
        checkIcon = new SvgIcon(CHECK_ICON, ICON_SIZE, VoxelColors.EMERALD_400);
        checkIcon.setVisible(false);
        checkIcon.setManaged(false);

        getChildren().addAll(copyIcon, checkIcon);
        setCursor(Cursor.HAND);
        setPrefSize(24, 24);
        setMinSize(24, 24);
        setMaxSize(24, 24);

        setOnMouseClicked(e -> copy());
        setOnMouseEntered(e -> copyIcon.setIconFill(VoxelColors.ZINC_300));
        setOnMouseExited(e -> copyIcon.setIconFill(VoxelColors.ZINC_500));
    }

    public CopyButton(String text) {
        this(() -> text);
    }

    private void copy() {
        ClipboardContent content = new ClipboardContent();
        content.putString(textSupplier.get());
        Clipboard.getSystemClipboard().setContent(content);
        showConfirmation();
    }

    private void showConfirmation() {
        copyIcon.setVisible(false);
        copyIcon.setManaged(false);
        checkIcon.setVisible(true);
        checkIcon.setManaged(true);

        new Timeline(new KeyFrame(Duration.millis(1200), e -> {
            checkIcon.setVisible(false);
            checkIcon.setManaged(false);
            copyIcon.setVisible(true);
            copyIcon.setManaged(true);
        })).play();
    }
}
