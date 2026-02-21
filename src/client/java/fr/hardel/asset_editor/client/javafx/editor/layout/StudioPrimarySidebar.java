package fr.hardel.asset_editor.client.javafx.editor.layout;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.editor.StudioContext;
import fr.hardel.asset_editor.client.javafx.editor.state.StudioConcept;
import fr.hardel.asset_editor.client.javafx.editor.state.StudioRoute;
import fr.hardel.asset_editor.client.javafx.ui.ResourceImageIcon;
import fr.hardel.asset_editor.client.javafx.ui.SvgIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

/**
 * Primary sidebar (w-16 = 64px).
 * Structure: logo area (h-16) → concept cards → spacer → bottom buttons.
 * Matches EditorLayout.tsx aside + StudioSidebar.tsx.
 */
public final class StudioPrimarySidebar extends VBox {

    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("asset_editor", "icons/logo.svg");

    public StudioPrimarySidebar(StudioContext context) {
        getStyleClass().add("studio-primary-sidebar");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(buildLogoArea(context), buildConcepts(context), spacer, buildBottom());
    }

    private StackPane buildLogoArea(StudioContext context) {
        SvgIcon logo = new SvgIcon(LOGO, 20, Color.WHITE);
        StackPane area = new StackPane(logo);
        area.getStyleClass().add("studio-logo-area");
        area.setCursor(Cursor.HAND);
        area.setOnMouseEntered(e -> logo.setOpacity(0.8));
        area.setOnMouseExited(e -> logo.setOpacity(1.0));
        area.setOnMouseClicked(e -> context.router().navigate(StudioRoute.ENCHANTMENT_OVERVIEW));
        return area;
    }

    private VBox buildConcepts(StudioContext context) {
        VBox concepts = new VBox(12); // gap-3
        concepts.setAlignment(Pos.TOP_CENTER);
        concepts.setPadding(new Insets(16, 0, 0, 0)); // mt-4
        for (StudioConcept concept : StudioConcept.values())
            concepts.getChildren().add(conceptCard(context, concept));
        return concepts;
    }

    private StackPane conceptCard(StudioContext context, StudioConcept concept) {
        boolean active = concept == StudioConcept.ENCHANTMENT;
        ResourceImageIcon icon = new ResourceImageIcon(concept.icon(), 24); // size-6
        icon.setOpacity(active ? 1.0 : 0.8);

        StackPane card = new StackPane(icon);
        card.getStyleClass().add("studio-concept-card");
        if (active) card.getStyleClass().add("studio-concept-card-active");
        card.setCursor(active ? Cursor.DEFAULT : Cursor.HAND);

        if (!active) {
            card.setOnMouseEntered(e -> icon.setOpacity(1.0));
            card.setOnMouseExited(e -> icon.setOpacity(0.8));
        }

        Tooltip tooltip = new Tooltip(I18n.get(concept.titleKey()));
        tooltip.setShowDelay(Duration.millis(150));
        Tooltip.install(card, tooltip);
        return card;
    }

    private VBox buildBottom() {
        // Settings gear icon (size-6 = 24px, opacity-70)
        SVGPath gear = new SVGPath();
        gear.setContent("M12 15.5A3.5 3.5 0 018.5 12 3.5 3.5 0 0112 8.5a3.5 3.5 0 013.5 3.5 3.5 3.5 0 01-3.5 3.5m7.43-2.92c.04-.32.07-.64.07-.08 0-.32-.03-.56-.07-.88l1.9-1.47c.17-.14.22-.36.12-.56l-1.8-3.11c-.1-.2-.33-.27-.53-.2l-2.24.9c-.47-.36-.97-.66-1.51-.88l-.34-2.38c-.04-.21-.22-.36-.44-.36H9.04c-.22 0-.4.15-.44.36l-.34 2.38c-.54.22-1.04.52-1.51.88l-2.24-.9c-.2-.07-.43 0-.53.2l-1.8 3.11c-.1.2-.05.42.12.56l1.9 1.47c-.04.32-.07.65-.07.88s.03.56.07.88L2.3 13.4c-.17.14-.22.36-.12.56l1.8 3.11c.1.2.33.27.53.2l2.24-.9c.47.36.97.66 1.51.88l.34 2.38c.04.21.22.36.44.36h3.6c.22 0 .4-.15.44-.36l.34-2.38c.54-.22 1.04-.52 1.51-.88l2.24.9c.2.07.43 0 .53-.2l1.8-3.11c.1-.2.05-.42-.12-.56l-1.9-1.47z");
        gear.setFill(VoxelColors.ZINC_500);
        gear.setOpacity(0.7);
        // 24x24 viewBox, render at 24px
        gear.getTransforms().add(new Scale(24.0 / 24.0, 24.0 / 24.0, 0, 0));

        StackPane btn = new StackPane(gear);
        btn.getStyleClass().add("studio-settings-button");
        btn.setPrefSize(40, 40);
        btn.setMinSize(40, 40);
        btn.setMaxSize(40, 40);
        btn.setAlignment(Pos.CENTER);
        btn.setCursor(Cursor.HAND);
        btn.setOnMouseEntered(e -> gear.setFill(VoxelColors.ZINC_400));
        btn.setOnMouseExited(e -> gear.setFill(VoxelColors.ZINC_500));

        VBox bottom = new VBox(8, btn);
        bottom.setAlignment(Pos.BOTTOM_CENTER);
        bottom.setPadding(new Insets(0, 0, 12, 0));
        return bottom;
    }
}
