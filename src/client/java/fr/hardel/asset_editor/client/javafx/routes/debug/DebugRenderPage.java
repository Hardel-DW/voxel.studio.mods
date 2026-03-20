package fr.hardel.asset_editor.client.javafx.routes.debug;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.InputText;
import fr.hardel.asset_editor.client.javafx.components.ui.ItemSprite;
import fr.hardel.asset_editor.client.javafx.lib.ItemAtlasGenerator;
import fr.hardel.asset_editor.client.javafx.lib.Page;
import fr.hardel.asset_editor.client.javafx.lib.utils.ColorUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class DebugRenderPage extends StackPane implements Page {

    private final Label loading = new Label(I18n.get("debug:render.loading"));
    private final FlowPane grid = new FlowPane(4, 4);
    private final List<Identifier> allItems;
    private Runnable subscription;

    public DebugRenderPage() {
        getStyleClass().add("concept-main-page");
        allItems = new ArrayList<>(BuiltInRegistries.ITEM.keySet());

        Label title = new Label(I18n.get("debug:render.title", allItems.size()));
        title.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 18));
        title.setTextFill(VoxelColors.ZINC_100);

        InputText searchInput = new InputText(I18n.get("debug:render.search"));
        searchInput.setMaxWidth(576);
        HBox.setHgrow(searchInput, Priority.ALWAYS);
        searchInput.field().textProperty().addListener((obs, old, query) -> filterItems(query));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(16, searchInput, spacer, title);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        grid.setPadding(new Insets(16, 0, 0, 0));
        grid.setMaxWidth(Double.MAX_VALUE);

        populateGrid(allItems);

        VBox column = new VBox(16, toolbar, grid);
        column.setPadding(new Insets(32));
        column.setMaxWidth(Double.MAX_VALUE);

        ScrollPane scroll = new ScrollPane(column);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("debug-subpage-scroll");

        loading.setFont(VoxelFonts.of(VoxelFonts.Variant.SEMI_BOLD, 18));
        loading.setTextFill(VoxelColors.ZINC_400);
        loading.setMouseTransparent(true);

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                if (subscription != null) {
                    subscription.run();
                    subscription = null;
                }
                return;
            }
            if (subscription == null)
                subscription = ItemAtlasGenerator.subscribe(this::refreshLoadingState);
            refreshLoadingState();
        });

        getChildren().addAll(scroll, loading);
        refreshLoadingState();
    }

    private void filterItems(String query) {
        String lower = query == null ? "" : query.toLowerCase().trim();
        if (lower.isEmpty()) {
            populateGrid(allItems);
            return;
        }

        List<Identifier> filtered = allItems.stream()
            .filter(id -> id.toString().contains(lower))
            .toList();
        populateGrid(filtered);
    }

    private void populateGrid(List<Identifier> items) {
        grid.getChildren().clear();
        for (Identifier itemId : items)
            grid.getChildren().add(buildItemCell(itemId));
    }

    private static StackPane buildItemCell(Identifier itemId) {
        ItemSprite icon = new ItemSprite(itemId, 32);

        StackPane cell = new StackPane(icon);
        cell.setPrefSize(40, 40);
        cell.setMinSize(40, 40);
        cell.setMaxSize(40, 40);
        cell.setAlignment(Pos.CENTER);
        cell.setStyle("-fx-background-color: " + ColorUtils.toCssRgb(VoxelColors.CARD) + "; -fx-background-radius: 4;");

        Tooltip tooltip = new Tooltip(itemId.toString());
        tooltip.setShowDelay(Duration.millis(200));
        Tooltip.install(cell, tooltip);

        return cell;
    }

    private void refreshLoadingState() {
        boolean showLoading = ItemAtlasGenerator.getAtlasImage() == null;
        loading.setVisible(showLoading);
        loading.setManaged(showLoading);
    }
}
