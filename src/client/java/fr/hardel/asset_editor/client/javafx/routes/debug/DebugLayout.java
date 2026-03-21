package fr.hardel.asset_editor.client.javafx.routes.debug;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.layout.editor.EditorBreadcrumb;
import fr.hardel.asset_editor.client.javafx.components.layout.editor.EditorHeaderTabItem;
import fr.hardel.asset_editor.client.javafx.lib.Page;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.utils.ColorUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.minecraft.client.resources.language.I18n;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class DebugLayout extends VBox implements Page {

    private static final String TAB_WORKSPACE = "workspace";
    private static final String TAB_CODE = "code";
    private static final String TAB_RENDER = "render";
    private static final String TAB_LOGS = "logs";
    private static final String TAB_NETWORK = "network";
    private static final Color DEBUG_TINT = ColorUtils.hueToColor(24, 0.74, 0.58);

    private final Label subtitle = new Label();
    private final StackPane outlet = new StackPane();
    private final Map<String, Node> pages = new HashMap<>();
    private final VBox headerContent = new VBox();
    private final StudioContext context;
    private String currentTab = TAB_WORKSPACE;

    public DebugLayout(StudioContext context) {
        this.context = context;
        getStyleClass().add("concept-main-page");
        getStyleClass().add("debug-layout");
        setFillWidth(true);

        StackPane headerSurface = new StackPane();
        headerSurface.getStyleClass().add("editor-header");
        headerSurface.getStyleClass().add("editor-header-surface");
        headerSurface.setAlignment(Pos.TOP_LEFT);
        headerSurface.setMaxWidth(Double.MAX_VALUE);

        Region tintLayer = new Region();
        tintLayer.getStyleClass().add("editor-header-tint");
        tintLayer.setOpacity(0.4);
        tintLayer.setMouseTransparent(true);
        tintLayer.setStyle("-fx-background-color: " + ColorUtils.toCssRgba(DEBUG_TINT) + ";");

        Region gradientLayer = new Region();
        gradientLayer.getStyleClass().add("editor-header-gradient");
        gradientLayer.setMouseTransparent(true);

        headerContent.getStyleClass().add("editor-header-content");
        headerContent.setPadding(new Insets(32, 32, 24, 32));
        headerContent.setSpacing(0);
        headerContent.setMaxWidth(Double.MAX_VALUE);

        headerSurface.getChildren().addAll(tintLayer, gradientLayer, headerContent);

        outlet.getStyleClass().add("debug-layout-outlet");
        VBox.setVgrow(outlet, Priority.ALWAYS);

        getChildren().addAll(headerSurface, outlet);
        refreshHeader();
        showTab(TAB_WORKSPACE);
    }

    private void refreshHeader() {
        headerContent.getChildren().clear();

        Label title = new Label(I18n.get("debug:layout.title"));
        title.getStyleClass().add("editor-header-title");
        title.setFont(VoxelFonts.of(VoxelFonts.Variant.MINECRAFT_TEN, 36));

        subtitle.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
        subtitle.setTextFill(VoxelColors.ZINC_500);

        Region colorLine = new Region();
        colorLine.getStyleClass().add("editor-header-color-line");
        colorLine.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, "
            + ColorUtils.toCssRgba(DEBUG_TINT) + " 0%, transparent 100%);");

        EditorBreadcrumb breadcrumb = new EditorBreadcrumb(I18n.get("debug:layout.title"), List.of(), false, null);
        VBox left = new VBox(8, breadcrumb, title, colorLine);
        HBox.setHgrow(left, Priority.ALWAYS);

        HBox row = new HBox(32, left);
        row.setAlignment(Pos.BOTTOM_LEFT);
        headerContent.getChildren().add(row);

        HBox tabs = new HBox(4);
        tabs.getStyleClass().add("editor-header-tabs");
        tabs.setAlignment(Pos.CENTER_LEFT);
        tabs.setPadding(new Insets(24, 0, 0, 0));
        tabs.getChildren().addAll(
            new EditorHeaderTabItem(I18n.get("debug:layout.tab.workspace"), TAB_WORKSPACE.equals(currentTab), () -> showTab(TAB_WORKSPACE)),
            new EditorHeaderTabItem(I18n.get("debug:layout.tab.code"), TAB_CODE.equals(currentTab), () -> showTab(TAB_CODE)),
            new EditorHeaderTabItem(I18n.get("debug:layout.tab.render"), TAB_RENDER.equals(currentTab), () -> showTab(TAB_RENDER)),
            new EditorHeaderTabItem(I18n.get("debug:layout.tab.logs"), TAB_LOGS.equals(currentTab), () -> showTab(TAB_LOGS)),
            new EditorHeaderTabItem(I18n.get("debug:layout.tab.network"), TAB_NETWORK.equals(currentTab), () -> showTab(TAB_NETWORK)));
        VBox.setMargin(tabs, new Insets(0, 0, -8, 0));
        headerContent.getChildren().add(tabs);
    }

    private void showTab(String tab) {
        currentTab = tab;
        subtitle.setText(switch (tab) {
            case TAB_WORKSPACE -> I18n.get("debug:layout.subtitle.workspace");
            case TAB_CODE -> I18n.get("debug:layout.subtitle.code");
            case TAB_LOGS -> I18n.get("debug:layout.subtitle.logs");
            case TAB_NETWORK -> I18n.get("debug:layout.subtitle.network");
            default -> I18n.get("debug:layout.subtitle.render");
        });
        refreshHeader();
        outlet.getChildren().setAll(resolvePage(tab));
    }

    private Node resolvePage(String tab) {
        return pages.computeIfAbsent(tab, this::createPage);
    }

    private Node createPage(String tab) {
        Supplier<Node> factory = switch (tab) {
            case TAB_WORKSPACE -> () -> new DebugWorkspacePage(context);
            case TAB_CODE -> DebugCodeBlockPage::new;
            case TAB_LOGS -> DebugLogsPage::new;
            case TAB_NETWORK -> DebugNetworkPage::new;
            default -> DebugRenderPage::new;
        };
        Node page = factory.get();
        if (page instanceof Region region)
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return page;
    }
}
