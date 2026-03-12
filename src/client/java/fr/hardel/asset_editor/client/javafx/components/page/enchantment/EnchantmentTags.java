package fr.hardel.asset_editor.client.javafx.components.page.enchantment;

import fr.hardel.asset_editor.client.javafx.VoxelColors;
import fr.hardel.asset_editor.client.javafx.VoxelFonts;
import fr.hardel.asset_editor.client.javafx.components.ui.Popover;
import fr.hardel.asset_editor.client.javafx.components.ui.ResourceImageIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.SimpleCard;
import fr.hardel.asset_editor.client.javafx.components.ui.Button;
import fr.hardel.asset_editor.client.javafx.components.ui.SvgIcon;
import fr.hardel.asset_editor.client.javafx.components.ui.ToggleSwitch;
import fr.hardel.asset_editor.client.javafx.lib.utils.IconUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Color;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public final class EnchantmentTags extends SimpleCard {

    private static final int MAX_DISPLAY = 3;
    private static final Identifier STAR_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/star.svg");

    private final Function<String, String> labelResolver;
    private final boolean locked;
    private final BooleanProperty targetState = new SimpleBooleanProperty(false);
    private final BooleanProperty membershipState = new SimpleBooleanProperty(false);
    private final List<String> values = new ArrayList<>();

    private final VBox tagListBox = new VBox(4);
    private final Region hr = new Region();
    private final Label seeMoreLabel;
    private final VBox overflowContent = new VBox(4);

    public EnchantmentTags(String titleKey, String descKey, Identifier imageId,
            List<String> initialValues, boolean isTarget, boolean isMember, boolean locked,
            Consumer<Boolean> onTargetToggle, Consumer<Boolean> onMembershipToggle,
            Function<String, String> labelResolver) {
        super(new Insets(16, 24, 16, 24));
        setCursor(javafx.scene.Cursor.DEFAULT);
        this.labelResolver = labelResolver;
        this.locked = locked;
        if (initialValues != null) values.addAll(initialValues);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        VBox header = buildHeader(titleKey, descKey, imageId);

        hr.getStyleClass().add("enchantment-tags-hr");
        hr.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(hr, new Insets(8, 0, 8, 0));

        VBox body = new VBox();
        body.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(body, Priority.ALWAYS);
        body.getChildren().addAll(header, hr, tagListBox);

        seeMoreLabel = new Label();
        seeMoreLabel.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
        seeMoreLabel.getStyleClass().add("enchantment-tags-see-more");
        overflowContent.getStyleClass().add("enchantment-tags-popover-list");
        new Popover(seeMoreLabel, overflowContent);

        Button actionsBtn = new Button(Button.Variant.GHOST_BORDER, Button.Size.SM, I18n.get("generic:actions"));
        actionsBtn.getStyleClass().add("enchantment-tags-actions-button");
        new Popover(actionsBtn, buildActionsContent(onTargetToggle, onMembershipToggle));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(seeMoreLabel, spacer, actionsBtn);
        bottom.setAlignment(Pos.CENTER_LEFT);

        VBox layout = new VBox(16, body, bottom);
        layout.setMaxWidth(Double.MAX_VALUE);
        layout.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(layout, Priority.ALWAYS);
        contentBox.setFillWidth(true);
        contentBox.setMaxHeight(Double.MAX_VALUE);
        contentBox.getChildren().add(layout);

        if (locked) setOpacity(0.5);
        targetState.addListener((obs, oldValue, newValue) -> applyTargetStyle(newValue));
        targetState.set(isTarget);
        membershipState.set(isMember);

        SvgIcon inListIcon = new SvgIcon(STAR_ICON, 16, Color.WHITE);
        inListIcon.getStyleClass().add("enchantment-tags-in-list");
        inListIcon.setMouseTransparent(true);
        inListIcon.visibleProperty().bind(membershipState);
        StackPane.setAlignment(inListIcon, Pos.TOP_RIGHT);
        StackPane.setMargin(inListIcon, new Insets(8, 8, 0, 0));
        visualCard.getChildren().add(inListIcon);

        refreshTagList();
        updateSeeMore();
        refreshOverflow();
    }

    public void updateTarget(boolean value) {
        targetState.set(value);
    }

    public void updateMembership(boolean value) {
        membershipState.set(value);
    }

    public List<String> getValues() {
        return List.copyOf(values);
    }

    public void updateValues(List<String> nextValues) {
        values.clear();
        if (nextValues != null) values.addAll(nextValues);
        refreshTagList();
        updateSeeMore();
        refreshOverflow();
    }

    private VBox buildHeader(String titleKey, String descKey, Identifier imageId) {
        Label title = new Label(resolveText(titleKey));
        title.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 16));
        title.setTextFill(Color.WHITE);

        Label desc = new Label(resolveText(descKey));
        desc.setFont(VoxelFonts.of(VoxelFonts.Variant.LIGHT, 12));
        desc.setTextFill(VoxelColors.ZINC_400);
        desc.setWrapText(true);

        VBox textBlock = new VBox(4, title, desc);
        textBlock.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        HBox row = new HBox(16, textBlock);
        row.setAlignment(Pos.CENTER_LEFT);

        if (imageId != null) {
            var icon = IconUtils.isSvgIcon(imageId)
                    ? new SvgIcon(imageId, 32, Paint.valueOf("#d4d4d8"))
                    : new ResourceImageIcon(imageId, 32);
            row.getChildren().addFirst(icon);
        }

        return new VBox(row);
    }

    private void refreshTagList() {
        tagListBox.getChildren().clear();
        boolean hasValues = !values.isEmpty();
        hr.setVisible(hasValues);
        hr.setManaged(hasValues);
        int count = Math.min(values.size(), MAX_DISPLAY);
        for (int i = 0; i < count; i++) {
            tagListBox.getChildren().add(buildTagChip(values.get(i)));
        }
    }

    private void updateSeeMore() {
        boolean show = values.size() > MAX_DISPLAY;
        seeMoreLabel.setVisible(show);
        seeMoreLabel.setManaged(show);
        if (show) {
            seeMoreLabel.setText(I18n.get("generic:see.more") + " (" + (values.size() - MAX_DISPLAY) + ")");
        }
    }

    private void refreshOverflow() {
        overflowContent.getChildren().clear();
        for (int i = MAX_DISPLAY; i < values.size(); i++) {
            overflowContent.getChildren().add(buildTagChip(values.get(i)));
        }
    }

    private Label buildTagChip(String value) {
        String label = labelResolver == null ? value : labelResolver.apply(value);
        Label chip = new Label(label == null || label.isBlank() ? value : label);
        chip.setFont(VoxelFonts.of(VoxelFonts.Variant.REGULAR, 12));
        chip.setTextFill(VoxelColors.ZINC_400);
        chip.getStyleClass().add("enchantment-tags-value");
        chip.setMaxWidth(Double.MAX_VALUE);
        return chip;
    }

    private VBox buildActionsContent(Consumer<Boolean> onTargetToggle, Consumer<Boolean> onMembershipToggle) {
        VBox content = new VBox(8);
        content.getStyleClass().add("enchantment-tags-actions-popover");
        content.getChildren().addAll(
                buildActionRow(
                        "enchantment:exclusive.actions.target.title",
                        "enchantment:exclusive.actions.target.subtitle",
                        "enchantment:exclusive.actions.target.description",
                        targetState, onTargetToggle),
                buildActionRow(
                        "enchantment:exclusive.actions.membership.title",
                        "enchantment:exclusive.actions.membership.subtitle",
                        "enchantment:exclusive.actions.membership.description",
                        membershipState, onMembershipToggle));
        return content;
    }

    private HBox buildActionRow(String titleKey, String subtitleKey, String descriptionKey,
            BooleanProperty state, Consumer<Boolean> handler) {
        Label title = new Label(resolveText(titleKey));
        title.getStyleClass().add("enchantment-tags-action-title");

        HBox titleLine = new HBox(6, title);
        if (subtitleKey != null && !subtitleKey.isBlank()) {
            Label subtitle = new Label(resolveText(subtitleKey));
            subtitle.getStyleClass().add("enchantment-tags-action-subtitle");
            titleLine.getChildren().add(subtitle);
        }
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label description = new Label(resolveText(descriptionKey));
        description.getStyleClass().add("enchantment-tags-action-description");
        description.setWrapText(true);

        VBox textBlock = new VBox(2, titleLine, description);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        ToggleSwitch toggle = new ToggleSwitch();
        toggle.setValue(state.get());
        toggle.setDisable(locked);
        state.addListener((obs, oldValue, newValue) -> toggle.setValue(newValue));
        toggle.addEventHandler(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        HBox row = new HBox(12, textBlock, toggle);
        row.getStyleClass().add("enchantment-tags-action-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setOnMouseClicked(event -> {
            if (!toggle.isDisabled() && handler != null) {
                handler.accept(!state.get());
            }
        });

        return row;
    }

    private void applyTargetStyle(boolean isTarget) {
        if (isTarget) {
            if (!visualCard.getStyleClass().contains("enchantment-tags-targeted"))
                visualCard.getStyleClass().add("enchantment-tags-targeted");
            return;
        }
        visualCard.getStyleClass().remove("enchantment-tags-targeted");
    }

    private static String resolveText(String keyOrText) {
        if (keyOrText == null) return "";
        return I18n.exists(keyOrText) ? I18n.get(keyOrText) : keyOrText;
    }
}
