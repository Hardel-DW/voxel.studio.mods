package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.SimpleCard;
import fr.hardel.asset_editor.client.javafx.components.ui.Button;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Tag display card: title + description + up-to-3 tag values + "see more" + actions button.
 * Ring when current enchantment is in the tag list (targetValue).
 * Locked: opacity-50.
 */
public final class EnchantmentTags extends SimpleCard {

    private static final int MAX_DISPLAY = 3;

    public EnchantmentTags(String titleKey, String descKey, Identifier imageId, List<String> values, boolean isTarget, boolean locked) {
        super(new Insets(16, 24, 16, 24));

        if (locked) setOpacity(0.5);
        if (isTarget) visualCard.getStyleClass().add("enchantment-tags-targeted");

        VBox top = buildHeader(titleKey, descKey, imageId);

        VBox body = new VBox(top);
        body.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(body, Priority.ALWAYS);

        if (!values.isEmpty()) {
            Region hr = new Region();
            hr.getStyleClass().add("enchantment-tags-hr");
            hr.setMaxWidth(Double.MAX_VALUE);
            VBox.setMargin(hr, new Insets(8, 0, 8, 0));

            VBox tagList = buildTagList(values);
            body.getChildren().addAll(hr, tagList);
        }

        HBox bottom = buildBottom(values);

        VBox layout = new VBox(16, body, bottom);
        layout.setMaxWidth(Double.MAX_VALUE);
        contentBox.getChildren().add(layout);
    }

    private VBox buildHeader(String titleKey, String descKey, Identifier imageId) {
        Label title = new Label(I18n.get(titleKey));
        title.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 16));
        title.setTextFill(Color.WHITE);

        Label desc = new Label(I18n.get(descKey));
        desc.setFont(VoxelFonts.of(VoxelFonts.Variant.LIGHT, 12));
        desc.setTextFill(VoxelColors.ZINC_400);
        desc.setWrapText(true);

        VBox textBlock = new VBox(4, title, desc);
        textBlock.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        HBox row = new HBox(16, textBlock);
        row.setAlignment(Pos.CENTER_LEFT);

        if (imageId != null) {
            fr.hardel.asset_editor.client.javafx.components.ui.ResourceImageIcon icon =
                new fr.hardel.asset_editor.client.javafx.components.ui.ResourceImageIcon(imageId, 32);
            row.getChildren().add(0, icon);
        }

        return new VBox(row);
    }

    private VBox buildTagList(List<String> values) {
        VBox list = new VBox(4);
        int count = Math.min(values.size(), MAX_DISPLAY);
        for (int i = 0; i < count; i++) {
            list.getChildren().add(buildTagChip(values.get(i)));
        }
        return list;
    }

    private Label buildTagChip(String value) {
        Label chip = new Label(resolveTagLabel(value));
        chip.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
        chip.setTextFill(VoxelColors.ZINC_400);
        chip.getStyleClass().add("enchantment-tags-value");
        chip.setMaxWidth(Double.MAX_VALUE);
        return chip;
    }

    private HBox buildBottom(List<String> values) {
        HBox bottom = new HBox();
        bottom.setAlignment(Pos.CENTER_LEFT);

        if (values.size() > MAX_DISPLAY) {
            int overflow = values.size() - MAX_DISPLAY;
            Label seeMore = new Label(I18n.get("generic.see.more") + " (" + overflow + ")");
            seeMore.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
            seeMore.setTextFill(VoxelColors.ZINC_400);
            seeMore.setPadding(new Insets(0, 8, 0, 0));
            seeMore.setCursor(javafx.scene.Cursor.HAND);
            bottom.getChildren().add(seeMore);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bottom.getChildren().add(spacer);

        Button actionsBtn = new Button(Button.Variant.GHOST_BORDER, Button.Size.SM, I18n.get("generic.actions"));
        actionsBtn.setMouseTransparent(true);
        bottom.getChildren().add(actionsBtn);

        return bottom;
    }

    private String resolveTagLabel(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String clean = value.startsWith("#") ? value.substring(1) : value;
        Identifier identifier = Identifier.tryParse(clean);
        if (identifier == null) {
            return value;
        }
        if (BuiltInRegistries.ITEM.containsKey(identifier)) {
            var item = BuiltInRegistries.ITEM.getValue(identifier);
            return item.getName().getString();
        }
        return value;
    }
}
