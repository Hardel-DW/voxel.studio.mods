package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.AssetEditor;
import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.minecraft.resources.Identifier;

/**
 * Inline button card: px-6 py-4, title + description on left, check/lock icon on right. Active: bg-zinc-950/50 + zinc-700 ring. Locked: opacity-50.
 */
public final class InlineCard extends SimpleCard {

    private static final Identifier CHECK = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/check.svg");
    private static final Identifier LOCK = Identifier.fromNamespaceAndPath(AssetEditor.MOD_ID, "icons/tools/lock.svg");

    private final BooleanProperty active = new SimpleBooleanProperty(false);

    public InlineCard(String title, String description) {
        this(title, description, false, false, null);
    }

    public InlineCard(String title, String description, boolean initialActive, boolean locked, String lockText) {
        super(new Insets(16, 24, 16, 24));
        setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(title);
        titleLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 16));
        titleLabel.setTextFill(Color.WHITE);

        String descText = locked && lockText != null ? lockText : description;
        Label descLabel = new Label(descText);
        descLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.LIGHT, 12));
        descLabel.setTextFill(VoxelColors.ZINC_400);
        descLabel.setWrapText(true);

        VBox textBlock = new VBox(4, titleLabel, descLabel);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        Region iconSlot;
        if (locked) {
            iconSlot = new SvgIcon(LOCK, 24, Color.WHITE);
            setOpacity(0.5);
        } else {
            SvgIcon check = new SvgIcon(CHECK, 24, Color.WHITE);
            check.setVisible(initialActive);
            active.addListener((obs, o, v) -> check.setVisible(v));
            iconSlot = check;
        }

        HBox row = new HBox(16, textBlock, iconSlot);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        contentBox.getChildren().add(row);

        applyActive(initialActive);
        active.set(initialActive);
        if (!locked) {
            setOnMouseClicked(e -> active.set(!active.get()));
            active.addListener((obs, o, v) -> applyActive(v));
        }
    }

    private void applyActive(boolean on) {
        if (on) {
            visualCard.getStyleClass().add("ui-tool-slot-active");
        } else {
            visualCard.getStyleClass().remove("ui-tool-slot-active");
        }
    }

    public BooleanProperty activeProperty() {
        return active;
    }

    public boolean isActive() {
        return active.get();
    }
}
