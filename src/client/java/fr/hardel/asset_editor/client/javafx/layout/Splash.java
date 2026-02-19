package fr.hardel.asset_editor.client.javafx.layout;

import fr.hardel.asset_editor.client.AssetEditorClient;
import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.ui.SvgIcon;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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

    public Splash(Stage stage, String fontFamily) {
        setStyle("-fx-background-color: black;");

        TitleBar titleBar = new TitleBar(stage, fontFamily);
        StackPane.setAlignment(titleBar, Pos.TOP_LEFT);

        StackPane frame = buildDashedFrame();
        StackPane.setMargin(frame, new Insets(48, 24, 24, 24));

        StackPane center = buildCenterContent(fontFamily);

        getChildren().addAll(new GridBackground(), frame, center, titleBar);
    }

    private StackPane buildDashedFrame() {
        BorderPane content = new BorderPane();
        content.setPadding(new Insets(16));
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
        frame.setBorder(new Border(new BorderStroke(
                VoxelColors.WHITE_5,
                BorderStrokeStyle.DASHED,
                CornerRadii.EMPTY,
                new BorderWidths(1))));
        frame.setMouseTransparent(true);
        return frame;
    }

    private HBox buildFrameTop() {
        HBox dots = new HBox(4, dot("dot-1"), dot("dot-2"), dot("dot-3"));
        dots.setAlignment(Pos.CENTER_LEFT);

        Label build = new Label("BUILD " + AssetEditorClient.BUILD_VERSION);
        build.getStyleClass().add("build-label");
        build.setFont(Font.font("Consolas", 10));
        build.setTextFill(VoxelColors.ZINC_600);

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
        help.setFont(Font.font("Consolas", 10));
        help.setTextFill(VoxelColors.ZINC_600);
        help.setOnMouseEntered(e -> help.setTextFill(VoxelColors.ZINC_400));
        help.setOnMouseExited(e -> help.setTextFill(VoxelColors.ZINC_600));

        SvgIcon github = new SvgIcon(GITHUB, 16, Color.WHITE);
        github.setOnMouseEntered(e -> github.setOpacity(0.7));
        github.setOnMouseExited(e -> github.setOpacity(1.0));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bottom = new HBox();
        bottom.setAlignment(Pos.BOTTOM_LEFT);
        bottom.getChildren().addAll(help, spacer, github);
        return bottom;
    }

    private StackPane buildCenterContent(String fontFamily) {
        VBox column = buildCenterColumn(fontFamily);
        column.setMouseTransparent(true);

        Label loading = buildLoadingLabel();
        StackPane.setAlignment(loading, Pos.BOTTOM_CENTER);
        StackPane.setMargin(loading, new Insets(0, 0, 80, 0));

        StackPane wrapper = new StackPane(column, loading);
        wrapper.setMouseTransparent(true);
        return wrapper;
    }

    private VBox buildCenterColumn(String fontFamily) {
        StackPane logoGroup = buildLogoGroup();

        Text title = new Text("VOXEL");
        title.getStyleClass().add("splash-title");
        title.setFont(Font.font(fontFamily, FontWeight.EXTRA_BOLD, 36));
        title.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(1, VoxelColors.ZINC_400)));

        Label subtitle = new Label(I18n.get("tauri:splash.subtitle").toUpperCase());
        subtitle.getStyleClass().add("splash-subtitle");
        subtitle.setFont(Font.font(fontFamily, FontWeight.MEDIUM, 12));
        subtitle.setTextFill(VoxelColors.ZINC_500);

        VBox titleGroup = new VBox(4, title, subtitle);
        titleGroup.setAlignment(Pos.CENTER);

        VBox column = new VBox(32, logoGroup, titleGroup);
        column.setAlignment(Pos.CENTER);
        return column;
    }

    private StackPane buildLogoGroup() {
        Region glow = new Region();
        glow.setPrefSize(96, 96);
        glow.setMaxSize(96, 96);
        glow.setBackground(new Background(new BackgroundFill(
                VoxelColors.WHITE_10,
                new CornerRadii(48),
                null)));
        glow.setEffect(new GaussianBlur(40));

        SvgIcon logo = new SvgIcon(LOGO, 96, Color.WHITE);

        FadeTransition pulse = new FadeTransition(Duration.seconds(2), logo);
        pulse.setFromValue(1.0);
        pulse.setToValue(0.5);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        return new StackPane(glow, logo);
    }

    private Label buildLoadingLabel() {
        Label loading = new Label(I18n.get("tauri:splash.loading").toUpperCase());
        loading.getStyleClass().add("loading-label");
        loading.setFont(Font.font("Consolas", 10));
        loading.setTextFill(VoxelColors.ZINC_400);

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
