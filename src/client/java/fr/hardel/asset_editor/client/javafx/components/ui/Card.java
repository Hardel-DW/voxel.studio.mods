package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

/**
 * Slot card: p-4 card with image (bottom) and title/desc (top).
 * Active:  bg-zinc-950/50 + zinc-700 border ring.
 * Locked:  opacity-50 + zinc-700 border + lock icon + lock text.
 * Clicking toggles active state (local BooleanProperty).
 */
public final class Card extends SimpleCard {

    private static final Identifier CHECK = Identifier.fromNamespaceAndPath("asset_editor", "icons/check.svg");
    private static final Identifier LOCK  = Identifier.fromNamespaceAndPath("asset_editor", "icons/tools/lock.svg");

    private final BooleanProperty active = new SimpleBooleanProperty(false);
    private final boolean locked;

    public Card(Identifier imageId, String titleKey, String descKey, boolean initialActive, boolean locked, String lockKey) {
        super(new Insets(16));
        setMaxWidth(Double.MAX_VALUE);
        this.locked = locked;

        Label title = new Label(I18n.get(titleKey));
        title.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 16));
        title.setTextFill(VoxelColors.ZINC_100);

        VBox textBlock = new VBox(4, title);
        textBlock.setAlignment(Pos.TOP_LEFT);
        textBlock.setMaxWidth(Double.MAX_VALUE);
        if (descKey != null) {
            Label desc = new Label(I18n.get(descKey));
            desc.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 13));
            desc.setTextFill(VoxelColors.ZINC_400);
            desc.setWrapText(true);
            textBlock.getChildren().add(desc);
        }

        ResourceImageIcon image = new ResourceImageIcon(imageId, 64);
        StackPane imageWrap = new StackPane(image);
        imageWrap.setAlignment(Pos.CENTER);
        imageWrap.setMinSize(64, 64);
        imageWrap.setPrefSize(64, 64);
        imageWrap.setMaxSize(64, 64);

        VBox inner = new VBox(textBlock, imageWrap);
        inner.setAlignment(Pos.TOP_CENTER);
        VBox.setMargin(textBlock, new Insets(0, 0, 32, 0));
        VBox.setMargin(imageWrap, new Insets(0, 0, 32, 0));

        contentBox.getChildren().add(inner);

        buildOverlays(lockKey);
        applyActiveState(initialActive);
        active.set(initialActive);

        if (locked) {
            setOpacity(0.5);
        } else {
            setOnMouseClicked(e -> active.set(!active.get()));
            active.addListener((obs, o, v) -> applyActiveState(v));
        }
    }

    public Card(Identifier imageId, String titleKey) {
        this(imageId, titleKey, null, false, false, null);
    }

    public Card(Identifier imageId, String titleKey, boolean initialActive) {
        this(imageId, titleKey, null, initialActive, false, null);
    }

    private void buildOverlays(String lockKey) {
        if (locked) {
            SvgIcon lockIcon = new SvgIcon(LOCK, 24, Color.WHITE);
            StackPane.setAlignment(lockIcon, Pos.TOP_RIGHT);
            StackPane.setMargin(lockIcon, new Insets(16));
            lockIcon.setMouseTransparent(true);
            visualCard.getChildren().add(lockIcon);

            if (lockKey != null) {
                Label lockText = new Label(I18n.get(lockKey));
                lockText.setFont(VoxelFonts.of(VoxelFonts.Variant.LIGHT, 11));
                lockText.setTextFill(VoxelColors.ZINC_400);
                lockText.setMouseTransparent(true);
                StackPane.setAlignment(lockText, Pos.BOTTOM_RIGHT);
                StackPane.setMargin(lockText, new Insets(16));
                visualCard.getChildren().add(lockText);
            }
        } else {
            SvgIcon checkIcon = new SvgIcon(CHECK, 24, Color.WHITE);
            checkIcon.setMouseTransparent(true);
            StackPane.setAlignment(checkIcon, Pos.TOP_RIGHT);
            StackPane.setMargin(checkIcon, new Insets(16));
            checkIcon.setVisible(false);
            visualCard.getChildren().add(checkIcon);

            active.addListener((obs, o, v) -> checkIcon.setVisible(v));
        }
    }

    private void applyActiveState(boolean on) {
        if (on) {
            visualCard.getStyleClass().add("ui-tool-slot-active");
        } else {
            visualCard.getStyleClass().remove("ui-tool-slot-active");
        }
    }

    public BooleanProperty activeProperty() { return active; }
    public boolean isActive() { return active.get(); }
    public void setActive(boolean v) { active.set(v); }
}
