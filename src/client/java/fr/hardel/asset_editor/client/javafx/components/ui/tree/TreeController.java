package fr.hardel.asset_editor.client.javafx.components.ui.tree;

import fr.hardel.asset_editor.client.javafx.context.StudioContext;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.store.StudioOpenTab;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.List;
import java.util.Map;

public final class TreeController {

    private static final String DEFAULT_ELEMENT_ICON = "/images/features/item/bundle_open.png";

    private final StudioContext context;
    private final Config config;
    private final ObjectProperty<TreeNodeModel> tree;
    private final ObjectProperty<Map<String, String>> folderIcons;
    private final ObjectProperty<String> elementIconPath;
    private final BooleanProperty disableAutoExpand;
    private final ReadOnlyBooleanWrapper allActive = new ReadOnlyBooleanWrapper(true);

    public TreeController(StudioContext context, Config config) {
        this.context = context;
        this.config = config;
        this.tree = new SimpleObjectProperty<>(config.tree());
        this.folderIcons = new SimpleObjectProperty<>(config.folderIcons() == null ? Map.of() : config.folderIcons());
        this.elementIconPath = new SimpleObjectProperty<>(
                config.elementIconPath() == null || config.elementIconPath().isBlank()
                        ? DEFAULT_ELEMENT_ICON
                        : config.elementIconPath());
        this.disableAutoExpand = new SimpleBooleanProperty(config.disableAutoExpand());

        context.uiState().filterPathProperty().addListener((obs, oldValue, newValue) -> refreshState());
        context.tabsState().currentElementIdProperty().addListener((obs, oldValue, newValue) -> refreshState());
        context.router().routeProperty().addListener((obs, oldValue, newValue) -> {
            syncSelectionFromActiveTab();
            refreshState();
        });
        refreshState();
    }

    public StudioRoute overviewRoute() {
        return config.overviewRoute();
    }

    public StudioRoute changesRoute() {
        return config.changesRoute();
    }

    public String concept() {
        return config.concept();
    }

    public TreeNodeModel tree() {
        return tree.get();
    }

    public ObjectProperty<TreeNodeModel> treeProperty() {
        return tree;
    }

    public String filterPath() {
        return context.uiState().filterPath();
    }

    public String currentElementId() {
        if (config.selectedElementId() != null) {
            String selected = config.selectedElementId().get();
            return selected == null || selected.isBlank() ? null : selected;
        }
        String selected = context.tabsState().currentElementId();
        return selected == null || selected.isBlank() ? null : selected;
    }

    public String elementIconPath() {
        return elementIconPath.get();
    }

    public ObjectProperty<String> elementIconPathProperty() {
        return elementIconPath;
    }

    public Map<String, String> folderIcons() {
        return folderIcons.get();
    }

    public ObjectProperty<Map<String, String>> folderIconsProperty() {
        return folderIcons;
    }

    public boolean disableAutoExpand() {
        return disableAutoExpand.get();
    }

    public BooleanProperty disableAutoExpandProperty() {
        return disableAutoExpand;
    }

    public boolean isAllActive() {
        return allActive.get();
    }

    public ReadOnlyBooleanProperty allActiveProperty() {
        return allActive.getReadOnlyProperty();
    }

    public int modifiedCount() {
        return config.modifiedCount() == null ? 0 : config.modifiedCount().getAsInt();
    }

    public void setTree(TreeNodeModel value) {
        tree.set(value);
    }

    public void setFolderIcons(Map<String, String> value) {
        folderIcons.set(value == null ? Map.of() : value);
    }

    public void setDisableAutoExpand(boolean value) {
        disableAutoExpand.set(value);
    }

    public void selectFolder(String path) {
        if (config.onSelectFolder() != null) {
            config.onSelectFolder().accept(path);
            return;
        }
        context.uiState().setFilterPath(path);
        clearSelection();
        context.router().navigate(config.overviewRoute());
    }

    public void selectElement(String elementId) {
        if (config.onSelectElement() != null) {
            config.onSelectElement().accept(elementId);
            return;
        }
        context.tabsState().openElement(elementId, config.detailRoute());
        if (!isOnTabRoute()) {
            context.router().navigate(config.detailRoute());
        }
    }

    public void selectAll() {
        context.uiState().setFilterPath("");
        clearSelection();
        context.router().navigate(config.overviewRoute());
    }

    public void clearSelection() {
        context.tabsState().setCurrentElementId("");
    }

    private boolean isOnTabRoute() {
        List<StudioRoute> routes = config.tabRoutes();
        if (routes == null || routes.isEmpty()) return false;
        StudioRoute route = context.router().currentRoute();
        return routes.contains(route);
    }

    private void syncSelectionFromActiveTab() {
        if (!isOnTabRoute()) return;
        String current = context.tabsState().currentElementId();
        if (current != null && !current.isBlank()) return;
        StudioOpenTab activeTab = context.tabsState().activeTab();
        if (activeTab != null) {
            context.tabsState().setCurrentElementId(activeTab.elementId());
        }
    }

    private void refreshState() {
        String filterPath = context.uiState().filterPath();
        String activeId = currentElementId();
        allActive.set((filterPath == null || filterPath.isBlank()) && (activeId == null || activeId.isBlank()));
    }

    public record Config(
            StudioRoute overviewRoute,
            StudioRoute detailRoute,
            StudioRoute changesRoute,
            String concept,
            List<StudioRoute> tabRoutes,
            TreeNodeModel tree,
            String elementIconPath,
            Map<String, String> folderIcons,
            boolean disableAutoExpand,
            java.util.function.Supplier<String> selectedElementId,
            java.util.function.Consumer<String> onSelectElement,
            java.util.function.Consumer<String> onSelectFolder,
            java.util.function.IntSupplier modifiedCount
    ) {
    }
}
