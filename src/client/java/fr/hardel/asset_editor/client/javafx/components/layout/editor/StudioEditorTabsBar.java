package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelStudioWindow;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioElementId;
import fr.hardel.asset_editor.client.javafx.lib.FxSelectionBindings;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.lib.StudioText;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.components.ui.ResourceImageIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.WindowControls;
import fr.hardel.asset_editor.client.state.StudioOpenTab;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
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
    private final FxSelectionBindings bindings = new FxSelectionBindings();
    private final HBox tabsContainer = new HBox(4);

    public StudioEditorTabsBar(StudioContext context, Stage stage) {
        this.context = context;
        getStyleClass().add("studio-editor-tabs");
        setAlignment(Pos.CENTER_LEFT);
        setFillHeight(true);

        setPadding(new Insets(0, 0, 0, 8));
        setSpacing(10);
        tabsContainer.setAlignment(Pos.CENTER_LEFT);
        tabsContainer.setFillHeight(false);
        tabsContainer.getStyleClass().add("studio-editor-tablist");

        PackSelector packSelector = new PackSelector(context);

        Region separator = new Region();
        separator.getStyleClass().add("tabs-bar-separator");
        separator.setPrefWidth(1);
        separator.setMinWidth(1);
        separator.setMaxWidth(1);
        separator.setPrefHeight(24);
        separator.setMinHeight(24);
        separator.setMaxHeight(24);

        Region dragSpacer = new Region();
        HBox.setHgrow(dragSpacer, Priority.ALWAYS);
        dragSpacer.setPickOnBounds(true);
        VoxelStudioWindow.bindDragArea(dragSpacer);

        bindings.observe(context.selectOpenTabs(), tabs -> refreshTabs());
        bindings.observe(context.selectActiveTabIndex(), index -> refreshTabs());

        getChildren().addAll(packSelector, separator, tabsContainer, dragSpacer, buildWindowControls(stage));
        refreshTabs();
    }

    private HBox buildWindowControls(Stage stage) {
        return new WindowControls(stage, "studio-window-button", 48, 48, null, stage::hide);
    }

    private void refreshTabs() {
        tabsContainer.getChildren().clear();
        int activeIndex = context.tabsState().activeTabIndex();
        int index = 0;
        for (StudioOpenTab tab : context.tabsState().openTabsView()) {
            tabsContainer.getChildren().add(createTab(tab, index == activeIndex, index));
            index++;
        }
    }

    private HBox createTab(StudioOpenTab tab, boolean active, int index) {
        HBox item = new HBox(8);
        item.getStyleClass().add("studio-editor-tab-item");
        if (active)
            item.getStyleClass().add("studio-editor-tab-item-active");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(6, 12, 6, 12)); // px-3 py-1.5
        item.setMinHeight(Region.USE_PREF_SIZE);
        item.setMaxHeight(Region.USE_PREF_SIZE);
        item.setCursor(Cursor.HAND);
        item.setOnMouseClicked(e -> {
            context.tabsState().switchTab(index);
            context.router().navigate(tab.route());
            e.consume();
        });

        ResourceImageIcon icon = new ResourceImageIcon(StudioConcept.byRoute(tab.route()).icon(), 16); // size-4
        icon.setOpacity(active ? 1.0 : 0.85);

        StudioElementId parsed = StudioElementId.parse(tab.elementId());
        String displayName = parsed != null
                ? StudioText.resolve(
                        StudioConcept.byRoute(tab.route()).registryKey(),
                        parsed.identifier())
                : (tab.elementId() != null ? tab.elementId() : "");
        Label label = new Label(displayName);
        label.getStyleClass().add("studio-editor-tab-label");
        label.getStyleClass().add(active ? "studio-editor-tab-label-active" : "studio-editor-tab-label-inactive");
        label.setMaxWidth(192);
        label.setEllipsisString("...");

        StackPane closeBtn = closeButton(index, active);
        item.setOnMouseEntered(e -> closeBtn.setOpacity(1.0));
        item.setOnMouseExited(e -> closeBtn.setOpacity(active ? 1.0 : 0.0));

        item.getChildren().addAll(icon, label, closeBtn);
        return item;
    }

    private StackPane closeButton(int index, boolean active) {
        SvgIcon icon = new SvgIcon(CLOSE_ICON, 10, active ? VoxelColors.ZINC_200 : VoxelColors.ZINC_400);

        StackPane btn = new StackPane(icon);
        btn.getStyleClass().add("studio-editor-tab-close");
        btn.setPrefSize(16, 16);
        btn.setMinSize(16, 16);
        btn.setMaxSize(16, 16);
        btn.setCursor(Cursor.HAND);
        btn.setOpacity(active ? 1.0 : 0.0);
        btn.setOnMouseEntered(e -> icon.setIconFill(Color.WHITE));
        btn.setOnMouseExited(e -> icon.setIconFill(active ? VoxelColors.ZINC_200 : VoxelColors.ZINC_400));
        btn.setOnMouseClicked(e -> {
            e.consume();
            context.tabsState().closeTab(index);
            StudioOpenTab next = context.tabsState().activeTab();
            StudioRoute fallback = StudioRoute.overviewOf(context.router().currentRoute().concept());
            context.router().navigate(next != null ? next.route() : fallback);
        });
        return btn;
    }
}
