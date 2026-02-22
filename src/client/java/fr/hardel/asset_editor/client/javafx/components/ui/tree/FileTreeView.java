package fr.hardel.asset_editor.client.javafx.components.ui.tree;

import fr.hardel.asset_editor.client.javafx.components.ui.ResourceImageIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.context.StudioContext;
import javafx.animation.RotateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FileTreeView extends VBox {

    private final StudioContext context;
    private final TreeController tree;
    private final HashMap<String, Boolean> openState = new HashMap<>();

    public FileTreeView(StudioContext context, TreeController tree) {
        this.context = context;
        this.tree = tree;
        getStyleClass().add("tree-file");

        context.uiState().filterPathProperty().addListener((obs, oldValue, newValue) -> refresh());
        context.tabsState().currentElementIdProperty().addListener((obs, oldValue, newValue) -> refresh());
        tree.treeProperty().addListener((obs, oldValue, newValue) -> refresh());
        tree.folderIconsProperty().addListener((obs, oldValue, newValue) -> refresh());
        tree.elementIconPathProperty().addListener((obs, oldValue, newValue) -> refresh());
        tree.disableAutoExpandProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    private void refresh() {
        getChildren().clear();
        TreeNodeModel root = tree.tree();
        if (root == null) return;
        for (Map.Entry<String, TreeNodeModel> entry : sortedEntries(root.children())) {
            getChildren().add(createNode(entry.getKey(), entry.getKey(), entry.getValue(), 0, false));
        }
    }

    private Node createNode(String name, String path, TreeNodeModel node, int depth, boolean forceOpen) {
        boolean isElement = node.elementId() != null && !node.elementId().isBlank();
        boolean hasChildren = !node.children().isEmpty();
        String activeElementId = tree.currentElementId();
        boolean hasActiveChild = !tree.disableAutoExpand() && TreeUtils.hasActiveDescendant(node, activeElementId);
        boolean defaultOpen = forceOpen || hasActiveChild;
        boolean isOpen = openState.computeIfAbsent(path, key -> defaultOpen);
        if (hasActiveChild && !isOpen) {
            openState.put(path, true);
            isOpen = true;
        }

        String filterPath = tree.filterPath();
        boolean isHighlighted = isElement
                ? node.elementId().equals(activeElementId)
                : (activeElementId == null || activeElementId.isBlank()) && path.equals(filterPath);
        boolean isEmpty = !isElement && node.count() == 0;

        String iconPath = node.iconPath();
        boolean isDefaultFolderIcon = false;
        if (iconPath == null || iconPath.isBlank()) {
            if (isElement) {
                iconPath = tree.elementIconPath();
            } else {
                String folderIcon = tree.folderIcons().get(name);
                if (folderIcon == null || folderIcon.isBlank()) {
                    iconPath = "/icons/folder.svg";
                    isDefaultFolderIcon = true;
                } else {
                    iconPath = folderIcon;
                }
            }
        }

        VBox wrapper = new VBox();
        wrapper.getStyleClass().add("tree-node-wrapper");
        StackPane rowContainer = new StackPane();
        rowContainer.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(8);
        row.getStyleClass().add("tree-row");
        row.getStyleClass().add(isHighlighted ? "tree-row-active" : "tree-row-inactive");
        if (depth > 0) row.getStyleClass().add("tree-row-depth");
        row.setPadding(new Insets(0, 8, 0, depth * 8 + 8));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        if (isEmpty && !isHighlighted) {
            row.setOpacity(0.5);
        }

        if (isHighlighted) {
            int hue = TreeColorUtil.stringToHue(isElement ? node.elementId() : path);
            Region accent = new Region();
            accent.getStyleClass().add("tree-row-accent");
            accent.setPrefWidth(4);
            accent.setMinWidth(4);
            accent.setMaxWidth(4);
            accent.setStyle("-fx-background-color: " + toCssColor(TreeColorUtil.hueToColor(hue)) + ";");
            StackPane.setAlignment(accent, Pos.CENTER_LEFT);
            StackPane.setMargin(accent, new Insets(8, 0, 8, 0));
            rowContainer.getChildren().add(accent);
        }

        if (!isElement) {
            Node chevronIcon = new SvgIcon("/icons/chevron-down.svg", 12, Color.WHITE);
            chevronIcon.setRotate(isOpen ? 0 : -90);
            if (!hasChildren) chevronIcon.setOpacity(0.2);

            Button chevron = new Button();
            chevron.getStyleClass().add("tree-chevron");
                if (hasChildren) chevron.getStyleClass().add("tree-chevron-hoverable");
            chevron.setGraphic(chevronIcon);
            chevron.setOnAction(event -> {
                event.consume();
                boolean next = !openState.getOrDefault(path, false);
                openState.put(path, next);
                RotateTransition rotate = new RotateTransition(Duration.millis(150), chevronIcon);
                rotate.setToAngle(next ? 0 : -90);
                rotate.play();
                refresh();
            });
            row.getChildren().add(chevron);
        }

        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        Node icon = iconPath.endsWith(".svg")
                ? new SvgIcon(iconPath, 20, Color.WHITE)
                : new ResourceImageIcon(iconPath, 20);
        icon.getStyleClass().add("tree-row-icon");
        if (isDefaultFolderIcon) icon.setOpacity(isHighlighted ? 1.0 : 0.6);
        content.getChildren().add(icon);

        Label label = new Label(TreeTextUtil.toDisplay(name));
        label.getStyleClass().add("tree-row-label");
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);
        content.getChildren().add(label);

        Button mainButton = new Button();
        mainButton.getStyleClass().add("tree-main-button");
        mainButton.setGraphic(content);
        mainButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(mainButton, Priority.ALWAYS);
        mainButton.setOnAction(event -> {
            event.consume();
            if (isElement) {
                tree.selectElement(node.elementId());
                return;
            }
            openState.put(path, !openState.getOrDefault(path, false));
            tree.selectFolder(path);
            refresh();
        });
        row.getChildren().add(mainButton);

        if (!isElement) {
            Label count = new Label(String.valueOf(node.count()));
            count.getStyleClass().add("tree-row-count");
            row.getChildren().add(count);
        }

        rowContainer.getChildren().add(row);
        wrapper.getChildren().add(rowContainer);

        if (hasChildren && isOpen) {
            VBox childrenBox = new VBox();
            childrenBox.getStyleClass().add("tree-children");
            for (Map.Entry<String, TreeNodeModel> child : sortedEntries(node.children())) {
                boolean childForceOpen = node.children().size() == 1;
                childrenBox.getChildren().add(createNode(
                        child.getKey(),
                        path + "/" + child.getKey(),
                        child.getValue(),
                        depth + 1,
                        childForceOpen));
            }
            wrapper.getChildren().add(childrenBox);
        }

        return wrapper;
    }

    private static List<Map.Entry<String, TreeNodeModel>> sortedEntries(Map<String, TreeNodeModel> map) {
        ArrayList<Map.Entry<String, TreeNodeModel>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Comparator.comparing(entry -> isElement(entry.getValue())));
        return entries;
    }

    private static boolean isElement(TreeNodeModel node) {
        return node.elementId() != null && !node.elementId().isBlank();
    }

    private static String toCssColor(Color color) {
        return "rgba(%d,%d,%d,%.3f)".formatted(
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255),
                color.getOpacity());
    }
}
