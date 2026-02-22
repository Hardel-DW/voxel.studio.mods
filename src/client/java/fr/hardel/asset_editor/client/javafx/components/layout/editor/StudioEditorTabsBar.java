package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.lib.utils.TextUtils;
import fr.hardel.asset_editor.client.javafx.lib.store.StudioOpenTab;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.components.ui.ResourceImageIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.WindowControls;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import net.minecraft.resources.Identifier;

/**
 * Workspace header bar — always visible.
 * Left: window controls (minimize / maximize / close).
 * Center/right: open element tabs + drag spacer.
 * Drag/resize behavior is handled at window level (VoxelStudioWindow).
 */
public final class StudioEditorTabsBar extends HBox {

    private static final Identifier CLOSE_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/close.svg");

    private final StudioContext context;
    private final HBox tabsContainer = new HBox(4);

    public StudioEditorTabsBar(StudioContext context, Stage stage) {
        this.context = context;
        getStyleClass().add("studio-editor-tabs");
        setAlignment(Pos.CENTER_LEFT);

        setPadding(new Insets(0, 0, 0, 16)); // pl-4
        tabsContainer.setAlignment(Pos.CENTER_LEFT);

        Region dragSpacer = new Region();
        HBox.setHgrow(dragSpacer, Priority.ALWAYS);

        context.tabsState().openTabs().addListener((ListChangeListener<StudioOpenTab>) c -> refreshTabs());
        context.tabsState().activeTabIndexProperty().addListener((obs, o, n) -> refreshTabs());

        getChildren().addAll(tabsContainer, dragSpacer, buildWindowControls(stage));
        refreshTabs();
    }

    private HBox buildWindowControls(Stage stage) {
        return new WindowControls(stage, "studio-window-button", 36, 48, null, stage::hide);
    }

    private void refreshTabs() {
        tabsContainer.getChildren().clear();
        int activeIndex = context.tabsState().activeTabIndexProperty().get();
        int index = 0;
        for (StudioOpenTab tab : context.tabsState().openTabs()) {
            tabsContainer.getChildren().add(createTab(tab, index == activeIndex, index));
            index++;
        }
    }

    private HBox createTab(StudioOpenTab tab, boolean active, int index) {
        HBox item = new HBox(8);
        item.getStyleClass().add("studio-editor-tab-item");
        if (active) item.getStyleClass().add("studio-editor-tab-item-active");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(6, 12, 6, 12)); // px-3 py-1.5
        item.setCursor(Cursor.HAND);
        item.setOnMouseClicked(e -> {
            context.tabsState().switchTab(index);
            context.router().navigate(tab.route());
            e.consume();
        });

        ResourceImageIcon icon = new ResourceImageIcon(StudioConcept.byRoute(tab.route()).icon(), 16); // size-4
        Text label = new Text(displayName(tab.elementId()));
        label.getStyleClass().add("studio-editor-tab-label");
        StackPane closeBtn = closeButton(index);

        item.getChildren().addAll(icon, label, closeBtn);
        return item;
    }

    private StackPane closeButton(int index) {
        SvgIcon icon = new SvgIcon(CLOSE_ICON, 10, VoxelColors.ZINC_400);

        StackPane btn = new StackPane(icon);
        btn.getStyleClass().add("studio-editor-tab-close");
        btn.setPrefSize(16, 16);
        btn.setMinSize(16, 16);
        btn.setMaxSize(16, 16);
        btn.setCursor(Cursor.HAND);
        btn.setOnMouseClicked(e -> {
            e.consume();
            context.tabsState().closeTab(index);
            StudioOpenTab next = context.tabsState().activeTab();
            StudioRoute fallback = StudioRoute.overviewOf(context.router().currentRoute().concept());
            context.router().navigate(next != null ? next.route() : fallback);
        });
        return btn;
    }

    private String displayName(String elementId) {
        if (elementId == null || elementId.isBlank()) return "";
        String clean = elementId.contains("$") ? elementId.substring(0, elementId.indexOf('$')) : elementId;
        int sep = clean.indexOf(':');
        if (sep < 0 || sep + 1 >= clean.length()) return TextUtils.toDisplay(clean);
        return TextUtils.toDisplay(clean.substring(sep + 1));
    }
}



