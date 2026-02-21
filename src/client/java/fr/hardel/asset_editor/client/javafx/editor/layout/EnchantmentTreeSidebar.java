package fr.hardel.asset_editor.client.javafx.editor.layout;

import fr.hardel.asset_editor.client.javafx.editor.StudioContext;
import fr.hardel.asset_editor.client.javafx.editor.model.StudioMockEnchantment;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public final class EnchantmentTreeSidebar extends VBox {

    private final StudioContext context;

    public EnchantmentTreeSidebar(StudioContext context) {
        this.context = context;
        getStyleClass().add("enchantment-tree");
        setSpacing(2);
        setPadding(new Insets(8, 0, 8, 0));
        context.uiState().sidebarViewProperty().addListener((obs, oldValue, newValue) -> refresh());
        context.uiState().filterPathProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    private void refresh() {
        getChildren().clear();
        for (String group : context.repository().groups(context.uiState().sidebarView())) {
            getChildren().add(groupButton(group));
            VBox leaves = new VBox(1);
            leaves.getStyleClass().add("enchantment-tree-leaves");
            for (StudioMockEnchantment enchantment : context.repository().enchantments()) {
                if (!matchesGroup(enchantment, group))
                    continue;
                leaves.getChildren().add(leafButton(group, enchantment.resource()));
            }
            getChildren().add(leaves);
        }
    }

    private boolean matchesGroup(StudioMockEnchantment enchantment, String group) {
        return switch (context.uiState().sidebarView()) {
            case SLOTS -> enchantment.slots().contains(group);
            case ITEMS -> enchantment.items().contains(group);
            case EXCLUSIVE -> enchantment.exclusiveGroup().equals(group);
        };
    }

    private Button groupButton(String group) {
        Button button = new Button(group);
        button.getStyleClass().add("enchantment-tree-group");
        button.setMaxWidth(Double.MAX_VALUE);
        String activePath = context.uiState().filterPath();
        if (group.equals(activePath))
            button.getStyleClass().add("enchantment-tree-group-active");
        button.setOnAction(e -> context.uiState().setFilterPath(group));
        return button;
    }

    private Button leafButton(String group, String leaf) {
        Button button = new Button(leaf);
        button.getStyleClass().add("enchantment-tree-leaf");
        button.setMaxWidth(Double.MAX_VALUE);
        String path = group + "/" + leaf;
        if (path.equals(context.uiState().filterPath()))
            button.getStyleClass().add("enchantment-tree-leaf-active");
        button.setOnAction(e -> context.uiState().setFilterPath(path));
        return button;
    }
}
