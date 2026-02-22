package fr.hardel.asset_editor.client.javafx.components.layout.loading;

import fr.hardel.asset_editor.client.AssetEditorClient;
import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.GridBackground;
import fr.hardel.asset_editor.client.javafx.components.ui.SpacedText;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.lib.utils.BrowserUtils;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

/**
 * Matches the Splash component (Splash.tsx) exactly
 * Z-order (bottom → top):
 *   0 – GridBackground   (fills scene)
 *   1 – DashedFrame      (margins: top 48, LRB 24)
 *   2 – CenterContent    (fills scene, mouse-transparent)
 *   3 – TitleBar         (aligned to top)
 */
public final class Splash extends StackPane {

    private static final Identifier LOGO   = Identifier.fromNamespaceAndPath("asset_editor", "icons/logo.svg");
    private static final Identifier GITHUB = Identifier.fromNamespaceAndPath("asset_editor", "icons/company/github.svg");

    public Splash(Stage stage) {
        setStyle("-fx-background-color: black;");

        TitleBar titleBar = new TitleBar(stage);
        titleBar.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(titleBar, Pos.TOP_LEFT);

        StackPane frame = buildDashedFrame();
        StackPane.setMargin(frame, new Insets(48, 24, 24, 24));

        VBox centerColumn = buildCenterColumn();
        centerColumn.setMouseTransparent(true);

        SpacedText loading = buildLoadingText();
        loading.setMaxHeight(Region.USE_PREF_SIZE);
        loading.setMouseTransparent(true);
        StackPane.setAlignment(loading, Pos.BOTTOM_CENTER);
        StackPane.setMargin(loading, new Insets(0, 0, 80, 0));

        GridBackground grid = new GridBackground();
        Rectangle clip = new Rectangle();
        grid.layoutBoundsProperty().addListener((obs, ov, nv) -> {
            clip.setWidth(nv.getWidth());
            clip.setHeight(nv.getHeight());
        });
        grid.setClip(clip);
        StackPane.setMargin(grid, new Insets(48, 24, 24, 24));

        getChildren().addAll(grid, centerColumn, loading, frame, titleBar);
    }

    private StackPane buildDashedFrame() {
        BorderPane content = new BorderPane();
        content.setPadding(new Insets(16));
        content.setPickOnBounds(false);
        content.setTop(buildFrameTop());
        content.setBottom(buildFrameBottom());

        Region cornerTL = corner("corner-tl");
        Region cornerTR = corner("corner-tr");
        Region cornerBL = corner("corner-bl");
        Region cornerBR = corner("corner-br");

        StackPane.setAlignment(cornerTL, Pos.TOP_LEFT);
        StackPane.setAlignment(cornerTR, Pos.TOP_RIGHT);
        StackPane.setAlignment(cornerBL, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(cornerBR, Pos.BOTTOM_RIGHT);

        StackPane.setMargin(cornerTL, new Insets(-1, 0, 0, -1));
        StackPane.setMargin(cornerTR, new Insets(-1, -1, 0, 0));
        StackPane.setMargin(cornerBL, new Insets(0, 0, -1, -1));
        StackPane.setMargin(cornerBR, new Insets(0, -1, -1, 0));

        StackPane frame = new StackPane(content, cornerTL, cornerTR, cornerBL, cornerBR);
        frame.getStyleClass().add("dashed-frame");
        frame.setPickOnBounds(false);
        return frame;
    }

    private HBox buildFrameTop() {
        HBox dots = new HBox(4, dot("dot-1"), dot("dot-2"), dot("dot-3"));
        dots.setAlignment(Pos.CENTER_LEFT);

        SpacedText build = new SpacedText(
                "BUILD " + AssetEditorClient.BUILD_VERSION,
                Font.font("Consolas", 10),
                VoxelColors.ZINC_600,
                0.1);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox();
        top.setAlignment(Pos.TOP_LEFT);
        top.getChildren().addAll(dots, spacer, build);
        return top;
    }

    private HBox buildFrameBottom() {
        Label help = new Label(I18n.get("tauri:splash.help"));
        help.getStyleClass().add("help-label");
        help.setTextFill(VoxelColors.ZINC_600);
        help.setCursor(Cursor.HAND);
        help.setOnMouseEntered(e -> help.setTextFill(VoxelColors.ZINC_400));
        help.setOnMouseExited(e -> help.setTextFill(VoxelColors.ZINC_600));
        help.setOnMouseClicked(e -> BrowserUtils.openBrowser("https://github.com"));

        SvgIcon github = new SvgIcon(GITHUB, 16, Color.WHITE);
        github.setCursor(Cursor.HAND);
        github.setOnMouseEntered(e -> github.setOpacity(0.7));
        github.setOnMouseExited(e -> github.setOpacity(1.0));
        github.setOnMouseClicked(e -> BrowserUtils.openBrowser("https://github.com"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bottom = new HBox();
        bottom.setAlignment(Pos.BOTTOM_LEFT);
        bottom.getChildren().addAll(help, spacer, github);
        return bottom;
    }

    private VBox buildCenterColumn() {
        StackPane logoGroup = buildLogoGroup();

        Text title = new Text(I18n.get("tauri:splash.title"));
        title.setFont(VoxelFonts.rubik(VoxelFonts.Rubik.EXTRA_BOLD, 36));
        title.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(1, VoxelColors.ZINC_400)));

        SpacedText subtitle = new SpacedText(
                I18n.get("tauri:splash.subtitle").toUpperCase(),
                VoxelFonts.rubik(VoxelFonts.Rubik.MEDIUM, 12),
                VoxelColors.ZINC_500,
                0.3);
        subtitle.setAlignment(Pos.CENTER);

        VBox titleGroup = new VBox(4, title, subtitle);
        titleGroup.setAlignment(Pos.CENTER);

        VBox column = new VBox(32, logoGroup, titleGroup);
        column.setAlignment(Pos.CENTER);
        return column;
    }

    private StackPane buildLogoGroup() {
        SvgIcon logo = new SvgIcon(LOGO, 96, Color.WHITE);

        FadeTransition pulse = new FadeTransition(Duration.seconds(2), logo);
        pulse.setFromValue(1.0);
        pulse.setToValue(0.5);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        return new StackPane(logo);
    }

    private SpacedText buildLoadingText() {
        SpacedText loading = new SpacedText(
                I18n.get("tauri:splash.loading").toUpperCase(),
                Font.font("Consolas", 10),
                VoxelColors.ZINC_400,
                0.1);
        loading.setMaxWidth(Region.USE_PREF_SIZE);

        FadeTransition pulse = new FadeTransition(Duration.seconds(1.5), loading);
        pulse.setFromValue(1.0);
        pulse.setToValue(0.3);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        return loading;
    }

    private Region corner(String styleClass) {
        Region r = new Region();
        r.getStyleClass().addAll("corner-decoration", styleClass);
        r.setMouseTransparent(true);
        return r;
    }

    private Region dot(String styleClass) {
        Region r = new Region();
        r.getStyleClass().addAll("dot", styleClass);
        return r;
    }
}



