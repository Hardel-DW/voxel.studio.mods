package fr.hardel.asset_editor.client.javafx.components.ui;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

/**
 * Section with title + gradient hr + tab buttons on the right.
 * Active tab: bg-zinc-300 text-zinc-900. Inactive hover: bg-zinc-900.
 * Children are shown below the header, switching per selected tab.
 */
public final class ToolSectionSelector extends VBox {

    private final VBox childrenBox = new VBox(16);

    public ToolSectionSelector(String titleKey, LinkedHashMap<String, String> tabs, String defaultTab, Consumer<String> onTabChange, Node... initialContent) {
        setSpacing(0);
        setMaxWidth(Double.MAX_VALUE);

        Label title = new Label(I18n.get(titleKey));
        title.setFont(VoxelFonts.rubik(VoxelFonts.Rubik.SEMI_BOLD, 24));
        title.setTextFill(VoxelColors.ZINC_100);

        Region hr = new Region();
        hr.getStyleClass().add("tool-section-hr");
        hr.setMaxWidth(Double.MAX_VALUE);

        VBox titleBlock = new VBox(8, title, hr);
        titleBlock.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        AnimatedTabs tabsCard = new AnimatedTabs(tabs, defaultTab, onTabChange);
        tabsCard.setMinWidth(Region.USE_PREF_SIZE);
        tabsCard.setMaxWidth(Region.USE_PREF_SIZE);

        HBox header = new HBox(16, titleBlock, spacer, tabsCard);
        header.getStyleClass().add("tool-section-selector-header");
        header.setAlignment(Pos.CENTER_LEFT);

        childrenBox.setPadding(new Insets(16, 0, 0, 0));
        childrenBox.getChildren().addAll(initialContent);

        getChildren().addAll(header, childrenBox);
    }

    public void setContent(Node... nodes) {
        childrenBox.getChildren().setAll(nodes);
    }

    public void addContent(Node... nodes) {
        childrenBox.getChildren().addAll(nodes);
    }
}
