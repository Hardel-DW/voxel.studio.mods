package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import net.minecraft.resources.Identifier;

public final class Dialog {

    private static final Identifier CLOSE_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/close.svg");

    private final Stage stage = new Stage(StageStyle.TRANSPARENT);
    private final HBox footer = new HBox(12);
    private final StackPane backdrop = new StackPane();
    private final StackPane dialogCard = new StackPane();
    private boolean ownerSet = false;

    public Dialog(String title, Node content) {
        stage.initModality(Modality.WINDOW_MODAL);

        Label titleLabel = new Label(title);
        titleLabel.setTextFill(VoxelColors.ZINC_100);
        titleLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 18));

        SvgIcon closeIcon = new SvgIcon(CLOSE_ICON, 14, VoxelColors.ZINC_400);
        StackPane closeBtn = new StackPane(closeIcon);
        closeBtn.getStyleClass().add("ui-dialog-close");
        closeBtn.setPrefSize(28, 28);
        closeBtn.setMaxSize(28, 28);
        closeBtn.setCursor(Cursor.HAND);
        closeBtn.setOnMouseEntered(e -> closeIcon.setIconFill(Color.WHITE));
        closeBtn.setOnMouseExited(e -> closeIcon.setIconFill(VoxelColors.ZINC_400));
        closeBtn.setOnMouseClicked(e -> close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(titleLabel, spacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 20, 12, 20));

        VBox body = new VBox(content);
        body.setPadding(new Insets(0, 20, 16, 20));

        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(0, 20, 20, 20));

        VBox layout = new VBox(header, body, footer);
        layout.setMaxSize(Double.MAX_VALUE, Region.USE_PREF_SIZE);

        dialogCard.getStyleClass().add("ui-dialog-content");
        dialogCard.getChildren().add(layout);

        var shine = new ShineOverlay(dialogCard.widthProperty(), dialogCard.heightProperty().multiply(0.4))
            .withOpacity(0.15);
        if (shine.isLoaded())
            dialogCard.getChildren().addFirst(shine);

        dialogCard.setMaxWidth(460);
        dialogCard.setMinWidth(360);
        dialogCard.setMaxHeight(Region.USE_PREF_SIZE);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        dialogCard.widthProperty().addListener((obs, o, w) -> clip.setWidth(w.doubleValue()));
        dialogCard.heightProperty().addListener((obs, o, h) -> clip.setHeight(h.doubleValue()));
        dialogCard.setClip(clip);

        backdrop.getStyleClass().add("ui-dialog-backdrop");
        backdrop.getChildren().add(dialogCard);
        backdrop.setAlignment(Pos.CENTER);
        backdrop.setOnMouseClicked(e -> {
            if (e.getTarget() == backdrop)
                close();
        });

        Scene scene = new Scene(backdrop);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(
            Dialog.class.getResource("/assets/asset_editor/css/editor.css").toExternalForm());
        stage.setScene(scene);
    }

    public Dialog addFooterButton(Node button) {
        footer.getChildren().add(button);
        return this;
    }

    public void show(Window owner) {
        if (!ownerSet) {
            stage.initOwner(owner);
            ownerSet = true;
        }
        stage.setWidth(owner.getWidth());
        stage.setHeight(owner.getHeight());
        stage.setX(owner.getX());
        stage.setY(owner.getY());
        stage.show();
    }

    public void close() {
        stage.close();
    }

    public Stage stage() {
        return stage;
    }
}
