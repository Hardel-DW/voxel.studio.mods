package fr.hardel.asset_editor.client.javafx.components.layout.editor;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioConcept;
import fr.hardel.asset_editor.client.javafx.routes.StudioRoute;
import fr.hardel.asset_editor.client.javafx.components.ui.ResourceImageIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

/**
 * Primary sidebar (w-16 = 64px).
 * Structure: logo area (h-16) → concept cards → spacer → bottom buttons.
 * Matches EditorLayout.tsx aside + StudioSidebar.tsx.
 */
public final class StudioPrimarySidebar extends VBox {

    private static final Identifier LOGO     = Identifier.fromNamespaceAndPath("asset_editor", "icons/logo.svg");
    private static final Identifier SETTINGS = Identifier.fromNamespaceAndPath("asset_editor", "icons/settings.svg");

    private final StudioContext context;
    private final VBox concepts = new VBox(12);

    public StudioPrimarySidebar(StudioContext context) {
        this.context = context;
        getStyleClass().add("studio-primary-sidebar");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(buildLogoArea(), buildConcepts(), spacer, buildBottom());
        context.router().routeProperty().addListener((obs, oldValue, newValue) -> refreshConcepts());
        refreshConcepts();
    }

    private StackPane buildLogoArea() {
        SvgIcon logo = new SvgIcon(LOGO, 20, Color.WHITE);
        StackPane area = new StackPane(logo);
        area.getStyleClass().add("studio-logo-area");
        area.setCursor(Cursor.HAND);
        area.setOnMouseEntered(e -> logo.setOpacity(0.8));
        area.setOnMouseExited(e -> logo.setOpacity(1.0));
        area.setOnMouseClicked(e -> context.router().navigate(StudioRoute.ENCHANTMENT_OVERVIEW));
        return area;
    }

    private VBox buildConcepts() {
        concepts.setAlignment(Pos.TOP_CENTER);
        concepts.setPadding(new Insets(16, 0, 0, 0)); // mt-4
        return concepts;
    }

    private StackPane conceptCard(StudioConcept concept) {
        boolean active = context.router().currentRoute().concept().equals(concept.registry());
        ResourceImageIcon icon = new ResourceImageIcon(concept.icon(), 24); // size-6
        icon.setOpacity(active ? 1.0 : 0.8);

        StackPane card = new StackPane(icon);
        card.getStyleClass().add("studio-concept-card");
        if (active) card.getStyleClass().add("studio-concept-card-active");
        card.setCursor(active ? Cursor.DEFAULT : Cursor.HAND);

        if (!active) {
            card.setOnMouseEntered(e -> icon.setOpacity(1.0));
            card.setOnMouseExited(e -> icon.setOpacity(0.8));
            card.setOnMouseClicked(e -> {
                context.uiState().setFilterPath("");
                context.tabsState().setCurrentElementId("");
                context.router().navigate(concept.overviewRoute());
            });
        }

        Tooltip tooltip = new Tooltip(I18n.get(concept.titleKey()));
        tooltip.setShowDelay(Duration.millis(150));
        Tooltip.install(card, tooltip);
        return card;
    }

    private void refreshConcepts() {
        concepts.getChildren().clear();
        for (StudioConcept concept : StudioConcept.values()) {
            if (concept == StudioConcept.STRUCTURE) continue;
            concepts.getChildren().add(conceptCard(concept));
        }
    }

    private VBox buildBottom() {
        SvgIcon gear = new SvgIcon(SETTINGS, 24, VoxelColors.ZINC_400);
        gear.setOpacity(0.7);

        StackPane btn = new StackPane(gear);
        btn.getStyleClass().add("studio-settings-button");
        btn.setPrefSize(40, 40);
        btn.setMinSize(40, 40);
        btn.setMaxSize(40, 40);
        btn.setAlignment(Pos.CENTER);
        btn.setCursor(Cursor.HAND);
        btn.setOnMouseEntered(e -> { gear.setIconFill(Color.WHITE); gear.setOpacity(1.0); });
        btn.setOnMouseExited(e -> { gear.setIconFill(VoxelColors.ZINC_400); gear.setOpacity(0.7); });

        VBox bottom = new VBox(8, btn);
        bottom.setAlignment(Pos.BOTTOM_CENTER);
        bottom.setPadding(new Insets(0, 0, 12, 0));
        return bottom;
    }
}



